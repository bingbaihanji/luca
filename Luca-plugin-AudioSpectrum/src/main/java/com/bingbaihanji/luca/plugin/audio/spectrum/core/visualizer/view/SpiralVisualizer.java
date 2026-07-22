package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.view;


import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioData;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core.AudioVisualizer;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal.SpectrumAnalyzer;
import javafx.beans.property.*;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * 螺旋可视化器（简化的多螺旋/玫瑰花结）
 *
 * <p>根据音频强度显示动画螺旋和粒子效果
 *
 * @author bingbaihanji
 */
public class SpiralVisualizer extends AudioVisualizer {

    // 处理器配置

    private final Random random = new Random();
    private final IntegerProperty fftSize = new SimpleIntegerProperty(512);
    private final IntegerProperty bandCount = new SimpleIntegerProperty(3);
    private final DoubleProperty decay = new SimpleDoubleProperty(0.05);
    private final IntegerProperty particleDensity = new SimpleIntegerProperty(50);

    // 配置属性
    private final IntegerProperty rosetteVertices = new SimpleIntegerProperty(6);
    private final BooleanProperty rainbowMode = new SimpleBooleanProperty(true);
    private final ObjectProperty<Color> particleColor = new SimpleObjectProperty<>(Color.CYAN);
    private SpectrumAnalyzer analyzer;

    // 彩虹色状态
    private int colorIndex = 0;

    // 玫瑰花结状态

    private double rosetteAngle = 0;
    private double centerX, centerY;
    private double radius;

    /**
     * 创建一个新的螺旋可视化器
     */
    public SpiralVisualizer() {
        super();
        initializeAnalyzer();

        // 当配置变化时重新初始化分析器
        fftSize.addListener((obs, old, val) -> initializeAnalyzer());
        bandCount.addListener((obs, old, val) -> initializeAnalyzer());
        decay.addListener((obs, old, val) -> initializeAnalyzer());
    }

    /**
     * 初始化频谱分析器
     */
    private void initializeAnalyzer() {
        analyzer = new SpectrumAnalyzer(fftSize.get(), bandCount.get(), (float) decay.get());
    }

    @Override
    protected void render(GraphicsContext gc, AudioData data) {
        double width = getCanvasWidth();
        double height = getCanvasHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        centerX = width / 2.0;
        centerY = height / 2.0;
        radius = Math.min(width, height) / 3.0;

        // 分析音频以获取强度
        float[] bands = analyzer.analyze(data.getStereoMerge());
        float lowIntensity = bands[0];
        float midIntensity = bands.length > 1 ? bands[1] : 0;

        // 根据强度绘制粒子
        drawParticles(gc, lowIntensity);

        // 绘制玫瑰花结
        drawRosette(gc, midIntensity);

        // 更新玫瑰花结角度
        rosetteAngle += 0.02 * (1.0 + lowIntensity);
    }

    /**
     * 绘制随机粒子
     */
    private void drawParticles(GraphicsContext gc, float intensity) {
        int particleCount = (int) (particleDensity.get() * (intensity + 0.2));

        for (int i = 0; i < particleCount; i++) {
            double x = random.nextDouble() * getCanvasWidth();
            double y = random.nextDouble() * getCanvasHeight();
            double size = 2 + random.nextDouble() * 4;

            Color color = rainbowMode.get() ?
                    Color.hsb((colorIndex + i * 5) % 360, 1.0, 1.0) :
                    particleColor.get();

            gc.setFill(color);
            gc.fillOval(x, y, size, size);
        }

        colorIndex = (colorIndex + 2) % 360;
    }

    /**
     * 绘制玫瑰花结图案
     */
    private void drawRosette(GraphicsContext gc, float intensity) {
        int vertices = rosetteVertices.get();
        if (vertices < 3) {
            return;
        }

        double adjustedRadius = radius * (0.5 + intensity);
        double angleStep = 2 * Math.PI / vertices;

        Point2D[] points = new Point2D[vertices];

        // 计算玫瑰花结顶点
        for (int i = 0; i < vertices; i++) {
            double angle = rosetteAngle + i * angleStep;
            double x = centerX + Math.cos(angle) * adjustedRadius;
            double y = centerY + Math.sin(angle) * adjustedRadius;
            points[i] = new Point2D(x, y);
        }

        // 绘制连线
        gc.setStroke(rainbowMode.get() ?
                Color.hsb((colorIndex + 180) % 360, 1.0, 1.0) :
                particleColor.get());
        gc.setLineWidth(2);

        for (int i = 0; i < vertices; i++) {
            int next = (i + 1) % vertices;
            gc.strokeLine(points[i].getX(), points[i].getY(),
                    points[next].getX(), points[next].getY());
        }

        // 绘制中心连线
        gc.setLineWidth(1);
        for (Point2D point : points) {
            gc.strokeLine(centerX, centerY, point.getX(), point.getY());
        }
    }

    // 属性访问器

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
     * 粒子密度属性
     *
     * @return 粒子密度属性
     */
    public IntegerProperty particleDensityProperty() {
        return particleDensity;
    }

    /**
     * 获取粒子密度
     *
     * @return 粒子密度
     */
    public int getParticleDensity() {
        return particleDensity.get();
    }

    /**
     * 设置粒子密度
     *
     * @param density 粒子密度（必须为正数）
     */
    public void setParticleDensity(int density) {
        if (density > 0) {
            particleDensity.set(density);
        }
    }

    /**
     * 玫瑰花结顶点数属性
     *
     * @return 玫瑰花结顶点数属性
     */
    public IntegerProperty rosetteVerticesProperty() {
        return rosetteVertices;
    }

    /**
     * 获取玫瑰花结顶点数
     *
     * @return 玫瑰花结顶点数
     */
    public int getRosetteVertices() {
        return rosetteVertices.get();
    }

    /**
     * 设置玫瑰花结顶点数
     *
     * @param vertices 玫瑰花结顶点数（必须 >= 3）
     */
    public void setRosetteVertices(int vertices) {
        if (vertices >= 3) {
            rosetteVertices.set(vertices);
        }
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
     * 粒子颜色属性
     *
     * @return 粒子颜色属性
     */
    public ObjectProperty<Color> particleColorProperty() {
        return particleColor;
    }

    /**
     * 获取粒子颜色
     *
     * @return 粒子颜色
     */
    public Color getParticleColor() {
        return particleColor.get();
    }

    /**
     * 设置粒子颜色
     *
     * @param color 粒子颜色
     */
    public void setParticleColor(Color color) {
        if (color != null) {
            particleColor.set(color);
        }
    }
}