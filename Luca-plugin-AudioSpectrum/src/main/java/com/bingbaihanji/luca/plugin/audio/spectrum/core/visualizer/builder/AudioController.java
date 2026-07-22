package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.builder;

/**
 * 音频控制器接口
 * <p>
 * 定义音频播放控制的基本操作，如播放、暂停、停止等。
 * </p>
 *
 * @author bingbaihanji
 * @date 2026-03-21
 */
public interface AudioController {

    /**
     * 设置关联的 VisualizerKit
     *
     * @param kit 可视化器工具包
     */
    void setKit(VisualizerKit kit);

    /**
     * 播放音频
     */
    void play();

    /**
     * 暂停播放
     */
    void pause();

    /**
     * 恢复播放
     */
    void resume();

    /**
     * 停止播放
     */
    void stop();

    /**
     * 检查是否正在播放
     *
     * @return 如果正在播放返回 true，否则返回 false
     */
    boolean isPlaying();
}
