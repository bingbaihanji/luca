package com.bingbaihanji.luca.plugin.audio.spectrum.decoder.impl;


import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.AudioDecoder;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderException;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderMetadata;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderProgressListener;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * WAV 格式解码器实现
 * 使用 javax.sound.sampled 处理 WAV 文件，智能转换为标准格式
 */
public class WavDecoder implements AudioDecoder {

    private static final String FORMAT_NAME = "wav";
    private static final String[] EXTENSIONS = {"wav", "wave"};

    // 目标音频格式参数
    private static final int TARGET_SAMPLE_RATE = 44100;
    private static final int TARGET_CHANNELS = 2;
    private static final int TARGET_SAMPLE_SIZE_BITS = 16;

    @Override
    public String getSupportedFormat() {
        return FORMAT_NAME;
    }

    @Override
    public String[] getSupportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public boolean canDecode(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        // 检查扩展名
        String fileName = file.getName().toLowerCase();
        for (String ext : EXTENSIONS) {
            if (fileName.endsWith("." + ext)) {
                // 进一步验证是否是有效的 WAV 文件
                try {
                    AudioSystem.getAudioInputStream(file);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public File decode(File sourceFile, DecoderProgressListener listener)
            throws DecoderException, IOException {

        if (listener != null) {
            listener.onStart(sourceFile.getName());
            listener.onMessage("开始处理 WAV 文件: " + sourceFile.getName());
        }

        try {
            // 创建临时目标文件
            File destinationFile = File.createTempFile("wav_decoded_", ".wav");
            destinationFile.deleteOnExit();

            // 读取源 WAV 文件
            AudioInputStream sourceStream = AudioSystem.getAudioInputStream(sourceFile);
            AudioFormat sourceFormat = sourceStream.getFormat();

            if (listener != null) {
                listener.onProgress(0.2);
                listener.onMessage("源格式: " + formatToString(sourceFormat));
            }

            // 检查是否需要转换格式
            if (isTargetFormat(sourceFormat)) {
                // 格式已满足要求，直接复制
                if (listener != null) {
                    listener.onMessage("格式已符合要求，直接复制文件");
                }
                Files.copy(sourceFile.toPath(), destinationFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                if (listener != null) {
                    listener.onProgress(1.0);
                }
            } else {
                // 需要转换为标准 PCM 格式
                if (listener != null) {
                    listener.onMessage("转换为标准 PCM 格式");
                    listener.onProgress(0.4);
                }
                convertToTargetFormat(sourceStream, destinationFile, listener);
            }

            sourceStream.close();

            if (listener != null) {
                listener.onComplete(destinationFile.getName());
            }

            return destinationFile;

        } catch (UnsupportedAudioFileException e) {
            if (listener != null) {
                listener.onError("不支持的 WAV 文件格式");
            }
            throw new DecoderException(
                    "不支持的 WAV 文件格式: " + e.getMessage(),
                    DecoderException.ErrorCode.UNSUPPORTED_FORMAT,
                    e
            );
        } catch (IOException e) {
            if (listener != null) {
                listener.onError("IO 错误: " + e.getMessage());
            }
            if (e.getMessage() != null && e.getMessage().contains("space")) {
                throw new DecoderException(
                        "磁盘空间不足",
                        DecoderException.ErrorCode.INSUFFICIENT_DISK_SPACE,
                        e
                );
            }
            throw new DecoderException(
                    "IO 错误: " + e.getMessage(),
                    DecoderException.ErrorCode.IO_ERROR,
                    e
            );
        }
    }

    /**
     * 检查音频格式是否符合目标格式
     */
    private boolean isTargetFormat(AudioFormat format) {
        return format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED &&
                format.getSampleRate() == TARGET_SAMPLE_RATE &&
                format.getChannels() == TARGET_CHANNELS &&
                format.getSampleSizeInBits() == TARGET_SAMPLE_SIZE_BITS &&
                !format.isBigEndian();
    }

    /**
     * 将音频流转换为目标格式
     */
    private void convertToTargetFormat(AudioInputStream sourceStream, File destinationFile,
                                       DecoderProgressListener listener)
            throws IOException, DecoderException {

        AudioFormat sourceFormat = sourceStream.getFormat();

        // 构建目标格式：PCM_SIGNED, 16-bit, 44100Hz, 2 channels, little-endian
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                TARGET_SAMPLE_RATE,
                TARGET_SAMPLE_SIZE_BITS,
                TARGET_CHANNELS,
                TARGET_CHANNELS * (TARGET_SAMPLE_SIZE_BITS / 8), // frame size
                TARGET_SAMPLE_RATE,
                false // little-endian
        );

        // 检查是否支持转换
        if (!AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            throw new DecoderException(
                    "不支持从 " + formatToString(sourceFormat) + " 转换到目标格式",
                    DecoderException.ErrorCode.UNSUPPORTED_FORMAT
            );
        }

        // 执行转换
        try (AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream)) {

            if (listener != null) {
                listener.onProgress(0.6);
            }

            // 写入目标文件
            AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, destinationFile);

            if (listener != null) {
                listener.onProgress(1.0);
                listener.onMessage("转换完成");
            }

        } catch (Exception e) {
            throw new DecoderException(
                    "格式转换失败: " + e.getMessage(),
                    DecoderException.ErrorCode.DECODING_FAILED,
                    e
            );
        }
    }

    /**
     * 格式化音频格式为字符串
     */
    private String formatToString(AudioFormat format) {
        return String.format("%s, %.1f Hz, %d bit, %d channels",
                format.getEncoding(),
                format.getSampleRate(),
                format.getSampleSizeInBits(),
                format.getChannels()
        );
    }

    @Override
    public DecoderMetadata getMetadata() {
        return new DecoderMetadata(
                "WAV Decoder",
                "1.0.0",
                "Java Audio System",
                "Built-in WAV format decoder using javax.sound.sampled"
        );
    }
}
