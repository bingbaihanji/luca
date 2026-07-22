package com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.node;

import com.bingbaihanji.luca.plugin.audio.spectrum.controller.AudioPlayerController;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.spectrum.RadialSpectrumView;
import com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.PlayerInfoPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.nio.file.Path;

/**
 * 圆形辐射频谱可视化面板。
 *
 * <p>圆心实时显示剩余时长，外圈进度环与播放进度联动。
 * 通过 {@link AudioPlayerController} 单例与其他可视化视图共享播放状态，
 * 底部控制区由 {@link PlayerInfoPanel} 统一提供，额外提供"峰值连线"切换按钮。
 *
 * @author bingbaihanji
 */
public class RadialSpectrumViewNode extends BorderPane {

    private static final AudioPlayerController ctrl = AudioPlayerController.getInstance();

    private final RadialSpectrumView spectrumView;

    public RadialSpectrumViewNode() {
        spectrumView = new RadialSpectrumView(400.0, 400.0);
        ctrl.addVisualizer(spectrumView);

        // 播放进度 → 圆环外圈
        ctrl.progress.addListener((obs, o, n) -> spectrumView.setProgress(n));

        // 实时计算剩余时长并更新圆心文字
        Runnable syncRemaining = () -> {
            int cur = parseSeconds(ctrl.currentTime.get());
            int total = parseSeconds(ctrl.totalTime.get());
            spectrumView.setTimeText(formatSeconds(total - cur));
        };
        ctrl.currentTime.addListener((obs, o, n) -> syncRemaining.run());
        ctrl.totalTime.addListener((obs, o, n) -> syncRemaining.run());

        setCenter(buildSpectrumContainer());
        setBottom(new PlayerInfoPanel(ctrl, createPeakLineButton()));
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
     * 构建自适应大小的频谱容器，内边距 40px，频谱组件居中铺满。
     */
    private StackPane buildSpectrumContainer() {
        var container = new StackPane(spectrumView);
        container.setPadding(new Insets(40));
        StackPane.setAlignment(spectrumView, Pos.CENTER);
        container.widthProperty().addListener((obs, o, w) ->
                spectrumView.resize(w.doubleValue() - 80, container.getHeight() - 80));
        container.heightProperty().addListener((obs, o, h) ->
                spectrumView.resize(container.getWidth() - 80, h.doubleValue() - 80));
        return container;
    }

    /**
     * 峰值连线切换按钮（仅圆形频谱视图特有）。
     */
    private Button createPeakLineButton() {
        String off = "-fx-background-color:#4a5a7a;-fx-text-fill:white;" +
                "-fx-font-size:12px;-fx-padding:8px 16px;-fx-background-radius:5px;";
        String on = "-fx-background-color:#00aa88;-fx-text-fill:white;" +
                "-fx-font-size:12px;-fx-padding:8px 16px;-fx-background-radius:5px;";
        Button btn = new Button("峰值连线: 关");
        btn.setStyle(off);
        btn.setOnAction(e -> {
            boolean next = !spectrumView.isEnablePeakLine();
            spectrumView.setEnablePeakLine(next);
            btn.setText(next ? "峰值连线: 开" : "峰值连线: 关");
            btn.setStyle(next ? on : off);
        });
        return btn;
    }

    // ── 时间工具 ──────────────────────────────────────────────────

    /**
     * 将 "m:ss" 或 "h:mm:ss" 字符串解析为秒数，解析失败返回 0。
     */
    private int parseSeconds(String timeStr) {
        try {
            String[] p = timeStr.split(":");
            return switch (p.length) {
                case 2 -> toInt(p[0]) * 60 + toInt(p[1]);
                case 3 -> toInt(p[0]) * 3600 + toInt(p[1]) * 60 + toInt(p[2]);
                default -> 0;
            };
        } catch (Exception e) {
            return 0;
        }
    }

    private int toInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 将秒数格式化为 "m:ss"，负数按 0 处理。
     */
    private String formatSeconds(int total) {
        int s = Math.max(total, 0);
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
