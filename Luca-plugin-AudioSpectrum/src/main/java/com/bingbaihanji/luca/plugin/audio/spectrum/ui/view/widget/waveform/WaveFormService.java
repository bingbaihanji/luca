package com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.waveform;


import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.AudioDecoder;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.AudioDecoderFactory;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderException;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderProgressListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * WaveFormService 负责在后台线程中生成或处理音频文件的波形数据，
 * 以供 WaveVisualization 绘制使用。继承自 javafx.concurrent.Service<Boolean>，
 * 通过重写 createTask() 执行耗时操作，并在完成后通知 UI。
 */
public class WaveFormService extends Service<Boolean> {

    /**
     * 引用到外部的 WaveVisualization，用于回调设置波形等
     */
    private final WaveVisualization waveVisualization;
    /**
     * 解码进度监听器
     */
    private final WaveFormDecoderProgressListener progressListener = new WaveFormDecoderProgressListener();
    /**
     * 处理完毕后得到的归一化波形数据（0~1 之间浮点数组，长度通常为可视化宽度）
     */
    private float[] resultingWaveform;
    /**
     * 原始 WAV PCM 振幅数据（整型数组），用于后续转换
     */
    private int[] wavAmplitudes;
    /**
     * 当前处理的音频文件路径
     */
    private String fileAbsolutePath;
    /**
     * 临时文件引用，处理完后用于删除
     */
    private File temp1;
    private File temp2;
    /**
     * 当前使用的音频解码器
     */
    private AudioDecoder currentDecoder;
    /**
     * 当前任务类型
     */
    private WaveFormJob waveFormJob;

    /**
     * 构造方法
     *
     * @param waveVisualization 外部可视化器实例，用于任务完成后的回调
     */
    public WaveFormService(WaveVisualization waveVisualization) {
        this.waveVisualization = waveVisualization;
        // 成功/失败/取消时回调
        setOnSucceeded(s -> done());
        setOnFailed(f -> failure());
        setOnCancelled(c -> failure());
    }

    /**
     * 启动服务线程，传入文件路径和任务类型。
     * 如果当前正在执行 WAVEFORM（仅绘图）任务，则先取消；然后停止绘制服务，重置状态后 restart().
     *
     * @param fileAbsolutePath 要处理的音频文件绝对路径
     * @param waveFormJob      任务类型
     */
    public void startService(String fileAbsolutePath, WaveFormJob waveFormJob) {
        if (waveFormJob == WaveFormJob.WAVEFORM) {
            // 若是纯波形生成，先取消可能正在运行的相同任务
            cancel();
        }
        // 停止可视化绘制，等待新数据
        waveVisualization.stopPainterService();

        this.waveFormJob = waveFormJob;
        this.fileAbsolutePath = fileAbsolutePath;
        if (waveFormJob != WaveFormJob.WAVEFORM) {
            // 若不是仅生成波形，重置振幅数组
            this.wavAmplitudes = null;
        }
        // 重启 Service，createTask() 会被调用
        restart();
    }

    /**
     * 成功完成后回调：将 resultingWaveform 提交给可视化器，并启动绘制；同时删除临时文件
     */
    private void done() {
        waveVisualization.setWaveData(resultingWaveform);
        waveVisualization.startPainterService();
        deleteTemporaryFiles();
    }

    /**
     * 失败或取消时回调，清理临时文件
     */
    private void failure() {
        deleteTemporaryFiles();
    }

    /**
     * 删除临时文件；在 done() 或 failure() 时调用
     */
    private void deleteTemporaryFiles() {
        if (temp1 != null) {
            try {
                if (temp1.delete()) {
                    System.out.println("已删除临时文件: " + temp1.getAbsolutePath());
                } else if (temp1.exists()) {
                    System.err.println("无法删除临时文件: " + temp1.getAbsolutePath());
                }
            } catch (SecurityException e) {
                System.err.println("安全权限错误，无法删除临时文件: " + temp1.getAbsolutePath() + ", 错误: " + e.getMessage());
            }
            temp1 = null;
        }
        if (temp2 != null) {
            try {
                if (temp2.delete()) {
                    System.out.println("已删除临时文件: " + temp2.getAbsolutePath());
                } else if (temp2.exists()) {
                    System.err.println("无法删除临时文件: " + temp2.getAbsolutePath());
                }
            } catch (SecurityException e) {
                System.err.println("安全权限错误，无法删除临时文件: " + temp2.getAbsolutePath() + ", 错误: " + e.getMessage());
            }
            temp2 = null;
        }
    }

    /**
     * 重写 Service 的 createTask()，执行实际的波形数据处理逻辑
     */
    @Override
    protected Task<Boolean> createTask() {
        return new Task<Boolean>() {
            @Override
            protected Boolean call() {
                try {
                    if (waveFormJob == WaveFormJob.AMPLITUDES_AND_WAVEFORM) {
                        // 从音频文件处理：使用解码器解码，提取振幅，生成波形
                        System.out.println("AMPLITUDES_AND_WAVEFORM");
                        resultingWaveform = processFromAnyAudioFile();
                    } else if (waveFormJob == WaveFormJob.WAVEFORM) {
                        // 仅根据已有 wavAmplitudes 生成可视化波形数组
                        resultingWaveform = processAmplitudes(wavAmplitudes);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // 特殊提示：磁盘空间不足
                    if (ex.getMessage() != null && ex.getMessage().contains("There is not enough space on the disk")) {
                        System.err.println("Not enough disk space");
                    }
                    return false;
                }
                return true;
            }

            /**
             * 处理任意格式的音频文件（使用插件化解码器）
             * @return 归一化后的波形数组
             * @throws IOException IO异常
             * @throws UnsupportedAudioFileException 不支持的音频文件
             * @throws DecoderException 解码异常
             */
            private float[] processFromAnyAudioFile()
                    throws IOException, UnsupportedAudioFileException, DecoderException {

                File sourceFile = new File(fileAbsolutePath);

                // 自动选择解码器
                currentDecoder = AudioDecoderFactory.getInstance().getDecoder(sourceFile);
                if (currentDecoder == null) {
                    throw new DecoderException(
                            "未找到支持该文件格式的解码器: " + sourceFile.getName(),
                            DecoderException.ErrorCode.UNSUPPORTED_FORMAT
                    );
                }

                System.out.println("使用解码器: " + currentDecoder.getMetadata().name());

                // 执行解码，解码器内部创建临时文件并返回
                File temporalDecodedFile = currentDecoder.decode(sourceFile, progressListener);
                temp1 = temporalDecodedFile;

                // 提取振幅（使用现有方法）
                if (wavAmplitudes == null) {
                    wavAmplitudes = getWavAmplitudes(temporalDecodedFile);
                }

                // 清理临时文件
                temporalDecodedFile.delete();

                // 生成波形数据（使用现有方法）
                return processAmplitudes(wavAmplitudes);
            }

            /**
             * 从 WAV 文件中提取 PCM 振幅数组。读取 PCM 数据，按块累加，缩减尺寸到固定长度。
             * @param file WAV 文件
             * @return 整型振幅数组
             * @throws UnsupportedAudioFileException
             * @throws IOException
             */
            private int[] getWavAmplitudes(File file)
                    throws UnsupportedAudioFileException, IOException {
                System.out.println("Calculating WAV amplitudes");
                try (AudioInputStream input = AudioSystem.getAudioInputStream(file)) {
                    AudioFormat baseFormat = input.getFormat();

                    // 检查是否已经是PCM_SIGNED格式，如果是则直接使用，否则转换
                    AudioInputStream pcmDecodedInput;
                    if (baseFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED ||
                            baseFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
                        pcmDecodedInput = input;
                    } else {
                        // 目标解码格式：PCM_SIGNED、16 位、相同采样率和通道数
                        AudioFormat decodedFormat = new AudioFormat(
                                AudioFormat.Encoding.PCM_SIGNED,
                                baseFormat.getSampleRate(),
                                16,
                                baseFormat.getChannels(),
                                baseFormat.getFrameSize(),
                                baseFormat.getFrameRate(),
                                baseFormat.isBigEndian());
                        pcmDecodedInput = AudioSystem.getAudioInputStream(decodedFormat, input);
                    }

                    // 获取音频总帧数来估算数组大小
                    long totalFrames = pcmDecodedInput.getFrameLength();
                    int maxArrayLength = Math.min(100000, (int) Math.max(1000, totalFrames));

                    // 使用更精确的方法计算样本数
                    int bytesPerFrame = pcmDecodedInput.getFormat().getFrameSize();
                    int bitsPerSample = pcmDecodedInput.getFormat().getSampleSizeInBits();
                    int channels = pcmDecodedInput.getFormat().getChannels();

                    final int BUFFER_SIZE = 8192; // 增大缓冲区提高效率
                    byte[] buffer = new byte[BUFFER_SIZE];

                    // 将全部数据缩减到固定长度数组，避免过大
                    int[] finalAmplitudes = new int[maxArrayLength];
                    int totalSamples = (int) (totalFrames * channels);
                    int samplesPerPixel = Math.max(1, totalSamples / maxArrayLength);

                    // 累加临时变量
                    int currentSampleCounter = 0;
                    int arrayCellPosition = 0;
                    long currentCellValue = 0; // 使用long避免溢出
                    int sampleValue;
                    int samplesProcessed = 0;

                    // 逐块读取 PCM 数据
                    int bytesRead;
                    while ((bytesRead = pcmDecodedInput.read(buffer)) > 0) {
                        // 根据位深度和字节序处理样本
                        for (int i = 0; i < bytesRead; i += (bitsPerSample / 8)) {
                            if (i + (bitsPerSample / 8) > bytesRead) {
                                break; // 避免越界
                            }

                            // 根据位深度和字节序解析样本值
                            if (bitsPerSample == 16) {
                                if (pcmDecodedInput.getFormat().isBigEndian()) {
                                    sampleValue = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
                                } else {
                                    sampleValue = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                                }
                            } else if (bitsPerSample == 8) {
                                sampleValue = buffer[i];
                            } else {
                                // 对于其他位深度，简单地取中间值
                                sampleValue = 0;
                                for (int j = 0; j < bitsPerSample / 8; j++) {
                                    sampleValue |= (buffer[i + j] & 0xFF) << (j * 8);
                                }
                                // 归一化到16位范围
                                if (bitsPerSample > 16) {
                                    sampleValue >>= (bitsPerSample - 16);
                                }
                            }

                            // 累加绝对值
                            if (currentSampleCounter < samplesPerPixel) {
                                currentSampleCounter++;
                                currentCellValue += Math.abs(sampleValue);
                                samplesProcessed++;
                            } else {
                                // 当达到一个"像素"对应的采样数量时写入结果数组
                                if (arrayCellPosition < maxArrayLength) {
                                    int averaged = (int) (currentCellValue / Math.max(1, samplesPerPixel));
                                    finalAmplitudes[arrayCellPosition] = averaged;
                                }
                                // 重置计数，移动到下一个位置
                                currentSampleCounter = 0;
                                currentCellValue = 0;
                                arrayCellPosition++;

                                // 如果超出数组长度，退出循环
                                if (arrayCellPosition >= maxArrayLength) {
                                    break;
                                }
                            }
                        }

                        if (arrayCellPosition >= maxArrayLength) {
                            break;
                        }
                    }

                    // 处理剩余未写入的数据
                    if (currentSampleCounter > 0 && arrayCellPosition < maxArrayLength) {
                        int averaged = (int) (currentCellValue / currentSampleCounter);
                        finalAmplitudes[arrayCellPosition] = averaged;
                    }

                    return finalAmplitudes;
                } catch (UnsupportedAudioFileException | IOException ex) {
                    System.err.println("音频文件格式不受支持: " + ex.getMessage());
                    ex.printStackTrace();
                    throw ex;
                } catch (Exception ex) {
                    System.err.println("处理音频文件时发生错误: " + ex.getMessage());
                    ex.printStackTrace();
                    throw new IOException("音频处理错误", ex);
                }
            }

            /**
             * 根据整型振幅数组生成归一化波形 float 数组，长度等于可视化宽度，
             * 每个像素位置对应若干采样，取平均后归一化 (除以 65536.0f)。
             * @param sourcePcmData 振幅整型数组
             * @return 归一化波形数据，值大约在 0.0 ~ 1.0 之间
             */
            private float[] processAmplitudes(int[] sourcePcmData) {
                System.out.println("Processing WAV amplitudes");

                // 检查源数据是否为null
                if (sourcePcmData == null || sourcePcmData.length == 0) {
                    System.err.println("警告: 源PCM数据为空，返回默认波形");
                    int width = waveVisualization.width;
                    float[] defaultWaveData = new float[Math.max(1, width)];
                    // 填充默认值（中线）
                    Arrays.fill(defaultWaveData, 0.28802148f);
                    return defaultWaveData;
                }

                int width = waveVisualization.width;
                // 结果数组长度 = 可视化宽度
                float[] waveData = new float[Math.max(1, width)];
                int samplesPerPixel = Math.max(1, sourcePcmData.length / width);

                for (int w = 0; w < waveData.length; w++) {
                    int offset = w * samplesPerPixel;
                    float sum = 0.0f;
                    // 累加该像素对应的所有采样
                    for (int s = 0; s < samplesPerPixel && (offset + s) < sourcePcmData.length; s++) {
                        sum += Math.abs(sourcePcmData[offset + s]) / 65536.0f;
                    }
                    // 平均后赋值
                    waveData[w] = sum / samplesPerPixel;
                }
                System.out.println("Finished Processing amplitudes");
                return waveData;
            }
        };
    }

    public String getFileAbsolutePath() {
        return fileAbsolutePath;
    }

    public void setFileAbsolutePath(String fileAbsolutePath) {
        this.fileAbsolutePath = fileAbsolutePath;
    }

    // Getter / Setter

    public int[] getWavAmplitudes() {
        return wavAmplitudes;
    }

    public float[] getResultingWaveform() {
        return resultingWaveform;
    }

    public void setResultingWaveform(float[] resultingWaveform) {
        this.resultingWaveform = resultingWaveform;
    }

    /**
     * WaveService 要执行的任务类型：
     * AMPLITUDES_AND_WAVEFORM: 从音频文件先解码并提取振幅数组并生成波形
     * WAVEFORM: 已有振幅数组时，仅基于 wavAmplitudes 生成可视化波形数据
     */
    public enum WaveFormJob {
        AMPLITUDES_AND_WAVEFORM,
        WAVEFORM
    }

    /**
     * 解码进度监听器，实现 DecoderProgressListener，用于打印日志。
     */
    public static class WaveFormDecoderProgressListener implements DecoderProgressListener {

        @Override
        public void onProgress(double progress) {
            System.out.println("解码进度: " + (progress * 100) + "%");
        }

        @Override
        public void onMessage(String message) {
            System.out.println("解码消息: " + message);
        }

        @Override
        public void onStart(String sourceFile) {
            System.out.println("开始解码: " + sourceFile);
        }

        @Override
        public void onComplete(String destinationFile) {
            System.out.println("解码完成: " + destinationFile);
        }

        @Override
        public void onError(String error) {
            System.err.println("解码错误: " + error);
        }
    }
}
