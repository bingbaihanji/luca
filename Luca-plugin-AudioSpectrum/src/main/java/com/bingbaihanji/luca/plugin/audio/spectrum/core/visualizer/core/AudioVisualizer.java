package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core;

import javafx.animation.AnimationTimer;
import javafx.beans.property.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * 所有音频可视化器的抽象基类
 *
 * <p>该类为基于 JavaFX 的音频可视化提供公共基础设施，包括：
 * <ul>
 *   <li>带有自动尺寸管理的 Canvas 渲染</li>
 *   <li>基于 AnimationTimer 的渲染循环，支持帧率控制</li>
 *   <li>基于 JavaFX 属性的配置</li>
 *   <li>线程安全的音频数据更新</li>
 * </ul>
 *
 * <p>子类必须实现 {@link #render(GraphicsContext, AudioData)} 方法来定义其可视化逻辑
 *
 * @author 音频可视化组件
 */
public abstract class AudioVisualizer extends Region {

    //  Canvas 和图形上下文

    protected final Canvas canvas;
    protected final GraphicsContext gc;

    //  动画循环

    private final AnimationTimer renderLoop;
    private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper(false);

    //  配置属性

    private final IntegerProperty targetFPS = new SimpleIntegerProperty(60);
    private final ObjectProperty<Color> backgroundColor = new SimpleObjectProperty<>(Color.BLACK);
    private final DoubleProperty backgroundOpacity = new SimpleDoubleProperty(1.0);
    //  当前音频数据
    protected volatile AudioData currentData;
    //  帧率控制
    private long lastFrameTime = 0;
    private long frameInterval; // 纳秒

    /**
     * 创建一个新的 AudioVisualizer
     */
    public AudioVisualizer() {
        // 允许父容器（StackPane 等）将 Region 撑满可用空间
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // 创建 Canvas
        canvas = new Canvas();
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        // 将 Canvas 尺寸绑定到 Region 尺寸
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // 处理尺寸变化事件
        widthProperty().addListener((obs, oldVal, newVal) -> onResize());
        heightProperty().addListener((obs, oldVal, newVal) -> onResize());

        // 当目标帧率变化时计算帧间隔
        targetFPS.addListener((obs, oldVal, newVal) -> {
            int fps = newVal.intValue();
            if (fps <= 0) {
                fps = 60;
            }
            frameInterval = 1_000_000_000L / fps;
        });
        frameInterval = 1_000_000_000L / 60; // 默认 60 FPS

        // 创建渲染循环
        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 帧率限制
                if (now - lastFrameTime < frameInterval) {
                    return;
                }
                lastFrameTime = now;

                // 获取 Canvas 尺寸
                double width = canvas.getWidth();
                double height = canvas.getHeight();

                // 如果 Canvas 未就绪则跳过
                if (width <= 0 || height <= 0) {
                    return;
                }

                // 清除 Canvas
                gc.setFill(backgroundColor.get());
                gc.setGlobalAlpha(backgroundOpacity.get());
                gc.fillRect(0, 0, width, height);
                gc.setGlobalAlpha(1.0);

                // 渲染可视化
                if (currentData != null) {
                    render(gc, currentData);
                }
            }
        };
    }

    /**
     * 更新用于可视化的音频数据（线程安全）
     *
     * <p>此方法可以从任何线程调用数据将在下一个渲染周期中被处理
     *
     * @param leftChannel  左声道样本
     * @param rightChannel 右声道样本
     * @throws NullPointerException 如果任一通道为 null
     */
    public void updateAudioData(float[] leftChannel, float[] rightChannel) {
        this.currentData = new AudioData(leftChannel, rightChannel);
    }

    /**
     * 启动可视化器的渲染循环
     *
     * <p>此方法是幂等的——如果可视化器已经在运行，多次调用无效
     */
    public void start() {
        if (!running.get()) {
            renderLoop.start();
            running.set(true);
        }
    }

    /**
     * 停止可视化器的渲染循环
     *
     * <p>此方法是幂等的——如果可视化器已经停止，多次调用无效
     */
    public void stop() {
        if (running.get()) {
            renderLoop.stop();
            running.set(false);
        }
    }

    /**
     * 渲染可视化内容
     *
     * <p>此方法由渲染循环在每一帧调用子类必须在此实现其可视化逻辑
     *
     * @param gc   用于绘制的图形上下文
     * @param data 当前音频数据
     */
    protected abstract void render(GraphicsContext gc, AudioData data);

    /**
     * 当 Canvas 尺寸发生变化时调用
     *
     * <p>子类可以重写此方法，以在 Canvas 大小调整时执行必要的重新计算或更新
     * 默认实现不执行任何操作
     */
    protected void onResize() {
        // 默认实现：什么都不做
    }

    //  属性访问器

    /**
     * 目标帧率属性
     *
     * @return 目标帧率属性
     */
    public IntegerProperty targetFPSProperty() {
        return targetFPS;
    }

    /**
     * 获取目标帧率
     *
     * @return 目标帧率值
     */
    public int getTargetFPS() {
        return targetFPS.get();
    }

    /**
     * 设置目标帧率
     *
     * @param fps 目标帧率（必须为正数）
     */
    public void setTargetFPS(int fps) {
        if (fps > 0) {
            targetFPS.set(fps);
        }
    }

    /**
     * 背景颜色属性
     *
     * @return 背景颜色属性
     */
    public ObjectProperty<Color> backgroundColorProperty() {
        return backgroundColor;
    }

    /**
     * 获取背景颜色
     *
     * @return 背景颜色
     */
    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    /**
     * 设置背景颜色
     *
     * @param color 背景颜色
     */
    public void setBackgroundColor(Color color) {
        if (color != null) {
            backgroundColor.set(color);
        }
    }

    /**
     * 背景透明度属性
     *
     * @return 背景透明度属性
     */
    public DoubleProperty backgroundOpacityProperty() {
        return backgroundOpacity;
    }

    /**
     * 获取背景透明度
     *
     * @return 背景透明度（0.0 到 1.0）
     */
    public double getBackgroundOpacity() {
        return backgroundOpacity.get();
    }

    /**
     * 设置背景透明度
     *
     * @param opacity 背景透明度（0.0 到 1.0）
     */
    public void setBackgroundOpacity(double opacity) {
        backgroundOpacity.set(Math.max(0.0, Math.min(1.0, opacity)));
    }

    /**
     * 运行状态属性（只读）
     *
     * @return 运行状态属性
     */
    public ReadOnlyBooleanProperty runningProperty() {
        return running.getReadOnlyProperty();
    }

    /**
     * 检查可视化器当前是否正在运行
     *
     * @return 如果正在运行则返回 true，否则返回 false
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取 Canvas 宽度
     *
     * @return Canvas 宽度（像素）
     */
    protected double getCanvasWidth() {
        return canvas.getWidth();
    }

    /**
     * 获取 Canvas 高度
     *
     * @return Canvas 高度（像素）
     */
    protected double getCanvasHeight() {
        return canvas.getHeight();
    }
}