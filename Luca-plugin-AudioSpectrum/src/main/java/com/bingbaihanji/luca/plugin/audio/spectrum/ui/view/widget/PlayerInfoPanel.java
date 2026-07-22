package com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget;

import com.bingbaihanji.luca.plugin.audio.spectrum.controller.AudioPlayerController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;

/**
 * 播放器底部信息面板，所有可视化视图节点共用此组件。
 *
 * <p>包含：文件名标签、时间行（当前时间 / 总时长）、
 * 带波形叠加的进度滑块，以及播放/停止按钮区。
 * 调用方可通过 {@code extraButtons} 参数在按钮区末尾追加额外按钮（如"峰值连线"）。
 *
 * <p>布局结构：
 * <pre>
 * VBox
 * ├── Label        — 文件名
 * ├── HBox         — 当前时间 ｜ 弹簧 ｜ 总时长
 * ├── StackPane    — WaveformWidget（底层）+ 进度滑块（顶层，仅 track/thumb 区域响应事件）
 * └── HBox         — 播放 | 停止 | [extraButtons...]
 * </pre>
 *
 * @author bingbaihanji
 */
public class PlayerInfoPanel extends VBox {

    // Slider 子控件样式（需 applyCss + lookup 后才能精确设置，setStyle 无法覆盖子结构）
    private static final String TRACK_STYLE =
            "-fx-background-color: rgba(255,255,255,0.15);" +
                    "-fx-background-radius: 1px;" +
                    "-fx-pref-height: 2px;" +
                    "-fx-max-height: 2px;";

    private static final String THUMB_STYLE =
            "-fx-background-color: #00ffff;" +
                    "-fx-background-radius: 4px;" +
                    "-fx-pref-width: 8px;" +
                    "-fx-pref-height: 8px;";

    /**
     * 创建播放器底部信息面板。
     *
     * @param ctrl         共享的播放器控制器，用于属性绑定
     * @param extraButtons 追加到控制按钮区的额外按钮（可为空）
     */
    public PlayerInfoPanel(AudioPlayerController ctrl, Button... extraButtons) {
        super(2);

        // ── 文件名 ─────────────────────────────────────────────────
        var fileNameLabel = new Label();
        fileNameLabel.textProperty().bind(ctrl.currentFileName);
        fileNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        // ── 时间行 ─────────────────────────────────────────────────
        var currentTimeLabel = new Label();
        currentTimeLabel.textProperty().bind(ctrl.currentTime);
        currentTimeLabel.setStyle("-fx-text-fill: #00ffff; -fx-font-size: 12px;");

        var totalTimeLabel = new Label();
        totalTimeLabel.textProperty().bind(ctrl.totalTime);
        totalTimeLabel.setStyle("-fx-text-fill: #6496ff; -fx-font-size: 12px;");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var timeBox = new HBox(10, currentTimeLabel, spacer, totalTimeLabel);
        timeBox.setPadding(new Insets(2, 4, 2, 4));
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setMouseTransparent(true);

        // ── 进度滑块（WaveformWidget 在下层，滑块在上层）────────────
        var progressSlider = createProgressSlider(ctrl);
        var sliderBox = new HBox(progressSlider);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        sliderBox.setMaxWidth(Double.MAX_VALUE);
        sliderBox.setAlignment(Pos.CENTER);
        // HBox 自身不响应鼠标，让事件穿透到下层 WaveVisualization
        sliderBox.setPickOnBounds(false);

        var waveformPane = new WaveformWidget();
        waveformPane.setMaxWidth(Double.MAX_VALUE);
        waveformPane.setMaxHeight(Double.MAX_VALUE);
        var progressStack = new StackPane(waveformPane, sliderBox);
        progressStack.setStyle("-fx-background-color: transparent;");

        // ── 控制按钮 ────────────────────────────────────────────────
        var controlBox = new HBox(10, createPlayPauseButton(ctrl), createStopButton(ctrl));
        if (extraButtons != null) {
            for (Button btn : extraButtons) {
                controlBox.getChildren().add(btn);
            }
        }
        controlBox.setPadding(new Insets(10));
        controlBox.setAlignment(Pos.CENTER);

        // ── 组装 ────────────────────────────────────────────────────
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #0f1419;");
        getChildren().addAll(fileNameLabel, timeBox, progressStack, controlBox);
    }

    // ── 静态工厂（供外部单独创建按钮）────────────────────────────────

    /**
     * 创建播放/暂停切换按钮，自动响应 {@code ctrl.isPlaying} 变化切换文字。
     */
    public static Button createPlayPauseButton(AudioPlayerController ctrl) {
        String style = "-fx-background-color:#00ccff;-fx-text-fill:white;" +
                "-fx-font-size:12px;-fx-font-weight:bold;" +
                "-fx-padding:8px 16px;-fx-background-radius:5px;";
        Button btn = new Button("播放");
        btn.setStyle(style);
        ctrl.isPlaying.addListener((obs, old, playing) ->
                Platform.runLater(() -> btn.setText(Boolean.TRUE.equals(playing) ? "暂停" : "播放")));
        btn.setOnAction(e -> ctrl.togglePlayPause());
        return btn;
    }

    /**
     * 创建停止按钮，带悬浮高亮效果。
     */
    public static Button createStopButton(AudioPlayerController ctrl) {
        String base = "-fx-background-color:#ff6666;-fx-text-fill:white;" +
                "-fx-font-size:12px;-fx-padding:8px 16px;-fx-background-radius:5px;";
        String hover = "-fx-background-color:#ff8888;-fx-text-fill:white;" +
                "-fx-font-size:12px;-fx-padding:8px 16px;-fx-background-radius:5px;";
        Button btn = new Button("停止");
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(e -> ctrl.stop());
        return btn;
    }

    // ── 私有工厂 ──────────────────────────────────────────────────

    /**
     * 创建绑定到播放进度的细线型进度滑块。
     *
     * <p>Slider 的子节点（.track / .thumb）无法通过 {@code setStyle} 直接设样式，
     * 需等节点进入场景树后通过 {@code applyCss()} + {@code lookup()} 精确设置。
     */
    private static Slider createProgressSlider(AudioPlayerController ctrl) {
        Slider slider = new Slider(0, 1, 0);
        // 仅 track/thumb 区域响应鼠标，空白区域事件穿透到下层 WaveVisualization
        slider.setPickOnBounds(false);
        ctrl.progress.addListener((obs, o, n) -> {
            if (!slider.isValueChanging()) {
                slider.setValue(n.doubleValue());
            }
        });
        // 用户拖拽或点击结束时触发 seek
        slider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                ctrl.seek(slider.getValue());
            }
        });
        HBox.setHgrow(slider, Priority.ALWAYS);
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        slider.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                slider.applyCss();
                slider.layout();
                var track = slider.lookup(".track");
                if (track != null) {
                    track.setStyle(TRACK_STYLE);
                }
                var thumb = slider.lookup(".thumb");
                if (thumb != null) {
                    thumb.setStyle(THUMB_STYLE);
                }
            }
        });
        return slider;
    }
}
