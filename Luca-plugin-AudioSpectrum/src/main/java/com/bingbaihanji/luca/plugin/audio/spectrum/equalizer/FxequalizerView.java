package com.bingbaihanji.luca.plugin.audio.spectrum.equalizer;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Data;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 均衡器视图（独立窗口）
 */

/// 使用方法:
///      ```java
///         // 当播放器准备就绪时，绑定均衡器
///         player.setOnReady(() -> {
///             bindEqualizer(player);
///         });
///       ```
///
///  ```java
///      /**
///       * 将均衡器视图滑块与媒体播放器音频均衡器进行绑定
///       *
///       * @param player 媒体播放器实例，用于获取其内置的音频均衡器
///       */
///      private void bindEqualizer(javafx.scene.media.MediaPlayer player) {
///          FxequalizerView eqView = FxequalizerView.getInstance();
///          AudioEqualizer equalizer = player.getAudioEqualizer(); // 获取媒体播放器的音频均衡器
///          equalizer.setEnabled(true);
///          List<EqualizerBand> bands = equalizer.getBands();
///          List<Slider> sliders = eqView.getSliders();
///          // 为每个滑块添加值变化监听器，实现滑块与均衡器频带增益的实时同步
///          for (int i = 0; i < sliders.size(); i++) {
///              EqualizerBand band = bands.get(i);
///              sliders.get(i).valueProperty().addListener((observable, oldValue, newValue) -> {
///                  band.setGain(newValue.doubleValue());
///              });
///          }
///      }
///  ```
///


@Data
public class FxequalizerView {

    // 单例

    /**
     * 10段均衡器频率
     */
    private static final String[] BANDS = {
            "31", "62", "125", "250", "500",
            "1K", "2K", "4K", "8K", "16K"
    };
    private static final int BAND_COUNT = 10;

    // 常量
    // 滑块轨道几何常量（基于 Modena 主题，thumb -fx-padding: 7px）
    private static final double SLIDER_H = 200.0;
    private static final double THUMB_R = 7.0;
    private static final double TRACK_TOP = THUMB_R + 1.5;
    private static final double TRACK_BOT = SLIDER_H - THUMB_R - 1.5;
    private static final double TRACK_H = TRACK_BOT - TRACK_TOP;
    private static final double TRACK_CY = (TRACK_TOP + TRACK_BOT) / 2.0;
    private static final double BAND_W = 44.0;
    private static final double TRACK_W = 4.0;
    private static FxequalizerView instance;
    private final Stage stage = new Stage();

    // UI
    private final List<Slider> sliders = new ArrayList<>();
    private final HBox bandsBox = new HBox(8);
    private final ComboBox<String> presetBox = new ComboBox<>();
    private int currentPresetIndex;

    // 数据
    private boolean applyingPreset = false;
    private ArrayList<String> presetsNames;
    private ArrayList<int[]> presetsValues;

    public FxequalizerView() {

        initialize();

        VBox root = new VBox(8);
        HeaderBar headerBar = new HeaderBar();
        headerBar.setPadding(new Insets(10));


        root.setPadding(new Insets(16, 24, 16, 24));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #32404a;");

        createPresetBox();
        createBands();   // Y轴作为第一列已在 createBands() 中添加

        HBox topBox = new HBox();
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.getChildren().add(presetBox);

        root.getChildren().addAll(headerBar, topBox, bandsBox);
        VBox.setVgrow(bandsBox, Priority.ALWAYS);

        applyPreset(0);

        Scene scene = new Scene(root, 640, 440);
        loadSliderCss(scene);
        stage.setScene(scene);
        stage.setTitle("均衡器");
        stage.setResizable(false);
    }

    // 初始化

    // 单例
    public static FxequalizerView getInstance() {
        if (instance == null) {
            instance = new FxequalizerView();
        }
        return instance;
    }

    // 构造

    private void initialize() {
        stage.initStyle(StageStyle.EXTENDED);

        presetsNames = new ArrayList<>(Arrays.asList(
                "平滑", "原声", "电子", "流行", "摇滚", "重低音", "自定义"
        ));

        presetsValues = new ArrayList<>(Arrays.asList(
                new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                new int[]{5, 5, 4, 1, 2, 2, 3, 4, 3, 2},
                new int[]{4, 4, 1, 0, -2, 2, 1, 1, 4, 5},
                new int[]{-2, -1, 0, 2, 4, 4, 2, 0, -1, -2},
                new int[]{5, 4, 3, 1, 0, -1, 1, 3, 4, 5},
                new int[]{6, 5, 4, 3, 1, 0, 0, 0, 0, 0},
                new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
        ));
    }

    // 窗口控制

    public void show() {
        stage.show();
        stage.toFront();
    }

    public void hide() {
        stage.hide();
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    // ── Y 轴 dB 标签 

    /**
     * 构建 Y 轴列：与 band VBox 结构完全相同（sliderProxy + dummy 标签），
     * 作为 bandsBox 的第一列，保证垂直位置与滑块轨道严格一致。
     */
    private VBox buildYAxis() {

        // sliderProxy：与 Canvas 等高，用绝对坐标放置刻度标签
        Pane sliderProxy = new Pane();
        sliderProxy.setPrefHeight(SLIDER_H);
        sliderProxy.setMinHeight(SLIDER_H);
        sliderProxy.setMaxHeight(SLIDER_H);
        sliderProxy.setPrefWidth(36);

        double labelH = 12.0;
        String stylePri = "-fx-text-fill: #8aa0b0; -fx-font-size: 10px;";
        String styleSec = "-fx-text-fill: #5d7280; -fx-font-size: 10px;";

        int[] dbTicks = {12, 6, 0, -6, -12};
        for (int db : dbTicks) {
            String text = db > 0 ? ("+" + db) : String.valueOf(db);
            Label label = new Label(text);
            label.setStyle(db % 12 == 0 ? stylePri : styleSec);
            // Y 坐标与 Canvas 内的 valueToY 完全一致
            label.setLayoutY(valueToY(db) - labelH / 2);
            sliderProxy.getChildren().add(label);
        }

        // dummy：占位，与频率标签行等高，让整列高度与 band VBox 一致
        Label dummy = new Label(" ");
        dummy.setStyle("-fx-font-size: 11px;");

        VBox yAxis = new VBox(6, sliderProxy, dummy);
        yAxis.setAlignment(Pos.CENTER_RIGHT);
        return yAxis;
    }

    // ── CSS：透明轨道 + 自定义 thumb 

    private void loadSliderCss(Scene scene) {

        String css =
                ".eq-slider .track {" +
                        "  -fx-background-color: transparent;" +
                        "  -fx-border-color: transparent;" +
                        "}" +
                        ".eq-slider .thumb {" +
                        "  -fx-background-color: #ffffff;" +
                        "  -fx-background-radius: 1em;" +
                        "  -fx-padding: " + (int) THUMB_R + "px;" +
                        "  -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 3, 0, 0, 1);" +
                        "}";

        try {
            File tmp = File.createTempFile("eq-style-", ".css");
            tmp.deleteOnExit();
            try (FileWriter fw = new FileWriter(tmp)) {
                fw.write(css);
            }
            scene.getStylesheets().add(tmp.toURI().toString());
        } catch (Exception ignored) {
        }
    }

    // 预设

    private void createPresetBox() {

        presetBox.getItems().addAll(presetsNames);
        presetBox.setPrefWidth(100);
        presetBox.setPrefHeight(28);
        presetBox.setMaxWidth(100);
        presetBox.setMaxHeight(28);
        presetBox.setStyle(
                "-fx-background-color: #2b7a8e;" +
                        "-fx-border-color: #5bbdcf;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;"
        );

        presetBox.getSelectionModel().selectedIndexProperty()
                .addListener((obs, oldV, newV) -> {

                    if (newV == null) {
                        return;
                    }

                    currentPresetIndex = newV.intValue();
                    applyPreset(currentPresetIndex);
                });

        presetBox.getSelectionModel().select(0);
    }

    // 频段

    private void createBands() {

        bandsBox.setAlignment(Pos.TOP_CENTER);
        bandsBox.setPadding(new Insets(8, 0, 8, 0));

        // Y 轴作为第一列，与 band VBox 同处一个父容器，保证垂直对齐
        bandsBox.getChildren().add(buildYAxis());

        for (int i = 0; i < BAND_COUNT; i++) {

            Slider slider = buildSlider();
            Canvas canvas = buildCanvas(slider);
            sliders.add(slider);

            Label freqLabel = new Label(BANDS[i]);
            freqLabel.setStyle("-fx-text-fill: #d0eef5; -fx-font-size: 11px;");

            StackPane sliderPane = new StackPane(canvas, slider);
            sliderPane.setPrefWidth(BAND_W);
            sliderPane.setMaxWidth(BAND_W);
            sliderPane.setAlignment(Pos.CENTER);

            VBox band = new VBox(6, sliderPane, freqLabel);
            band.setAlignment(Pos.CENTER);
            bandsBox.getChildren().add(band);
        }
    }

    private Slider buildSlider() {

        Slider slider = new Slider(-12, 12, 0);
        slider.setOrientation(Orientation.VERTICAL);
        slider.setPrefHeight(SLIDER_H);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.getStyleClass().add("eq-slider");

        slider.valueProperty().addListener((obs, o, n) -> {

            if (applyingPreset) {
                return;
            }

            if (currentPresetIndex != presetsNames.size() - 1) {
                currentPresetIndex = presetsNames.size() - 1;
                presetBox.getSelectionModel().select(currentPresetIndex);
            }
        });

        return slider;
    }

    public List<Slider> getSliders() {
        return sliders;
    }

    private Canvas buildCanvas(Slider slider) {

        Canvas canvas = new Canvas(BAND_W, SLIDER_H);

        Runnable redraw = () -> paintTrack(canvas, slider.getValue());
        redraw.run();
        slider.valueProperty().addListener((obs, o, n) -> redraw.run());

        return canvas;
    }

    // ── Canvas 绘制

    private void paintTrack(Canvas canvas, double value) {

        double cw = canvas.getWidth();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, cw, SLIDER_H);

        double cx = cw / 2.0;
        double tx = cx - TRACK_W / 2.0;  // 轨道左边 X

        // 背景灰色轨道
        gc.setFill(Color.web("#4A5A68"));
        gc.fillRoundRect(tx, TRACK_TOP, TRACK_W, TRACK_H, TRACK_W, TRACK_W);

        // 从 0dB 中心到当前值的颜色填充
        if (Math.abs(value) > 0.05) {
            double vy = valueToY(value);
            gc.setFill(Color.web("#5BBDCF"));
            if (value > 0) {
                // 正值：从 vy 填充到中心线（向上）
                gc.fillRect(tx, vy, TRACK_W, TRACK_CY - vy);
            } else {
                // 负值：从中心线填充到 vy（向下）
                gc.fillRect(tx, TRACK_CY, TRACK_W, vy - TRACK_CY);
            }
        }

        // 刻度线：-12, -6, 0, 6, 12 dB
        gc.setLineWidth(1.0);
        for (int db = -12; db <= 12; db += 6) {
            double ty = valueToY(db);
            double tlen = (db == 0) ? 7 : 4;
            gc.setStroke(db == 0 ? Color.web("#6A7D8E") : Color.web("#4A5A68"));
            gc.strokeLine(tx - tlen - 1, ty, tx - 1, ty);
            gc.strokeLine(tx + TRACK_W + 1, ty, tx + TRACK_W + tlen + 1, ty);
        }
    }

    /**
     * 将 dB 值（−12…12）转换为 Canvas 的 Y 坐标。
     * 12dB → TRACK_TOP；−12dB → TRACK_BOT
     */
    private double valueToY(double value) {
        double ratio = (value + 12.0) / 24.0;   // 0.0（底）… 1.0（顶）
        return TRACK_BOT - ratio * TRACK_H;
    }

    // 应用预设

    public void applyPreset(int index) {

        applyingPreset = true;

        int[] values = presetsValues.get(index);

        for (int i = 0; i < sliders.size(); i++) {
            sliders.get(i).setValue(values[i]);
        }

        applyingPreset = false;
    }

    // 对外 API

    /**
     * 获取各频段当前增益值（dB）
     */
    public double[] getBandValues() {

        double[] result = new double[sliders.size()];

        for (int i = 0; i < sliders.size(); i++) {
            result[i] = sliders.get(i).getValue();
        }

        return result;
    }

    /**
     * 获取当前选中的预设名称
     */
    public String getCurrentPresetName() {
        return presetsNames.get(currentPresetIndex);
    }

    /**
     * 获取当前均衡器完整状态（预设索引、预设名、各频段值、频段标签）
     */
    public EqState getEqState() {
        return new EqState(
                currentPresetIndex,
                getCurrentPresetName(),
                getBandValues(),
                Arrays.copyOf(BANDS, BANDS.length)
        );
    }

    /**
     * 当前均衡器完整状态快照
     */
    public static class EqState {

        /**
         * 当前预设索引
         */
        public final int presetIndex;

        /**
         * 当前预设名称
         */
        public final String presetName;

        /**
         * 各频段增益值（dB），共10段，范围 -12 ~ 12
         */
        public final double[] bandValues;

        /**
         * 各频段频率标签，与 bandValues 下标对应
         */
        public final String[] bandNames;

        EqState(int presetIndex, String presetName, double[] bandValues, String[] bandNames) {
            this.presetIndex = presetIndex;
            this.presetName = presetName;
            this.bandValues = bandValues;
            this.bandNames = bandNames;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("EqState{preset=").append(presetIndex)
                    .append("(").append(presetName).append("), bands=[");
            for (int i = 0; i < bandNames.length; i++) {
                sb.append(bandNames[i]).append(":").append(String.format("%.1f", bandValues[i]));
                if (i < bandNames.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]}");
            return sb.toString();
        }
    }
}
