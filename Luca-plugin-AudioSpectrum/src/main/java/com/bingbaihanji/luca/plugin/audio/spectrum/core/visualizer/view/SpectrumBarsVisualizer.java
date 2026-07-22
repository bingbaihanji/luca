package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.view;


import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioData;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioVisualizer;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.color.ColorGradient;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal.PeakDetector;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal.SpectrumAnalyzer;
import javafx.beans.property.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 频谱条可视化器
 *
 * <p>将音频频谱显示为垂直的彩色渐变条，并可选显示峰值指示器
 *
 * @author bingbaihanji
 */
public class SpectrumBarsVisualizer extends AudioVisualizer {

    // 处理器配置

    private final IntegerProperty bandCount = new SimpleIntegerProperty(50);
    private final IntegerProperty fftSize = new SimpleIntegerProperty(512);
    private final DoubleProperty decay = new SimpleDoubleProperty(0.05);

    // 配置属性
    private final BooleanProperty showPeaks = new SimpleBooleanProperty(true);
    private final IntegerProperty barSpacing = new SimpleIntegerProperty(2);
    private final ObjectProperty<Color> peakColor = new SimpleObjectProperty<>(Color.WHITE);
    private final IntegerProperty barOffset = new SimpleIntegerProperty(1);

    // PeakDetector 配置属性
    private final IntegerProperty peakHoldFrames = new SimpleIntegerProperty(20);
    private final DoubleProperty peakFallSpeed = new SimpleDoubleProperty(2.0);

    private SpectrumAnalyzer analyzer;
    private PeakDetector peakDetector;
    private ColorGradient colorGradient;

    /**
     * 创建一个新的频谱条可视化器
     */
    public SpectrumBarsVisualizer() {
        super();
        initializeProcessors();

        // 当配置发生变化时重新初始化
        bandCount.addListener((obs, old, val) -> reinitializeProcessors());
        fftSize.addListener((obs, old, val) -> reinitializeProcessors());
        decay.addListener((obs, old, val) -> analyzer = new SpectrumAnalyzer(
                fftSize.get(), bandCount.get(), (float) decay.get()));
        peakHoldFrames.addListener((obs, old, val) -> reinitializePeakDetector());
        peakFallSpeed.addListener((obs, old, val) -> reinitializePeakDetector());
    }

    /**
     * 初始化分析器、峰值检测器和颜色渐变
     */
    private void initializeProcessors() {
        analyzer = new SpectrumAnalyzer(fftSize.get(), bandCount.get(), (float) decay.get());
        peakDetector = new PeakDetector(bandCount.get(), peakHoldFrames.get(), (float) peakFallSpeed.get());
        colorGradient = ColorGradient.createFireGradient();
    }

    /**
     * 当频带数量或 FFT 尺寸变化时重新初始化处理器
     */
    private void reinitializeProcessors() {
        analyzer = new SpectrumAnalyzer(fftSize.get(), bandCount.get(), (float) decay.get());
        peakDetector = new PeakDetector(bandCount.get(), peakHoldFrames.get(), (float) peakFallSpeed.get());
    }

    /**
     * 当峰值检测器参数变化时重新初始化峰值检测器
     */
    private void reinitializePeakDetector() {
        peakDetector = new PeakDetector(bandCount.get(), peakHoldFrames.get(), (float) peakFallSpeed.get());
    }

    @Override
    protected void render(GraphicsContext gc, AudioData data) {
        double width = getCanvasWidth();
        double height = getCanvasHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        // 分析频谱
        float[] bands = analyzer.analyze(data.getStereoMerge());

        // 计算条柱尺寸
        int spacing = barSpacing.get();
        double totalSpacing = spacing * (bands.length - 1);
        double barWidth = (width - totalSpacing) / bands.length;

        // 颜色渐变的缩放因子
        float colorScale = (float) colorGradient.getColorCount() / (float) height * barOffset.get();

        // 绘制频谱条
        for (int i = 0; i < bands.length; i++) {
            double x = i * (barWidth + spacing);
            double barHeight = Math.log(1 + bands[i] * 10) * height;

            // 绘制渐变条
            drawGradientBar(gc, x, height - barHeight, barWidth, barHeight, colorScale);

            // 绘制峰值指示器
            if (showPeaks.get()) {
                float peakHeight = peakDetector.updateAndGetPeak(i, (float) barHeight);
                if (peakHeight > 0) {
                    gc.setStroke(peakColor.get());
                    gc.setLineWidth(2.0);
                    double peakY = height - peakHeight;
                    gc.strokeLine(x, peakY, x + barWidth, peakY);
                }
            }
        }
    }

    /**
     * 绘制单个带有颜色渐变的频谱条
     *
     * @param gc         图形上下文
     * @param x          条柱 x 坐标
     * @param y          条柱 y 坐标（顶部）
     * @param width      条柱宽度
     * @param height     条柱高度
     * @param colorScale 颜色渐变缩放因子
     */
    private void drawGradientBar(GraphicsContext gc, double x, double y,
                                 double width, double height, float colorScale) {
        if (height <= 0) {
            return;
        }

        int offset = barOffset.get();
        double bottom = y + height;
        float c = 0;

        // 从底部到顶部绘制带有渐变的条柱
        for (double yPos = bottom; yPos >= y; yPos -= offset) {
            c += colorScale;

            // 从渐变中获取颜色
            int colorIndex = (int) c;
            if (colorIndex >= 0 && colorIndex < colorGradient.getColorCount()) {
                Color color = colorGradient.getColor(colorIndex / (float) colorGradient.getColorCount());
                gc.setFill(color);
                gc.fillRect(x, yPos, width, offset);
            }
        }
    }

    /**
     * 获取当前颜色渐变
     *
     * @return 颜色渐变
     */
    public ColorGradient getColorGradient() {
        return colorGradient;
    }

    /**
     * 设置自定义颜色渐变
     *
     * @param gradient 颜色渐变
     */
    public void setColorGradient(ColorGradient gradient) {
        if (gradient != null) {
            this.colorGradient = gradient;
        }
    }

    @Override
    protected void onResize() {
        // 当 Canvas 尺寸改变时重置峰值
        if (peakDetector != null) {
            peakDetector.reset();
        }
    }

    // 属性访问器

    /**
     * 频带数量属性
     *
     * @return 频带数量属性
     */
    public IntegerProperty bandCountProperty() {
        return bandCount;
    }

    /**
     * 获取频带数量
     *
     * @return 频带数量
     */
    public int getBandCount() {
        return bandCount.get();
    }

    /**
     * 设置频带数量
     *
     * @param count 频带数量（必须为正数）
     */
    public void setBandCount(int count) {
        if (count > 0) {
            bandCount.set(count);
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
     * 显示峰值属性
     *
     * @return 显示峰值属性
     */
    public BooleanProperty showPeaksProperty() {
        return showPeaks;
    }

    /**
     * 获取是否显示峰值
     *
     * @return 如果显示峰值则返回 true
     */
    public boolean isShowPeaks() {
        return showPeaks.get();
    }

    /**
     * 设置是否显示峰值
     *
     * @param show 是否显示峰值
     */
    public void setShowPeaks(boolean show) {
        showPeaks.set(show);
    }

    /**
     * 条柱间距属性
     *
     * @return 条柱间距属性
     */
    public IntegerProperty barSpacingProperty() {
        return barSpacing;
    }

    /**
     * 获取条柱间距
     *
     * @return 条柱间距（像素）
     */
    public int getBarSpacing() {
        return barSpacing.get();
    }

    /**
     * 设置条柱间距
     *
     * @param spacing 条柱间距（像素，必须为非负数）
     */
    public void setBarSpacing(int spacing) {
        if (spacing >= 0) {
            barSpacing.set(spacing);
        }
    }

    /**
     * 峰值颜色属性
     *
     * @return 峰值颜色属性
     */
    public ObjectProperty<Color> peakColorProperty() {
        return peakColor;
    }

    /**
     * 获取峰值颜色
     *
     * @return 峰值颜色
     */
    public Color getPeakColor() {
        return peakColor.get();
    }

    /**
     * 设置峰值颜色
     *
     * @param color 峰值颜色
     */
    public void setPeakColor(Color color) {
        if (color != null) {
            peakColor.set(color);
        }
    }

    /**
     * 条柱偏移量属性（用于渐变渲染）
     *
     * @return 条柱偏移量属性
     */
    public IntegerProperty barOffsetProperty() {
        return barOffset;
    }

    /**
     * 获取条柱偏移量
     *
     * @return 条柱偏移量
     */
    public int getBarOffset() {
        return barOffset.get();
    }

    /**
     * 设置条柱偏移量
     *
     * @param offset 条柱偏移量（必须为正数）
     */
    public void setBarOffset(int offset) {
        if (offset > 0) {
            barOffset.set(offset);
        }
    }

    /**
     * 峰值保持帧数属性
     *
     * @return 峰值保持帧数属性
     */
    public IntegerProperty peakHoldFramesProperty() {
        return peakHoldFrames;
    }

    /**
     * 获取峰值保持帧数
     *
     * @return 峰值保持帧数
     */
    public int getPeakHoldFrames() {
        return peakHoldFrames.get();
    }

    /**
     * 设置峰值保持帧数
     *
     * @param frames 峰值保持帧数（必须为正数）
     */
    public void setPeakHoldFrames(int frames) {
        if (frames > 0) {
            peakHoldFrames.set(frames);
        }
    }

    /**
     * 峰值下降速度属性
     *
     * @return 峰值下降速度属性
     */
    public DoubleProperty peakFallSpeedProperty() {
        return peakFallSpeed;
    }

    /**
     * 获取峰值下降速度
     *
     * @return 峰值下降速度
     */
    public double getPeakFallSpeed() {
        return peakFallSpeed.get();
    }

    /**
     * 设置峰值下降速度
     *
     * @param speed 峰值下降速度（必须为非负数）
     */
    public void setPeakFallSpeed(double speed) {
        if (speed >= 0) {
            peakFallSpeed.set(speed);
        }
    }
}