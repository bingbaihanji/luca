package com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.node;

import com.bingbaihanji.luca.plugin.audio.spectrum.controller.AudioPlayerController;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.spectrum.SpectrumTubeView;
import com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.PlayerInfoPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.nio.file.Path;

/**
 * 发光试管频谱可视化面板。
 *
 * <p>通过 {@link AudioPlayerController} 单例与其他可视化视图共享播放状态，
 * 底部控制区由 {@link PlayerInfoPanel} 统一提供。
 *
 * @author bingbaihanji
 */
public class TubeSpectrumViewNode extends BorderPane {

    private static final AudioPlayerController ctrl = AudioPlayerController.getInstance();

    private final SpectrumTubeView spectrumView;

    public TubeSpectrumViewNode() {
        spectrumView = new SpectrumTubeView(400.0, 200.0);
        ctrl.addVisualizer(spectrumView);

        setCenter(buildSpectrumContainer());
        setBottom(new PlayerInfoPanel(ctrl));
        setStyle("-fx-background-color: #0a0e14;");
    }

    // ── 公共 API ──────────────────────────────────────────────────

    /**
     * 判断给定路径是否为受支持的音频文件。
     */
    public static boolean isAudioFile(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && ctrl.getSupportedAudioExtensions().contains(name.substring(dot + 1));
    }

    /**
     * 通过共享控制器加载并播放音频。
     */
    public void loadAndPlay(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || !ctrl.getSupportedAudioExtensions().contains(name.substring(dot + 1))) {
            return;
        }
        ctrl.loadAudioFile(file);
        if (!spectrumView.isRunning()) {
            spectrumView.start();
        }
    }

    // ── 私有方法 ──────────────────────────────────────────────────

    /**
     * 构建自适应大小的频谱容器，内边距 20px，频谱组件居中铺满。
     */
    private StackPane buildSpectrumContainer() {
        var container = new StackPane(spectrumView);
        container.setPadding(new Insets(20));
        StackPane.setAlignment(spectrumView, Pos.CENTER);
        container.widthProperty().addListener((obs, o, w) ->
                spectrumView.resize(w.doubleValue() - 40, container.getHeight() - 40));
        container.heightProperty().addListener((obs, o, h) ->
                spectrumView.resize(container.getWidth() - 40, h.doubleValue() - 40));
        return container;
    }
}
