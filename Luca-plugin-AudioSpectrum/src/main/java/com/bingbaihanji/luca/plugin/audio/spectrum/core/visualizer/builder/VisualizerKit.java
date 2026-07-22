package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.builder;


import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioVisualizer;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.audio.AudioFormatUtils;
import javafx.application.Platform;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;

/**
 * 可视化器工具包
 * <p>
 * 封装了音频可视化器、音频流、音频线路和控制器，
 * 提供统一的音频播放和可视化更新功能。
 * </p>
 *
 * @author bingbaihanji
 * @date 2026-03-21
 */
public class VisualizerKit {

    private final AudioVisualizer visualizer;
    private final AudioInputStream audioInputStream;
    private final SourceDataLine audioLine;
    private final AudioController controller;

    private volatile boolean playing = false;
    private volatile boolean stopped = false;
    private Thread playbackThread;

    /**
     * 创建一个新的 VisualizerKit
     *
     * @param visualizer       音频可视化器
     * @param audioInputStream 音频输入流
     * @param audioLine        音频输出线路
     * @param controller       音频控制器
     */
    public VisualizerKit(AudioVisualizer visualizer,
                         AudioInputStream audioInputStream,
                         SourceDataLine audioLine,
                         AudioController controller) {
        this.visualizer = visualizer;
        this.audioInputStream = audioInputStream;
        this.audioLine = audioLine;
        this.controller = controller;

        // 将控制器与此工具包关联
        controller.setKit(this);
    }

    /**
     * 获取可视化器
     *
     * @return 音频可视化器
     */
    public AudioVisualizer getVisualizer() {
        return visualizer;
    }

    /**
     * 获取音频控制器
     *
     * @return 音频控制器
     */
    public AudioController getController() {
        return controller;
    }

    /**
     * 启动可视化器并开始播放音频
     */
    public void start() {
        if (!playing) {
            playing = true;
            visualizer.start();

            // 启动播放线程
            playbackThread = new Thread(this::audioPlaybackLoop, "AudioPlayback");
            playbackThread.setDaemon(true);
            playbackThread.start();
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        playing = false;
    }

    /**
     * 恢复播放
     */
    public void resume() {
        if (!stopped && !playing && playbackThread != null && playbackThread.isAlive()) {
            playing = true;
        }
    }

    /**
     * 停止播放并释放资源
     */
    public void stop() {
        if (stopped) {
            return; // 避免重复停止
        }

        playing = false;
        stopped = true;
        visualizer.stop();

        // 等待播放线程结束（最多等待1秒）
        if (playbackThread != null && playbackThread.isAlive()) {
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 关闭资源
        try {
            if (audioInputStream != null) {
                audioInputStream.close();
            }
            if (audioLine != null) {
                audioLine.stop();
                audioLine.close();
            }
        } catch (IOException e) {
            // 静默处理，因为可能已经被关闭
        }
    }

    /**
     * 检查是否正在播放
     *
     * @return 如果正在播放返回 true，否则返回 false
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * 音频播放循环
     */
    private void audioPlaybackLoop() {
        byte[] buffer = new byte[4096];
        AudioFormat format = audioInputStream.getFormat();
        int channels = format.getChannels();
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;

        try {
            int bytesRead;
            while (!stopped && (bytesRead = audioInputStream.read(buffer)) != -1) {
                // 暂停时等待，但如果 stopped 则退出
                while (!playing && !stopped) {
                    Thread.sleep(100);
                }

                // 如果已停止，退出循环
                if (stopped) {
                    break;
                }

                // 播放音频
                audioLine.write(buffer, 0, bytesRead);

                // 转换为可视化数据
                float[] leftChannel = new float[1024];
                float[] rightChannel = new float[1024];
                AudioFormatUtils.decodeStereo(buffer, bytesRead, format, leftChannel, rightChannel);


                // 更新可视化器
                updateVisualizer(leftChannel, rightChannel);
            }
        } catch (IOException e) {
            // 如果是因为流被关闭（正常停止），则静默退出
            if (!stopped) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 更新可视化器（线程安全）
     */
    private void updateVisualizer(float[] left, float[] right) {
        Platform.runLater(() -> visualizer.updateAudioData(left, right));
    }

}
