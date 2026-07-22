package com.bingbaihanji.luca.plugin.audio.spectrum.core.spectrum;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Arrays;

/**
 * 实时音频频谱可视化组件-发光试管
 * 用于实时显示音频播放时的频谱数据
 */
public class SpectrumTubeView extends Canvas implements AudioSpectrumVisualizer {

    // 下降速度（总高度60）：（60/100）秒内从最大值到最小值
    private static final float FADE_SPEED = 100f; // 单位/秒
    // 背景色
    private static final Color BACKGROUND_COLOR = Color.rgb(23, 28, 35, 0.85);
    // 中心线颜色
    private static final Color CENTER_LINE_COLOR = Color.rgb(100, 150, 200, 0.3);
    private final GraphicsContext gc;
    // 当前显示的频谱值（用于平滑动画）
    private final float[] displayMagnitudes;
    // 目标频谱值（从音频系统接收到的实时数据）
    private float[] targetMagnitudes;
    // 动画定时器
    private AnimationTimer animationTimer;
    private long lastUpdateTime;
    private volatile boolean running = false;

    public SpectrumTubeView(double width, double height) {
        super(width, height);
        this.gc = getGraphicsContext2D();

        // 初始化数组
        targetMagnitudes = new float[128];
        displayMagnitudes = new float[128];
        Arrays.fill(targetMagnitudes, -60f);
        Arrays.fill(displayMagnitudes, -60f);

        // 添加尺寸监听器
        widthProperty().addListener((observable, oldValue, newValue) -> draw());
        heightProperty().addListener((observable, oldValue, newValue) -> draw());

        // 初始绘制
        draw();

        // 启动动画
        startAnimation();
        start(); // 启动动画

        // 离开场景（标签切走）时物理暂停定时器，切回来自动恢复，避免隐藏 Canvas 占用 60fps
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (animationTimer == null) {
                return;
            }
            if (newScene == null) {
                animationTimer.stop();
            } else if (running) {
                animationTimer.start();
            }
        });
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double minWidth(double height) {
        return 0.0;
    }

    @Override
    public double minHeight(double width) {
        return 0.0;
    }

    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public void resize(double w, double h) {
        // 防御性处理：忽略无效尺寸
        double newW = Double.isFinite(w) && w > 0.0 ? w : 0.0;
        double newH = Double.isFinite(h) && h > 0.0 ? h : 0.0;

        // 只有在尺寸真正变化时才设置并重绘
        if (getWidth() != newW || getHeight() != newH) {
            setWidth(newW);
            setHeight(newH);
            draw(); // 立即根据新尺寸重绘，避免依赖监听器触发
        }
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

    /**
     * 启动动画循环
     */
    private void startAnimation() {

        if (running) {
            return; // 已运行则直接返回
        }


        lastUpdateTime = System.nanoTime();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0; // 转换为秒
                lastUpdateTime = now;

                boolean needsRedraw = false;

                // 更新每个频段的显示值
                for (int i = 0; i < displayMagnitudes.length; i++) {
                    float target = targetMagnitudes[i];
                    float current = displayMagnitudes[i];

                    if (target > current) {
                        // 目标值更大：立即跳变
                        displayMagnitudes[i] = target;
                        needsRedraw = true;
                    } else if (target < current) {
                        // 目标值更小：平滑下降
                        float decrease = (float) (FADE_SPEED * deltaTime);
                        displayMagnitudes[i] = Math.max(current - decrease, target);
                        needsRedraw = true;
                    }
                }

                // 如果有变化则重新绘制
                if (needsRedraw) {
                    draw();
                }
            }
        };
        animationTimer.start();
        running = true;
    }

    /**
     * 更新频谱数据
     *
     * @param magnitudes 频谱幅度数据（来自 AudioSpectrumListener）
     */
    @Override
    public void updateSpectrum(float[] magnitudes) {
        this.targetMagnitudes = Arrays.copyOf(magnitudes, magnitudes.length);
    }

    /**
     * 获取基于位置的渐变颜色
     * 渐变: 绿 → 青 → 黄 → 橙 → 红
     */
    private Color getColorForPosition(double position) {

        position = Math.max(0, Math.min(1, position));

        if (position < 0.25) {
            return lerpColor(position / 0.25, 0, 255, 0, 0, 255, 255);
        } else if (position < 0.5) {
            return lerpColor((position - 0.25) / 0.25, 0, 255, 255, 255, 255, 0);
        } else if (position < 0.75) {
            return lerpColor((position - 0.5) / 0.25, 255, 255, 0, 255, 165, 0);
        } else {
            return lerpColor((position - 0.75) / 0.25, 255, 165, 0, 255, 0, 0);
        }
    }

    private Color lerpColor(double t,
                            int r1, int g1, int b1,
                            int r2, int g2, int b2) {

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return Color.rgb(r, g, b);
    }

    /**
     * 绘制频谱可视化
     */
    private void draw() {
        double w = getWidth();
        double h = getHeight();
        double centerY = h / 2.0;
        double centerX = w / 2.0;

        // 清空画布
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0.0, 0.0, w, h);

        // 绘制中心线
        gc.setStroke(CENTER_LINE_COLOR);
        gc.setLineWidth(1.0);
        gc.strokeLine(0.0, centerY, w, centerY);

        if (displayMagnitudes.length == 0) {
            return;
        }

        // 只取奇数索引的频段（0, 2, 4, ..., 126），共64个
        int displayBands = 64;
        double bandWidth = (w / 2.0) / displayBands;

        // 绘制右侧频段（从中心向右）
        for (int i = 0; i < displayBands; i++) {
            int dataIndex = i * 2; // 取偶数索引: 0, 2, 4, ..., 126
            if (dataIndex >= displayMagnitudes.length) {
                break;
            }

            double x = centerX + i * bandWidth + bandWidth / 2;
            float normalizedValue = (displayMagnitudes[dataIndex] + 60f) / 60f;
            float amplitude = Math.max(0f, Math.min(1f, normalizedValue));
            double barHeight = amplitude * (h / 2.0) * 0.85;

            // 计算当前位置的颜色（0.5 到 1.0）
            double position = 0.5 + (i / (double) displayBands) * 0.5;
            Color color = getColorForPosition(position);

            // 绘制中空试管效果
            drawTube(x, centerY, barHeight, bandWidth * 0.7, color);
        }

        // 绘制左侧频段（镜像对称）
        for (int i = 0; i < displayBands; i++) {
            int dataIndex = i * 2;
            if (dataIndex >= displayMagnitudes.length) {
                break;
            }

            double x = centerX - i * bandWidth - bandWidth / 2;
            float normalizedValue = (displayMagnitudes[dataIndex] + 60f) / 60f;
            float amplitude = Math.max(0f, Math.min(1f, normalizedValue));
            double barHeight = amplitude * (h / 2.0) * 0.85;

            // 计算当前位置的颜色（0.5 到 0.0）
            double position = 0.5 - (i / (double) displayBands) * 0.5;
            Color color = getColorForPosition(position);

            // 绘制中空试管效果
            drawTube(x, centerY, barHeight, bandWidth * 0.7, color);
        }
    }

    /**
     * 绘制中空试管效果（带发光边缘）
     */
    private void drawTube(double x, double centerY, double height, double width, Color color) {
        double halfWidth = width / 2.0;

        // 1. 绘制外发光层（最大半径）
        gc.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.15));
        gc.setLineWidth(3.5);
        drawTubeLine(x, centerY, height, halfWidth);

        // 2. 绘制中发光层
        gc.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.3));
        gc.setLineWidth(2.5);
        drawTubeLine(x, centerY, height, halfWidth);

        // 3. 绘制内发光层
        gc.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.6));
        gc.setLineWidth(1.8);
        drawTubeLine(x, centerY, height, halfWidth);

        // 4. 绘制主线条
        gc.setStroke(color);
        gc.setLineWidth(1.2);
        drawTubeLine(x, centerY, height, halfWidth);
    }

    /**
     * 绘制试管形状的单条线
     */
    private void drawTubeLine(double x, double centerY, double height, double halfWidth) {
        if (height < 2.0) {
            // 高度太小，只绘制一个点
            gc.strokeLine(x, centerY, x, centerY);
            return;
        }

        // 上半部分
        double topY = centerY - height;
        // 左边线
        gc.strokeLine(x - halfWidth, centerY, x - halfWidth, topY);
        // 顶部圆弧（用多段直线模拟）
        int arcSegments = 8;
        for (int i = 0; i < arcSegments; i++) {
            double angle1 = Math.PI + i * Math.PI / arcSegments;
            double angle2 = Math.PI + (i + 1) * Math.PI / arcSegments;
            double x1 = x + halfWidth * Math.cos(angle1);
            double y1 = topY + halfWidth * Math.sin(angle1);
            double x2 = x + halfWidth * Math.cos(angle2);
            double y2 = topY + halfWidth * Math.sin(angle2);
            gc.strokeLine(x1, y1, x2, y2);
        }
        // 右边线
        gc.strokeLine(x + halfWidth, topY, x + halfWidth, centerY);

        // 下半部分（镜像）
        double bottomY = centerY + height;
        // 左边线
        gc.strokeLine(x - halfWidth, centerY, x - halfWidth, bottomY);
        // 底部圆弧
        for (int i = 0; i < arcSegments; i++) {
            double angle1 = i * Math.PI / arcSegments;
            double angle2 = (i + 1) * Math.PI / arcSegments;
            double x1 = x + halfWidth * Math.cos(angle1);
            double y1 = bottomY + halfWidth * Math.sin(angle1);
            double x2 = x + halfWidth * Math.cos(angle2);
            double y2 = bottomY + halfWidth * Math.sin(angle2);
            gc.strokeLine(x1, y1, x2, y2);
        }
        // 右边线
        gc.strokeLine(x + halfWidth, bottomY, x + halfWidth, centerY);
    }

    /**
     * 清空频谱显示
     */
    @Override
    public void clear() {
        Arrays.fill(targetMagnitudes, -60f);
        // displayMagnitudes 会通过动画自动平滑下降到 -60f
    }

    @Override
    public void start() {
        if (!running) {
            startAnimation();
        }
    }

    @Override
    public void stop() {
        if (running && animationTimer != null) {
            animationTimer.stop();
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 停止动画（清理资源）
     */
    @Override
    public void dispose() {
        stop(); // 先停止动画
        animationTimer = null; // 释放定时器引用
    }
}