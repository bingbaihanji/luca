package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.audio;


import com.bingbaihanji.luca.plugin.audio.spectrum.utils.audio.AudioFormatUtils;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.datastructure.RingBuffer;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;

/**
 * 音频后端，负责从系统音频设备捕获音频数据
 * <p>
 * 使用 javax.sound.sampled API
 */
public class AudioBackend {

    public static final float SAMPLE_RATE = 48000f;
    public static final int CHANNELS = 1;  // Mono
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int BUFFER_SIZE = 512;  // frames per buffer
    public static final boolean SIGNED = true;
    public static final boolean BIG_ENDIAN = false;
    private static final Logger logger = LoggerFactory.getLogger(AudioBackend.class);
    private final RingBuffer ringBuffer;
    private final AudioBuffer audioBuffer;
    // 音频格式
    private final AudioFormat audioFormat = new AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            SIGNED,
            BIG_ENDIAN
    );
    private TargetDataLine targetDataLine;
    private Thread captureThread;
    private volatile boolean isRunning = false;

    /**
     * 创建音频后端
     *
     * @param ringBuffer  环形缓冲区
     * @param audioBuffer 音频缓冲区
     */
    public AudioBackend(RingBuffer ringBuffer, AudioBuffer audioBuffer) {
        this.ringBuffer = ringBuffer;
        this.audioBuffer = audioBuffer;
    }

    /**
     * 初始化音频设备
     */
    public void initialize() {
        try {
            logger.info("Initializing audio backend...");
            logger.info("Audio format: {} Hz, {} bit, {} channels", SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS);

            // 获取系统默认音频输入设备
            Mixer.Info mixerInfo = getDefaultInputDevice();
            logger.info("Using audio device: {}", mixerInfo != null ? mixerInfo.getName() : "default");

            // 获取并打开 TargetDataLine
            if (mixerInfo != null) {
                targetDataLine = AudioSystem.getTargetDataLine(audioFormat, mixerInfo);
            } else {
                targetDataLine = AudioSystem.getTargetDataLine(audioFormat);
            }

            // 打开 data line
            int bufferSizeBytes = BUFFER_SIZE * CHANNELS * (SAMPLE_SIZE_BITS / 8);
            targetDataLine.open(audioFormat, bufferSizeBytes * 4); // 4× buffer for safety

            logger.info("Audio backend initialized successfully");
        } catch (LineUnavailableException e) {
            logger.error("Audio line unavailable", e);
            throw new RuntimeException("Failed to initialize audio backend", e);
        }
    }

    /**
     * 启动音频捕获
     */
    public void start() {
        if (isRunning) {
            logger.warn("Audio backend is already running");
            return;
        }

        if (targetDataLine != null) {
            isRunning = true;
            targetDataLine.start();
            logger.info("Audio capture started");

            // 启动捕获线程
            captureThread = new Thread(this::captureLoop, "AudioCaptureThread");
            captureThread.setDaemon(true);
            captureThread.setPriority(Thread.MAX_PRIORITY);  // 高优先级避免丢帧
            captureThread.start();
        } else {
            logger.error("Cannot start audio capture: TargetDataLine is null");
        }
    }

    /**
     * 停止音频捕获
     */
    public void stop() {
        if (!isRunning) {
            return;
        }

        logger.info("Stopping audio capture...");
        isRunning = false;

        try {
            if (captureThread != null) {
                captureThread.join(1000); // 等待线程退出
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for capture thread", e);
        }

        if (targetDataLine != null) {
            targetDataLine.stop();
        }
        logger.info("Audio capture stopped");
    }

    /**
     * 关闭音频设备
     */
    public void close() {
        stop();
        if (targetDataLine != null) {
            targetDataLine.close();
        }
        logger.info("Audio backend closed");
    }

    /**
     * 音频捕获循环（在后台线程运行）
     */
    private void captureLoop() {
        int byteBufferSize = BUFFER_SIZE * CHANNELS * (SAMPLE_SIZE_BITS / 8);
        byte[] byteBuffer = new byte[byteBufferSize];
        double timestamp = 0.0;
        double timestampIncrement = BUFFER_SIZE / (double) SAMPLE_RATE;

        logger.info("Entering capture loop (buffer size: {} bytes)", byteBufferSize);

        // UI 更新节流：每 50ms 更新一次 UI（降低频率）
        long lastUIUpdateTime = System.nanoTime();
        long uiUpdateIntervalNanos = 50_000_000L;  // 50ms = 20 FPS for UI updates

        try {
            while (isRunning) {
                // 读取音频数据
                int bytesRead = targetDataLine.read(byteBuffer, 0, byteBufferSize);

                if (bytesRead > 0) {
                    // 转换为 Double 数组（归一化到 [-1.0, 1.0]）
                    double[] samples = bytesToDoubleArray(byteBuffer, bytesRead);

                    // 推送到 RingBuffer（在音频线程，快速）
                    ringBuffer.push(samples, timestamp);

                    // 节流 UI 更新
                    long now = System.nanoTime();
                    if (now - lastUIUpdateTime >= uiUpdateIntervalNanos) {
                        // 通知 AudioBuffer（触发 UI 更新）
                        final double[] finalSamples = samples;
                        final double finalTimestamp = timestamp;
                        Platform.runLater(() -> {
                            audioBuffer.handleNewData(finalSamples, finalTimestamp);
                        });
                        lastUIUpdateTime = now;
                    }

                    timestamp += timestampIncrement;
                }
            }
        } catch (Exception e) {
            logger.error("Error in capture loop", e);
        }

        logger.info("Exiting capture loop");
    }

    /**
     * 将字节数组转换为 Double 数组（归一化）
     *
     * @param byteArray 字节数组（16-bit signed PCM, little-endian）
     * @param length    有效字节数
     * @return 归一化的 Double 数组 [-1.0, 1.0]
     */
    private double[] bytesToDoubleArray(byte[] byteArray, int length) {
        return AudioFormatUtils.bytesToDoubleArray(byteArray, length);
    }

    /**
     * 获取系统默认输入设备
     */
    private Mixer.Info getDefaultInputDevice() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        logger.debug("Available audio devices:");
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] targetLines = mixer.getTargetLineInfo();

            if (targetLines.length > 0) {
                logger.debug("  - {}: {} target lines", mixerInfo.getName(), targetLines.length);

                // 尝试获取支持我们格式的 line
                try {
                    if (mixer.isLineSupported(dataLineInfo)) {
                        Line line = mixer.getLine(dataLineInfo);
                        if (line instanceof TargetDataLine) {
                            logger.info("Found compatible device: {}", mixerInfo.getName());
                            return mixerInfo;
                        }
                    }
                } catch (Exception e) {
                    // 不支持该格式
                }
            }
        }

        logger.info("Using system default audio device");
        return null;
    }

    /**
     * 列出所有可用的输入设备
     */
    public java.util.List<Mixer.Info> listInputDevices() {
        java.util.List<Mixer.Info> result = new java.util.ArrayList<>();
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.getTargetLineInfo().length > 0) {
                result.add(mixerInfo);
            }
        }
        return result;
    }

    /**
     * 获取当前音频延迟（毫秒）
     */
    public double getLatencyMs() {
        if (targetDataLine == null) {
            return 0.0;
        }
        int bufferSize = targetDataLine.getBufferSize();
        int frameSize = audioFormat.getFrameSize();
        int framesInBuffer = bufferSize / frameSize;
        return framesInBuffer / (double) SAMPLE_RATE * 1000.0;
    }

    /**
     * 检查音频捕获是否正在运行
     */
    public boolean isCapturing() {
        return isRunning;
    }
}
