package com.bingbaihanji.luca.plugin.audio.spectrum.core.spectrum;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.Arrays;

/**
 * 辐射状音频频谱可视化组件
 * 圆形布局，中心显示时间，带内外圈进度条
 */
public class RadialSpectrumView extends Canvas implements AudioSpectrumVisualizer {

    // 下降速度
    private static final float FADE_SPEED = 100f;
    // 频谱柱数量（环绕一圈）
    private static final int SPECTRUM_BARS = 64;
    // 布局参数
    private static final double PROGRESS_RING_RATIO = 0.30; // 进度条半径占比（时间区域和频谱柱之间）
    private static final double SPECTRUM_START_RATIO = 0.35; // 频谱柱起始半径占比
    private static final double SPECTRUM_MAX_LENGTH_RATIO = 0.35; // 频谱柱最大长度占比
    // 颜色定义
    private static final Color BACKGROUND_COLOR = Color.rgb(23, 28, 35, 0.95);
    private static final Color PRIMARY_COLOR = Color.rgb(60, 144, 241); // 蓝色
    private static final Color SECONDARY_COLOR = Color.rgb(0, 255, 255); // 青色
    private final GraphicsContext gc;
    //  当前显示的频谱值（用于平滑动画）
    private final float[] displayMagnitudes;
    // 频谱数据
    private float[] targetMagnitudes;
    // 动画定时器
    private AnimationTimer animationTimer;
    private long lastUpdateTime;
    // 播放时间和进度
    private String timeText = "0:00";
    private double progress = 0.0; // 播放进度 0.0 - 1.0
    // 峰值连线开关
    private boolean enablePeakLine = false;
    private volatile boolean running = false;

    public RadialSpectrumView(double width, double height) {
        super(width, height);
        this.gc = getGraphicsContext2D();

        // 初始化频谱数据
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
        start();

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
        double newW = Double.isFinite(w) && w > 0.0 ? w : 0.0;
        double newH = Double.isFinite(h) && h > 0.0 ? h : 0.0;

        if (getWidth() != newW || getHeight() != newH) {
            setWidth(newW);
            setHeight(newH);
            draw();
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
     * 设置播放时间文本
     */
    public void setTimeText(String timeText) {
        this.timeText = timeText;
    }

    /**
     * 设置播放进度
     *
     * @param progress 0.0 - 1.0
     */
    public void setProgress(double progress) {
        this.progress = Math.max(0.0, Math.min(1.0, progress));
    }

    /**
     * 获取峰值连线状态
     *
     * @return true 已启用，false 未启用
     */
    public boolean isEnablePeakLine() {
        return enablePeakLine;
    }

    /**
     * 设置是否启用峰值连线
     *
     * @param enable true 启用，false 禁用
     */
    public void setEnablePeakLine(boolean enable) {
        this.enablePeakLine = enable;
    }

    /**
     * 启动动画循环
     */
    private void startAnimation() {
        if (running) {
            return;
        }

        lastUpdateTime = System.nanoTime();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0;
                lastUpdateTime = now;

                boolean needsRedraw = false;

                // 更新频谱显示值
                for (int i = 0; i < displayMagnitudes.length; i++) {
                    float target = targetMagnitudes[i];// 原生频谱数据
                    float current = displayMagnitudes[i]; // 要展示的频谱值

                    if (target > current) {
                        displayMagnitudes[i] = target; // 原生频谱数据大于要展示的频谱数据时，直接赋值
                        needsRedraw = true;
                    } else if (target < current) {
                        float decrease = (float) (FADE_SPEED * deltaTime);
                        displayMagnitudes[i] = Math.max(current - decrease, target);
                        needsRedraw = true;
                    }
                }

                if (needsRedraw) {
                    draw();
                }
            }
        };
        animationTimer.start();
        running = true;
    }

    @Override
    public void updateSpectrum(float[] magnitudes) {
        // this.targetMagnitudes = Arrays.copyOf(magnitudes, magnitudes.length);

        arrayMirrorFormatting(magnitudes, this.targetMagnitudes);

    }

    /**
     * 数组镜像格式化
     * 将 first 数组中的元素按镜像对称的方式复制到 second 数组中
     *
     * @param first  源数组，提供原始数据
     * @param second 目标数组，接收镜像格式化后的数据，长度必须满足要求
     * @throws IllegalArgumentException 当 second 数组长度不足时抛出
     */
    private void arrayMirrorFormatting(float[] first, float[] second) {
        // 计算偶数位置的数量
        int evenCount = (first.length + 1) >> 1;
        // 计算需要填充的目标数组长度
        int len = evenCount * 2 - (first.length & 1);

        if (second.length < len) {
            throw new IllegalArgumentException();
        }

        // 将 first 数组的偶数位置元素镜像复制到 second 数组
        for (int i = 0; i < evenCount; i++) {
            second[i] = first[i << 1];
            second[len - 1 - i] = first[i << 1];
        }

        // 如果 first 数组长度为奇数，则中间位置重复前一个值
        if ((first.length & 1) == 1) {
            second[evenCount] = second[evenCount - 1];
        }

    }


    /**
     * 绘制频谱可视化
     */
    private void draw() {
        double w = getWidth();
        double h = getHeight();
        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double radius = Math.min(w, h) / 2.0 * 0.9;

        // 清空画布
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, w, h);

        // 绘制根部渐变环（在频谱柱下方，仅在未开启峰值连线时绘制）
        if (!enablePeakLine) {
            drawBaseRing(centerX, centerY, radius);
        }

        // 绘制辐射状频谱柱
        drawRadialSpectrum(centerX, centerY, radius);

        // 绘制外圈进度条（点状）
        drawOuterProgressRing(centerX, centerY, radius);

        // 绘制内圈进度条（圆形轨道）
        drawInnerProgressRing(centerX, centerY, radius);

        // 绘制中心时间文本
        drawCenterTime(centerX, centerY, radius);
    }

    /**
     * 绘制频谱柱根部的渐变环（连接所有柱子的基座）
     */
    private void drawBaseRing(double centerX, double centerY, double radius) {
        double innerRadius = radius * SPECTRUM_START_RATIO;
        double ringWidth = 2.0; // 环的宽度（减小为2.0使环更细）

        // 创建径向渐变：从内向外 PRIMARY_COLOR -> SECONDARY_COLOR
        javafx.scene.paint.RadialGradient gradient = new javafx.scene.paint.RadialGradient(
                0, 0,
                centerX, centerY,
                innerRadius,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0.85, PRIMARY_COLOR.deriveColor(0, 1, 1, 0.8)),
                new Stop(1.0, SECONDARY_COLOR.deriveColor(0, 1, 1, 0.6))
        );

        gc.setLineCap(StrokeLineCap.ROUND);

        // 绘制外层发光效果
        gc.setStroke(SECONDARY_COLOR.deriveColor(0, 1, 1, 0.25));
        gc.setLineWidth(ringWidth + 3);
        gc.strokeOval(centerX - innerRadius, centerY - innerRadius,
                innerRadius * 2, innerRadius * 2);

        // 绘制中层发光
        gc.setStroke(SECONDARY_COLOR.deriveColor(0, 1, 1, 0.4));
        gc.setLineWidth(ringWidth + 1.5);
        gc.strokeOval(centerX - innerRadius, centerY - innerRadius,
                innerRadius * 2, innerRadius * 2);

        // 绘制主环（使用渐变）
        gc.setStroke(gradient);
        gc.setLineWidth(ringWidth);
        gc.strokeOval(centerX - innerRadius, centerY - innerRadius,
                innerRadius * 2, innerRadius * 2);
    }

    /**
     * 绘制辐射状频谱柱（从根部青色到顶部绿色的径向渐变）
     */
    private void drawRadialSpectrum(double centerX, double centerY, double radius) {
        if (displayMagnitudes.length == 0) {
            return;
        }

        double innerRadius = radius * SPECTRUM_START_RATIO;
        double maxBarLength = radius * SPECTRUM_MAX_LENGTH_RATIO;

        gc.setLineCap(StrokeLineCap.ROUND);

        // 峰值连线坐标数组（仅在启用时分配）
        double[] peakX = enablePeakLine ? new double[SPECTRUM_BARS] : null;
        double[] peakY = enablePeakLine ? new double[SPECTRUM_BARS] : null;

        for (int i = 0; i < SPECTRUM_BARS; i++) {
            double angle = (i / (double) SPECTRUM_BARS) * 2.0 * Math.PI - Math.PI / 2.0;

            int dataIndex = (i * displayMagnitudes.length) / SPECTRUM_BARS;
            if (dataIndex >= displayMagnitudes.length) {
                dataIndex = displayMagnitudes.length - 1;
            }

            float normalizedValue = (displayMagnitudes[dataIndex] + 60f) / 60f;
            float amplitude = Math.max(0f, Math.min(1f, normalizedValue));
            double barLength = amplitude * maxBarLength;

            double startX = centerX + Math.cos(angle) * innerRadius;
            double startY = centerY + Math.sin(angle) * innerRadius;
            double endX = centerX + Math.cos(angle) * (innerRadius + barLength);
            double endY = centerY + Math.sin(angle) * (innerRadius + barLength);

            // 记录峰值点（用于连线）
            if (enablePeakLine) {
                peakX[i] = endX;
                peakY[i] = endY;
            }

            // 仅在未开启峰值连线时绘制柱状（使用径向渐变）
            if (!enablePeakLine && barLength > 0) {
                // 为每根柱子创建从根部（青色）到顶部（绿色）的线性渐变
                // 使用绝对坐标模式（proportional=false）确保渐变沿着柱子方向
                Stop[] stops03 = {
                        new Stop(0, SECONDARY_COLOR.deriveColor(0, 1, 1, 0.3)),
                        new Stop(1, PRIMARY_COLOR.deriveColor(0, 1, 1, 0.3))
                };
                Stop[] stops06 = {
                        new Stop(0, SECONDARY_COLOR.deriveColor(0, 1, 1, 0.6)),
                        new Stop(1, PRIMARY_COLOR.deriveColor(0, 1, 1, 0.6))
                };
                Stop[] stops10 = {
                        new Stop(0, SECONDARY_COLOR),
                        new Stop(1, PRIMARY_COLOR)
                };

                LinearGradient gradient03 = new LinearGradient(startX, startY, endX, endY,
                        false, CycleMethod.NO_CYCLE, stops03);
                LinearGradient gradient06 = new LinearGradient(startX, startY, endX, endY,
                        false, CycleMethod.NO_CYCLE, stops06);
                LinearGradient gradient10 = new LinearGradient(startX, startY, endX, endY,
                        false, CycleMethod.NO_CYCLE, stops10);

                // 发光层（三层透明度递减）
                gc.setStroke(gradient03);
                gc.setLineWidth(8);
                gc.strokeLine(startX, startY, endX, endY);

                gc.setStroke(gradient06);
                gc.setLineWidth(6);
                gc.strokeLine(startX, startY, endX, endY);

                gc.setStroke(gradient10);
                gc.setLineWidth(5);
                gc.strokeLine(startX, startY, endX, endY);
            }
        }

        // 绘制峰值连线（如果启用）
        if (enablePeakLine && peakX != null) {
            drawPeakLine(peakX, peakY);
        }
    }

    /**
     * 绘制平滑的峰值连线（闭合曲线），使用径向渐变效果
     */
    private void drawPeakLine(double[] peakX, double[] peakY) {
        if (peakX == null || peakY == null || peakX.length < 2) {
            return;
        }

        // 计算边界框用于创建渐变
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        for (int i = 0; i < peakX.length; i++) {
            minX = Math.min(minX, peakX[i]);
            maxX = Math.max(maxX, peakX[i]);
            minY = Math.min(minY, peakY[i]);
            maxY = Math.max(maxY, peakY[i]);
        }

        // 创建从左上到右下的渐变（用于闭合曲线的整体效果）
        Stop[] stops02 = {
                new Stop(0, SECONDARY_COLOR.deriveColor(0, 1, 1, 0.2)),
                new Stop(0.5, PRIMARY_COLOR.deriveColor(0, 1, 1, 0.15)),
                new Stop(1, SECONDARY_COLOR.deriveColor(0, 1, 1, 0.2))
        };
        Stop[] stops10 = {
                new Stop(0, SECONDARY_COLOR),
                new Stop(0.5, PRIMARY_COLOR),
                new Stop(1, SECONDARY_COLOR)
        };

        LinearGradient gradient02 = new LinearGradient(minX, minY, maxX, maxY,
                false, CycleMethod.NO_CYCLE, stops02);
        LinearGradient gradient10 = new LinearGradient(minX, minY, maxX, maxY,
                false, CycleMethod.NO_CYCLE, stops10);

        gc.setLineCap(StrokeLineCap.ROUND);

        // 外层发光（粗线，低透明度）
        gc.setStroke(gradient02);
        gc.setLineWidth(3);
        drawSmoothClosedPath(gc, peakX, peakY);
        gc.stroke();

        // 主线
        gc.setStroke(gradient10);
        gc.setLineWidth(1.5);
        drawSmoothClosedPath(gc, peakX, peakY);
        gc.stroke();
    }

    /**
     * 绘制闭合折线（直接连接各点）
     */
    private void drawClosedPath(GraphicsContext gc, double[] x, double[] y) {
        int n = x.length;
        gc.beginPath();
        gc.moveTo(x[0], y[0]);
        for (int i = 1; i < n; i++) {
            gc.lineTo(x[i], y[i]);
        }
        gc.closePath(); // 自动连接最后一个点到第一个点，形成闭合
    }

    /**
     * 优化 drawClosedPath 方法构建平滑闭合路径（Catmull-Rom 样条转换为三次贝塞尔曲线）
     */

    private void drawSmoothClosedPath(GraphicsContext gc, double[] x, double[] y) {
        int n = x.length;
        gc.beginPath();
        gc.moveTo(x[0], y[0]);

        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;                 // 下一个点
            int prev = (i - 1 + n) % n;           // 前一个点
            int next2 = (i + 2) % n;              // 后两个点（用于计算第二个控制点）

            // 计算三次贝塞尔曲线的两个控制点（Catmull-Rom 张力参数设为 0.5，对应除以6的系数）
            double cp1x = x[i] + (x[j] - x[prev]) / 6.0;
            double cp1y = y[i] + (y[j] - y[prev]) / 6.0;
            double cp2x = x[j] - (x[next2] - x[i]) / 6.0;
            double cp2y = y[j] - (y[next2] - y[i]) / 6.0;

            gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x[j], y[j]);
        }

    }


    /**
     * 绘制外圈进度条（点状）
     */
    private void drawOuterProgressRing(double centerX, double centerY, double radius) {
        double ringRadius = radius * 0.85;
        int dotCount = 120;

        for (int i = 0; i < dotCount; i++) {
            double angle = (i / (double) dotCount) * 2.0 * Math.PI - Math.PI / 2.0;
            double x = centerX + Math.cos(angle) * ringRadius;
            double y = centerY + Math.sin(angle) * ringRadius;

            double dotProgress = (i / (double) dotCount);
            Color dotColor;

            if (dotProgress <= progress) {
                // 已播放部分：青色发光
                dotColor = SECONDARY_COLOR;
                gc.setFill(Color.color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), 0.3));
                gc.fillOval(x - 3, y - 3, 6, 6);
                gc.setFill(dotColor);
                gc.fillOval(x - 2, y - 2, 4, 4);
            } else {
                // 未播放部分：暗色
                dotColor = Color.rgb(60, 80, 120, 0.4);
                gc.setFill(dotColor);
                gc.fillOval(x - 1.5, y - 1.5, 3, 3);
            }
        }
    }

    /**
     * 绘制内圈进度条（连续圆弧线）
     */
    private void drawInnerProgressRing(double centerX, double centerY, double radius) {
        double innerRadius = radius * PROGRESS_RING_RATIO;
        double lineWidth = 3.5;

        gc.setLineCap(StrokeLineCap.ROUND);

        // 绘制底部轨道（暗色完整圆环）
        gc.setStroke(Color.rgb(60, 80, 120, 0.5));
        gc.setLineWidth(lineWidth);
        gc.strokeOval(centerX - innerRadius, centerY - innerRadius,
                innerRadius * 2, innerRadius * 2);

        // 绘制进度部分（发光圆弧）
        if (progress > 0) {
            double arcAngle = progress * 360.0;

            // 最外层发光
            gc.setStroke(Color.color(SECONDARY_COLOR.getRed(), SECONDARY_COLOR.getGreen(),
                    SECONDARY_COLOR.getBlue(), 0.15));
            gc.setLineWidth(lineWidth + 6);
            gc.strokeArc(centerX - innerRadius, centerY - innerRadius,
                    innerRadius * 2, innerRadius * 2,
                    90, -arcAngle, javafx.scene.shape.ArcType.OPEN);

            // 中层发光
            gc.setStroke(Color.color(SECONDARY_COLOR.getRed(), SECONDARY_COLOR.getGreen(),
                    SECONDARY_COLOR.getBlue(), 0.3));
            gc.setLineWidth(lineWidth + 3);
            gc.strokeArc(centerX - innerRadius, centerY - innerRadius,
                    innerRadius * 2, innerRadius * 2,
                    90, -arcAngle, javafx.scene.shape.ArcType.OPEN);

            // 主线（高亮青色圆弧）
            gc.setStroke(SECONDARY_COLOR);
            gc.setLineWidth(lineWidth);
            gc.strokeArc(centerX - innerRadius, centerY - innerRadius,
                    innerRadius * 2, innerRadius * 2,
                    90, -arcAngle, javafx.scene.shape.ArcType.OPEN);
        }
    }

    /**
     * 绘制中心时间文本
     */
    private void drawCenterTime(double centerX, double centerY, double radius) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

        // 计算字体大小
        double fontSize = radius * 0.15;
        gc.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));

        // 绘制发光效果
        gc.setFill(Color.color(SECONDARY_COLOR.getRed(), SECONDARY_COLOR.getGreen(),
                SECONDARY_COLOR.getBlue(), 0.5));
        gc.fillText(timeText, centerX, centerY);

        // 绘制主文本
        gc.setFill(SECONDARY_COLOR);
        gc.fillText(timeText, centerX, centerY);
    }

    /**
     * 根据振幅获取频谱柱颜色（青色到绿色渐变）
     *
     * @param amplitude 振幅值，范围 0.0 - 1.0
     * @return 插值后的颜色
     */
    private Color getSpectrumColor(double amplitude) {
        // 青色 (0, 255, 255) -> 绿色 (10, 180, 90)
        // 使用线性插值计算中间颜色
        double r = SECONDARY_COLOR.getRed() + (PRIMARY_COLOR.getRed() - SECONDARY_COLOR.getRed()) * amplitude;
        double g = SECONDARY_COLOR.getGreen() + (PRIMARY_COLOR.getGreen() - SECONDARY_COLOR.getGreen()) * amplitude;
        double b = SECONDARY_COLOR.getBlue() + (PRIMARY_COLOR.getBlue() - SECONDARY_COLOR.getBlue()) * amplitude;

        return Color.color(
                Math.max(0.0, Math.min(1.0, r)),
                Math.max(0.0, Math.min(1.0, g)),
                Math.max(0.0, Math.min(1.0, b))
        );
    }

    @Override
    public void clear() {
        Arrays.fill(targetMagnitudes, -60f);
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

    @Override
    public void dispose() {
        stop();
        animationTimer = null;
    }
}
