package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.builder;

/**
 * 默认音频控制器实现
 * <p>
 * 提供基本的音频播放控制功能。
 * </p>
 *
 * @author bingbaihanji
 * @date 2026-03-21
 */
public class DefaultAudioController implements AudioController {

    private VisualizerKit kit;

    @Override
    public void setKit(VisualizerKit kit) {
        this.kit = kit;
    }

    @Override
    public void play() {
        if (kit != null) {
            kit.start();
        }
    }

    @Override
    public void pause() {
        if (kit != null) {
            kit.pause();
        }
    }

    @Override
    public void resume() {
        if (kit != null) {
            kit.resume();
        }
    }

    @Override
    public void stop() {
        if (kit != null) {
            kit.stop();
        }
    }

    @Override
    public boolean isPlaying() {
        return kit != null && kit.isPlaying();
    }
}
