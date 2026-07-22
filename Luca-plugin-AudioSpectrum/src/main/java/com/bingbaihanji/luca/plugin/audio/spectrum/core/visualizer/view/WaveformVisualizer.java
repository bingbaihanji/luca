package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.view;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioData;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioVisualizer;
import javafx.beans.property.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 波形可视化器（示波器）
 * <p>
 * 实时显示音频波形,支持单声道和立体声模式。
 * 立体声模式下,左声道波形中心位于组件高度的 1/3 处,右声道中心位于 2/3 处,
 * 各自占据 1/3 高度,两条波形在中间相接。
 * 默认情况下,左右声道颜色自动互为互补色（可通过 {@link #complementaryColorsProperty()} 关闭）。
 * </p>
 *
 * @author bingbaihanji
 */
public class WaveformVisualizer extends AudioVisualizer {

    // 配置属性
    private final BooleanProperty stereoMode = new SimpleBooleanProperty(true); // 是否启用立体声模式
    private final ObjectProperty<Color> waveColor = new SimpleObjectProperty<>(Color.CYAN); // 波形颜色
    private final ObjectProperty<Color> stereoWaveColor = new SimpleObjectProperty<>(Color.MAGENTA); // 立体声模式下左右声道颜色
    private final DoubleProperty lineWidth = new SimpleDoubleProperty(1.5); // 线条宽度
    private final BooleanProperty rainbowMode = new SimpleBooleanProperty(false); // 是否启用彩虹模式
    private final BooleanProperty complementaryColors = new SimpleBooleanProperty(true);

    // 彩虹色状态（每帧变化）
    private int rainbowHue = 0;

    /**
     * 创建一个新的波形可视化器
     */
    public WaveformVisualizer() {
        super();
    }

    @Override
    protected void render(GraphicsContext gc, AudioData data) {
        double width = getCanvasWidth();
        double height = getCanvasHeight();

        if (width <= 0 || height <= 0 || data == null) {
            return;
        }

        gc.setLineWidth(lineWidth.get());

        // 获取当前帧的彩虹色（彩虹模式下每帧变化一次）
        Color currentRainbowColor = rainbowMode.get() ? getNextRainbowColor() : null;

        if (stereoMode.get()) {
            // 立体声模式：左声道（上1/3）、右声道（下1/3）
            double leftYOffset = height / 6.0;
            double leftYRange = height / 3.0;
            Color leftColor = determineLeftColor(currentRainbowColor);
            drawChannel(gc, data.getLeftChannel(), leftYOffset, leftYRange, width, leftColor);

            double rightYOffset = height / 2.0;
            double rightYRange = height / 3.0;
            Color rightColor = determineRightColor(leftColor, currentRainbowColor);
            drawChannel(gc, data.getRightChannel(), rightYOffset, rightYRange, width, rightColor);
        } else {
            // 单声道模式：合并后的立体声数据占据整个高度
            Color color = (rainbowMode.get() && currentRainbowColor != null) ? currentRainbowColor : waveColor.get();
            drawChannel(gc, data.getStereoMerge(), 0, height, width, color);
        }
    }

    /**
     * 确定左声道颜色（立体声模式）
     *
     * @param rainbowColor 当前彩虹色（如果彩虹模式开启,否则为 null）
     * @return 左声道颜色
     */
    private Color determineLeftColor(Color rainbowColor) {
        if (rainbowColor != null) {
            return rainbowColor;
        }
        return waveColor.get();
    }

    /**
     * 确定右声道颜色（立体声模式）
     *
     * @param leftColor    左声道颜色（已确定）
     * @param rainbowColor 当前彩虹色（如果彩虹模式开启,否则为 null）
     * @return 右声道颜色
     */
    private Color determineRightColor(Color leftColor, Color rainbowColor) {
        if (complementaryColors.get()) {
            // 互补模式：右声道为左声道的互补色
            return getComplementaryColor(leftColor);
        } else {
            // 非互补模式：使用原有逻辑
            if (rainbowColor != null) {
                return rainbowColor;
            }
            return stereoWaveColor.get();
        }
    }

    /**
     * 绘制单个音频声道
     *
     * @param gc      图形上下文
     * @param samples 音频样本（范围 [-1, 1]）
     * @param yOffset 垂直偏移（起始 y 坐标）
     * @param yRange  垂直范围（波形最大振幅对应的像素范围）
     * @param width   画布宽度
     * @param color   线条颜色
     */
    private void drawChannel(GraphicsContext gc, float[] samples,
                             double yOffset, double yRange, double width, Color color) {
        if (samples == null || samples.length == 0) {
            return;
        }

        int sampleCount = Math.min(samples.length, (int) width);
        if (sampleCount <= 1) {
            return;
        }

        // 预计算垂直中心线和缩放因子
        double halfRange = yRange / 2.0;
        double centerY = yOffset + halfRange;

        // 水平步长（每个样本对应的 x 增量）
        double xStep = width / sampleCount;

        gc.setStroke(color);

        // 绘制第一个点（线段起点）
        double prevX = 0;
        double prevY = centerY + samples[0] * halfRange;
        prevY = clamp(prevY, yOffset, yOffset + yRange);

        for (int i = 1; i < sampleCount; i++) {
            double x = i * xStep;
            double y = centerY + samples[i] * halfRange;
            y = clamp(y, yOffset, yOffset + yRange);

            gc.strokeLine(prevX, prevY, x, y);

            prevX = x;
            prevY = y;
        }
    }

    /**
     * 限制值在 [min, max] 范围内
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 获取下一个彩虹色（色相递增,每帧调用一次）
     *
     * @return 新的彩虹色
     */
    private Color getNextRainbowColor() {
        rainbowHue = (rainbowHue + 1) % 360;
        return Color.hsb(rainbowHue, 1.0, 1.0);
    }

    /**
     * 计算给定颜色的互补色（色相旋转 180°）
     *
     * @param color 原始颜色
     * @return 互补色
     */
    private Color getComplementaryColor(Color color) {
        double hue = color.getHue();
        double saturation = color.getSaturation();
        double brightness = color.getBrightness();
        double complementaryHue = (hue + 180) % 360;
        return Color.hsb(complementaryHue, saturation, brightness);
    }

    // 属性访问器

    public BooleanProperty stereoModeProperty() {
        return stereoMode;
    }

    public boolean isStereoMode() {
        return stereoMode.get();
    }

    public void setStereoMode(boolean stereo) {
        stereoMode.set(stereo);
    }

    public ObjectProperty<Color> waveColorProperty() {
        return waveColor;
    }

    public Color getWaveColor() {
        return waveColor.get();
    }

    public void setWaveColor(Color color) {
        if (color != null) {
            waveColor.set(color);
        }
    }

    public ObjectProperty<Color> stereoWaveColorProperty() {
        return stereoWaveColor;
    }

    public Color getStereoWaveColor() {
        return stereoWaveColor.get();
    }

    public void setStereoWaveColor(Color color) {
        if (color != null) {
            stereoWaveColor.set(color);
        }
    }

    public DoubleProperty lineWidthProperty() {
        return lineWidth;
    }

    public double getLineWidth() {
        return lineWidth.get();
    }

    public void setLineWidth(double width) {
        if (width > 0) {
            lineWidth.set(width);
        }
    }

    public BooleanProperty rainbowModeProperty() {
        return rainbowMode;
    }

    public boolean isRainbowMode() {
        return rainbowMode.get();
    }

    public void setRainbowMode(boolean rainbow) {
        rainbowMode.set(rainbow);
        if (!rainbow) {
            // 非彩虹模式时重置色相索引,避免下次开启时颜色跳跃
            rainbowHue = 0;
        }
    }

    /**
     * 互补色模式属性（仅在立体声模式下生效）
     * <p>
     * 若为 true,右声道颜色将自动设置为左声道颜色的互补色；
     * 若为 false,则使用 {@link #stereoWaveColorProperty()} 指定的颜色（或彩虹模式下的同一彩虹色）。
     * </p>
     *
     * @return 互补色模式属性
     */
    public BooleanProperty complementaryColorsProperty() {
        return complementaryColors;
    }

    public boolean isComplementaryColors() {
        return complementaryColors.get();
    }

    public void setComplementaryColors(boolean complementary) {
        complementaryColors.set(complementary);
    }
}