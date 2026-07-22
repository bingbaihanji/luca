package com.bingbaihanji.luca.plugin.audio.spectrum;

import com.bingbaihanji.luca.plugin.audio.spectrum.controller.AudioPlayerController;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderPluginLoader;
import com.bingbaihanji.sunsen.api.annotation.Plugin;
import com.bingbaihanji.sunsen.api.support.AbstractPlugin;

/**
 * 音频频谱可视化插件入口，通过 Sunsen 插件框架注册到 luca UI 平台。
 *
 * <p>提供多种音频可视化效果（圆形频谱、发光试管、波形示波器等）及均衡器功能，
 * 支持 WAV、MP3、FLAC、APE 等音频格式。
 *
 * <p>生命周期：
 * <ul>
 *   <li>{@link #onInitialized()} — 加载内置音频解码器</li>
 *   <li>{@link #onStop()} — 释放播放器资源并清空监听器</li>
 * </ul>
 *
 * @author bingbaihanji
 */
@Plugin(
        id = "com.bingbaihanji.luca.plugin.audio.spectrum",
        name = "AudioSpectrum",
        version = "1.0.0",
        packagePrefixes = {"com.bingbaihanji.luca.plugin.audio.spectrum"}
)
public class LucaAudioSpectrumPlugin extends AbstractPlugin {

    private static final AudioPlayerController audioController = AudioPlayerController.getInstance();

    @Override
    protected void onInitialized() {
        DecoderPluginLoader.loadBuiltInDecoders();
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
        audioController.shutdown();
    }

    @Override
    public void onDestroy() {
    }
}
