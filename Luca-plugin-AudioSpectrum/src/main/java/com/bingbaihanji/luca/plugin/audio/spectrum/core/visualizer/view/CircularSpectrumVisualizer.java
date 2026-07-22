package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.view;


import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioData;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioVisualizer;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal.SpectrumAnalyzer;


import javafx.beans.property.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 圆形频谱可视化器（带线条的圆形）
 *
 * <p>将音频频谱显示为从中心圆向外辐射的径向线条
 *
 * @author bingbaihanji
 */
public class CircularSpectrumVisualizer extends AudioVisualizer {

    //  处理器配置

    private final IntegerProperty barCount = new SimpleIntegerProperty(360);
    private final IntegerProperty fftSize = new SimpleIntegerProperty(512);
    private final DoubleProperty decay = new SimpleDoubleProperty(0.05);

    //  配置属性
    private final DoubleProperty innerRadius = new SimpleDoubleProperty(0.3);
    private final BooleanProperty rainbowMode = new SimpleBooleanProperty(true);
    private final ObjectProperty<Color> lineColor = new SimpleObjectProperty<>(Color.CYAN);
    private final BooleanProperty bidirectional = new SimpleBooleanProperty(true);
    private SpectrumAnalyzer analyzer;

    /**
     * 创建一个新的圆形频谱可视化器
     */
    public CircularSpectrumVisualizer() {
        super();
        initializeAnalyzer();

        // 当配置变化时重新初始化分析器
        barCount.addListener((obs, old, val) -> initializeAnalyzer());
        fftSize.addListener((obs, old, val) -> initializeAnalyzer());
        decay.addListener((obs, old, val) -> initializeAnalyzer());
    }

    /**
     * 初始化频谱分析器
     */
    private void initializeAnalyzer() {
        analyzer = new SpectrumAnalyzer(fftSize.get(), barCount.get(), (float) decay.get());
    }

    @Override
    protected void render(GraphicsContext gc, AudioData data) {
        double width = getCanvasWidth();
        double height = getCanvasHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double maxRadius = Math.min(width, height) / 2.0 * 0.8;
        double minRadius = maxRadius * innerRadius.get();

        // 分析频谱
        float[] bands = analyzer.analyze(data.getStereoMerge());

        double angleStep = 360.0 / bands.length;
        gc.setLineWidth(2.0);

        for (int i = 0; i < bands.length; i++) {
            double angle = Math.toRadians(i * angleStep);
            float intensity = bands[i];

            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            // 起始点（内圆）
            double startX = centerX + cos * minRadius;
            double startY = centerY + sin * minRadius;

            // 向外延伸长度
            double length = intensity * (maxRadius - minRadius) * 3;
            double endX = centerX + cos * (minRadius + length);
            double endY = centerY + sin * (minRadius + length);

            // 颜色
            Color color = rainbowMode.get() ?
                    Color.hsb((double) i / bands.length * 360.0, 1.0, 1.0) :
                    lineColor.get();

            gc.setStroke(color);

            // 绘制外线
            gc.strokeLine(startX, startY, endX, endY);

            // 绘制内线（双向模式）
            if (bidirectional.get()) {
                double innerLength = length * 0.5;
                double innerEndX = centerX + cos * (minRadius - innerLength);
                double innerEndY = centerY + sin * (minRadius - innerLength);
                gc.strokeLine(startX, startY, innerEndX, innerEndY);
            }
        }
    }

    //  属性访问器

    /**
     * 条柱数量属性
     *
     * @return 条柱数量属性
     */
    public IntegerProperty barCountProperty() {
        return barCount;
    }

    /**
     * 获取条柱数量
     *
     * @return 条柱数量
     */
    public int getBarCount() {
        return barCount.get();
    }

    /**
     * 设置条柱数量
     *
     * @param count 条柱数量（必须为正数）
     */
    public void setBarCount(int count) {
        if (count > 0) {
            barCount.set(count);
        }
    }

    /**
     * FFT 尺寸属性
     *
     * @return FFT 尺寸属性
     */
    public IntegerProperty fftSizeProperty() {
        return fftSize;
    }

    /**
     * 获取 FFT 尺寸
     *
     * @return FFT 尺寸
     */
    public int getFFTSize() {
        return fftSize.get();
    }

    /**
     * 设置 FFT 尺寸
     *
     * @param size FFT 尺寸（必须为 2 的幂）
     */
    public void setFFTSize(int size) {
        if (size > 0 && (size & (size - 1)) == 0) {
            fftSize.set(size);
        }
    }

    /**
     * 衰减率属性
     *
     * @return 衰减率属性
     */
    public DoubleProperty decayProperty() {
        return decay;
    }

    /**
     * 获取衰减率
     *
     * @return 衰减率
     */
    public double getDecay() {
        return decay.get();
    }

    /**
     * 设置衰减率
     *
     * @param rate 衰减率（0.0 到 1.0）
     */
    public void setDecay(double rate) {
        decay.set(Math.max(0.0, Math.min(1.0, rate)));
    }

    /**
     * 内圆半径属性
     *
     * @return 内圆半径属性
     */
    public DoubleProperty innerRadiusProperty() {
        return innerRadius;
    }

    /**
     * 获取内圆半径
     *
     * @return 内圆半径（0.0 到 1.0）
     */
    public double getInnerRadius() {
        return innerRadius.get();
    }

    /**
     * 设置内圆半径
     *
     * @param radius 内圆半径（0.0 到 1.0）
     */
    public void setInnerRadius(double radius) {
        innerRadius.set(Math.max(0.0, Math.min(1.0, radius)));
    }

    /**
     * 彩虹模式属性
     *
     * @return 彩虹模式属性
     */
    public BooleanProperty rainbowModeProperty() {
        return rainbowMode;
    }

    /**
     * 获取是否使用彩虹模式
     *
     * @return 如果使用彩虹模式则返回 true
     */
    public boolean isRainbowMode() {
        return rainbowMode.get();
    }

    /**
     * 设置是否使用彩虹模式
     *
     * @param rainbow 是否使用彩虹模式
     */
    public void setRainbowMode(boolean rainbow) {
        rainbowMode.set(rainbow);
    }

    /**
     * 线条颜色属性
     *
     * @return 线条颜色属性
     */
    public ObjectProperty<Color> lineColorProperty() {
        return lineColor;
    }

    /**
     * 获取线条颜色
     *
     * @return 线条颜色
     */
    public Color getLineColor() {
        return lineColor.get();
    }

    /**
     * 设置线条颜色
     *
     * @param color 线条颜色
     */
    public void setLineColor(Color color) {
        if (color != null) {
            lineColor.set(color);
        }
    }

    /**
     * 双向模式属性
     *
     * @return 双向模式属性
     */
    public BooleanProperty bidirectionalProperty() {
        return bidirectional;
    }

    /**
     * 获取是否使用双向模式
     *
     * @return 如果使用双向模式则返回 true
     */
    public boolean isBidirectional() {
        return bidirectional.get();
    }

    /**
     * 设置是否使用双向模式
     *
     * @param bidir 是否使用双向模式
     */
    public void setBidirectional(boolean bidir) {
        bidirectional.set(bidir);
    }

}