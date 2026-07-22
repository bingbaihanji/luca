package com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.node;

import com.bingbaihanji.luca.plugin.audio.spectrum.controller.AudioPlayerController;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioVisualizer;
import com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.PlayerInfoPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.nio.file.Path;

/**
 * 通用 PCM 音频可视化面板，可包装任意 {@link AudioVisualizer} 实现。
 *
 * <p>构造时向 {@link AudioPlayerController} 单例注册可视化器，
 * 播放状态与其他视图节点完全共享。
 * 底部控制区由 {@link PlayerInfoPanel} 统一提供。
 *
 * @param <T> {@link AudioVisualizer} 的具体实现类型
 * @author bingbaihanji
 */
public class AudioVisualizerViewNode<T extends AudioVisualizer> extends BorderPane {

    private static final AudioPlayerController ctrl = AudioPlayerController.getInstance();

    private final T visualizer;
    private final String tabId;

    /**
     * @param title      面板标题（保留供后续扩展使用）
     * @param visualizer 具体可视化器实例
     * @param tabId      对应的 Tab 标识符，供 TabPane 去重
     */
    public AudioVisualizerViewNode(String title, T visualizer, String tabId) {
        this.visualizer = visualizer;
        this.tabId = tabId;

        ctrl.addPCMVisualizer(visualizer);
        if (!visualizer.isRunning()) {
            visualizer.start();
        }

        setCenter(buildVisualizerContainer());
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
        if (!visualizer.isRunning()) {
            visualizer.start();
        }
    }

    /**
     * 释放资源：从控制器注销可视化器并停止渲染循环。
     * 在对应 Tab 关闭时调用。
     */
    public void dispose() {
        ctrl.removePCMVisualizer(visualizer);
        visualizer.stop();
    }

    public T getVisualizer() {
        return visualizer;
    }

    public String getTabId() {
        return tabId;
    }

    // ── 私有方法 ──────────────────────────────────────────────────

    /**
     * 构建自适应大小的可视化器容器，内边距 40px，可视化器居中铺满。
     */
    private StackPane buildVisualizerContainer() {
        var container = new StackPane(visualizer);
        container.setPadding(new Insets(40));
        StackPane.setAlignment(visualizer, Pos.CENTER);
        container.widthProperty().addListener((obs, o, w) ->
                visualizer.setPrefSize(w.doubleValue() - 80, container.getHeight() - 80));
        container.heightProperty().addListener((obs, o, h) ->
                visualizer.setPrefSize(container.getWidth() - 80, h.doubleValue() - 80));
        return container;
    }
}
