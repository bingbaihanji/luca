package com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget;

import com.bingbaihanji.luca.plugin.audio.spectrum.controller.AudioPlayerController;
import com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.waveform.WaveFormService;
import com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.waveform.WaveVisualization;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.io.File;

/**
 * 波形预览组件，将整首音频的振幅包络绘制为静态波形图。
 *
 * <p>使用 {@link WaveVisualization} 作为底层 Canvas，大小跟随父容器自动调整。
 * 通过 {@link AudioPlayerController} 单例监听换歌事件，每次加载新文件后
 * 自动重新解码并绘制波形。
 *
 * @author bingbaihanji
 */
public class WaveformWidget extends HBox {

    private static final int INITIAL_WIDTH = 800;
    private static final int INITIAL_HEIGHT = 80;

    private static final AudioPlayerController ctrl = AudioPlayerController.getInstance();

    private final WaveVisualization waveVisualization;

    public WaveformWidget() {
        waveVisualization = new WaveVisualization(INITIAL_WIDTH, INITIAL_HEIGHT, null);

        // HBox 布局时会调用 Canvas 的 resize()，让 Canvas 随父容器撑满
        HBox.setHgrow(waveVisualization, Priority.ALWAYS);
        getChildren().add(waveVisualization);

        setPrefHeight(INITIAL_HEIGHT);
        setMaxWidth(Double.MAX_VALUE);

        // Canvas 跟随 HBox 尺寸变化
        widthProperty().addListener((obs, o, n) -> waveVisualization.setWidth(n.doubleValue()));
        heightProperty().addListener((obs, o, n) -> waveVisualization.setHeight(n.doubleValue()));

        waveVisualization.startPainterService();

        // 若初始化时已有解码文件（如组件延迟创建），立即加载
        File initialWav = ctrl.getCurrentDecodedWavFile();
        if (initialWav != null) {
            waveVisualization.setMediaPlayer(ctrl.getMediaPlayer());
            waveVisualization.getWaveService().startService(
                    initialWav.getAbsolutePath(), WaveFormService.WaveFormJob.AMPLITUDES_AND_WAVEFORM);
        }

        // 每次换歌后重新绑定 MediaPlayer 并重绘波形
        ctrl.addOnPlayerReady(player -> {
            File wav = ctrl.getCurrentDecodedWavFile();
            if (wav != null) {
                javafx.application.Platform.runLater(() -> {
                    waveVisualization.setMediaPlayer(player);
                    waveVisualization.getWaveService().startService(
                            wav.getAbsolutePath(), WaveFormService.WaveFormJob.AMPLITUDES_AND_WAVEFORM);
                });
            }
        });
    }
}
