package com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.waveform;

import javafx.scene.paint.Color;

/**
 * 波形画布面板 - 负责波形图形的绘制
 *
 * <p>继承自 ResizableCanvas，用于在画布上绘制音频波形。
 * 提供清除、设置波形数据、绘制波形、以及鼠标和定时位置指示等功能。</p>
 *
 * <p>主要特性:
 * <ul>
 *   <li>支持波形数据更新</li>
 *   <li>支持播放进度指示线</li>
 *   <li>支持鼠标位置跟踪</li>
 *   <li>支持已播放区域着色</li>
 * </ul>
 * </p>
 *
 * <p>作者: GOXR3PLUS STUDIO</p>
 */
public class WaveFormPane extends ResizableCanvas {

    //  颜色常量 
    /**
     * 默认背景色
     */
    private static final Color DEFAULT_BACKGROUND_COLOR = Color.web("#252525");

    /**
     * 鼠标指示线颜色（半透明白色）
     */
    private static final Color MOUSE_INDICATOR_COLOR = Color.rgb(255, 255, 255, 0.8);

    /**
     * 鼠标指示线阴影颜色
     */
    private static final Color MOUSE_INDICATOR_SHADOW_COLOR = Color.rgb(0, 0, 0, 0.3);

    /**
     * 已播放区域遮罩透明度
     */
    private static final double PLAYED_AREA_OPACITY = 0.3;

    /**
     * 进度指示线颜色
     */
    private static final Color PROGRESS_LINE_COLOR = Color.WHITE;

    /**
     * 默认波形线条颜色
     */
    private static final Color DEFAULT_WAVEFORM_COLOR = Color.DARKTURQUOISE;

    /**
     * 鼠标指示线未在画布内的标记值
     */
    private static final int MOUSE_POSITION_OUTSIDE = -1;

    /**
     * 波形数据有效性阈值
     * 如果波形最大值小于此阈值，认为是空波形（未加载音频），显示为直线
     * 推荐值：0.05
     */
    private static final float WAVEFORM_VALIDITY_THRESHOLD = 0.05f;

    /**
     * 波形放大系数 - 控制波形显示的饱满度（归一化到 0.0-1.0）
     * 0.0：显示为一条直线（无放大）
     * 0.5：波形占据约 50% 的垂直空间
     * 1.0：波形填满整个垂直区域（最大放大）
     */
    private static final double WAVEFORM_AMPLIFICATION = 0.618;

    //  画布尺寸 
    /**
     * 画布宽度（像素）
     */
    public int width;

    /**
     * 画布高度（像素）
     */
    public int height;

    //  波形数据 
    /**
     * 默认波形数据数组，用于初始或清除时显示一条基线
     */
    private float[] defaultWave;

    /**
     * 当前用于绘制的波形数据（归一化值数组，长度通常等于画布宽度）
     */
    private float[] waveData;

    //  颜色配置 
    /**
     * 背景色
     */
    private Color backgroundColor;

    /**
     * 前景色，用于波形线条
     */
    private Color foregroundColor;

    /**
     * 半透明前景色，用于绘制已播放区域的遮罩
     */
    private Color transparentForeground;

    /**
     * 鼠标指示线颜色
     */
    private Color mouseXColor = MOUSE_INDICATOR_COLOR;

    /**
     * 鼠标指示线阴影颜色
     */
    private Color mouseXShadowColor = MOUSE_INDICATOR_SHADOW_COLOR;

    //  位置指示 
    /**
     * 定时器或播放进度的 X 位置，用于绘制已播放遮罩和进度线
     */
    private int timerXPosition = 0;

    /**
     * 鼠标所在的 X 位置，用于绘制鼠标指示线；-1 表示未在画布内
     */
    private int mouseXPosition = MOUSE_POSITION_OUTSIDE;

    /**
     * 外部的 WaveVisualization 引用，用于判断动画状态等
     */
    private WaveVisualization waveVisualization;

    /**
     * 构造方法：初始化默认波形、尺寸、背景和前景色
     *
     * @param width  初始画布宽度
     * @param height 初始画布高度
     */
    public WaveFormPane(int width, int height) {
        // 初始化默认波形数组，长度等于初始宽度
        defaultWave = new float[width];
        this.width = width;
        this.height = height;
        this.setWidth(width);
        this.setHeight(height);

        // 填充极小的值，只显示细直线
        for (int i = 0; i < width; i++) {
            defaultWave[i] = 0.001f;  // 使用极小值，只显示中线
        }
        waveData = defaultWave;

        // 默认背景色和前景色
        backgroundColor = DEFAULT_BACKGROUND_COLOR;
        setForeground(DEFAULT_WAVEFORM_COLOR);  // 设置波形线条颜色为深青色
    }

    /**
     * 设置用于绘制的波形数据
     *
     * <p>外部在生成新波形数组后调用此方法，再触发重绘。</p>
     *
     * @param waveData 归一化波形数组，长度一般等于画布宽度
     */
    public void setWaveData(float[] waveData) {
        this.waveData = waveData;
    }

    /**
     * 设置前景色（波形线条颜色）
     *
     * <p>同时生成对应的半透明前景色，用于已播放区域遮罩。</p>
     *
     * @param color 前景色
     */
    public void setForeground(Color color) {
        this.foregroundColor = color;
        // 生成半透明版本，透明度为 0.3，用于绘制已播放区域
        transparentForeground = Color.rgb(
                (int) (foregroundColor.getRed() * 255),
                (int) (foregroundColor.getGreen() * 255),
                (int) (foregroundColor.getBlue() * 255),
                PLAYED_AREA_OPACITY);
    }

    /**
     * 设置背景色
     *
     * @param color 背景色
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    /**
     * 获取当前进度/定时器的 X 位置
     *
     * @return 定时器 X 坐标
     */
    public int getTimerXPosition() {
        return timerXPosition;
    }

    /**
     * 设置进度/定时器的 X 位置
     *
     * <p>用于绘制已播放遮罩和进度线。</p>
     *
     * @param timerXPosition 新的定时器 X 位置
     */
    public void setTimerXPosition(int timerXPosition) {
        this.timerXPosition = timerXPosition;
    }

    /**
     * 设置鼠标在画布上的 X 位置
     *
     * <p>传入 -1 表示鼠标移出画布，不绘制指示线。</p>
     *
     * @param mouseXPosition 鼠标 X 坐标或 -1 表示在画布外
     */
    public void setMouseXPosition(int mouseXPosition) {
        this.mouseXPosition = mouseXPosition;
    }

    /**
     * 清除波形：重置为默认波形，并绘制背景加中线基线
     *
     * <p>此方法会清空画布内容，显示一条中间线作为基线。</p>
     */
    public void clear() {
        // 重新创建默认波形数据，确保长度与当前宽度匹配
        defaultWave = new float[width];
        // 填充极小的值，只显示细直线
        for (int i = 0; i < width; i++) {
            defaultWave[i] = 0.001f;  // 使用极小值，只显示中线
        }
        // 恢复默认波形数据
        waveData = defaultWave;

        // 填充背景
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);

        // 绘制中线（基线）
        gc.setStroke(foregroundColor);
        gc.strokeLine(0, height / 2.0, width, height / 2.0);
    }

    /**
     * 绘制完整的波形画面
     *
     * <p>包括背景、波形线条、已播放遮罩、进度线、鼠标指示线等所有元素。
     * 此方法在每次动画帧时被调用。</p>
     */
    public void paintWaveForm() {
        // 1. 绘制背景整块
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);

        // 2. 绘制波形：遍历 waveData，每个像素列绘制垂直线段
        if (waveData != null) {
            drawWaveform();
        }

        // 3. 绘制已播放区域遮罩（半透明前景色）
        drawPlayedAreaMask();

        // 4. 绘制进度竖线（白色细线）
        drawProgressLine();

        // 5. 如果鼠标在画布上，绘制鼠标指示线
        drawMouseIndicatorLine();
    }

    /**
     * 绘制波形曲线
     *
     * <p>根据波形数据数组，逐像素绘制垂直线段，形成波形曲线。
     * 先对波形数据进行归一化处理（基于最大值），然后应用放大系数控制显示效果。
     * 如果波形数据无效（最大值太小），则显示为一条直线。</p>
     */
    private void drawWaveform() {
        gc.setStroke(foregroundColor);

        // 优化：如果 waveData 长度大于画布宽度，只需绘制 width 长度的数据
        int drawLength = Math.min(waveData.length, width);

        // 第一步：找到波形数据中的实际最大值
        float maxValue = 0.001f; // 避免除以零，设置最小值
        for (int i = 0; i < drawLength; i++) {
            if (waveData[i] > maxValue) {
                maxValue = waveData[i];
            }
        }

        // 判断波形数据是否有效
        // 如果最大值小于阈值，说明是空波形（未加载音频或默认波形），显示为直线
        boolean isValidWaveform = maxValue >= WAVEFORM_VALIDITY_THRESHOLD;

        // 最大可用振幅（画布高度的一半）
        double maxAmplitude = height * ((double) 3 / 5.0);

        // 第二步：绘制波形，使用归一化后的数据
        for (int i = 0; i < drawLength; i++) {
            // 若动画已停止，则执行 clear 并中断绘制
            if (waveVisualization != null && !waveVisualization.getAnimationService().isRunning()) {
                clear();
                return;
            }

            // 计算垂直线的高度
            int value;
            if (isValidWaveform) {
                // 有效波形：基于实际最大值进行归一化和放大
                // 步骤1: waveData[i] / maxValue -> 归一化到 0.0-1.0 (基于实际最大值)
                // 步骤2: * maxAmplitude -> 转换为像素高度 (最多 height/2)
                // 步骤3: * WAVEFORM_AMPLIFICATION -> 应用用户控制的放大系数 (0.0-1.0)
                value = (int) ((waveData[i] / maxValue) * maxAmplitude * WAVEFORM_AMPLIFICATION);
            } else {
                // 无效波形（空波形或默认波形）：显示为直线，只绘制中心线
                value = 0;
            }

            // 线段的中心 Y 坐标
            int centerY = height / 2;
            // 线段的起点和终点（从中心向上下延伸）
            int y1 = centerY - value;
            int y2 = centerY + value;

            // 绘制线段
            if (value > 0) {
                // 有振幅的线段
                gc.strokeLine(i, y1, i, y2);
            } else {
                // 无效波形或零值，绘制中心线上的点
                gc.strokeLine(i, centerY, i, centerY);
            }
        }
    }

    /**
     * 绘制已播放区域遮罩
     *
     * <p>在播放进度之前的区域使用半透明前景色填充，表示已播放部分。</p>
     */
    private void drawPlayedAreaMask() {
        if (timerXPosition > 0) {  // 优化：只有在有进度时才绘制遮罩
            gc.setFill(transparentForeground);
            gc.fillRect(0, 0, timerXPosition, height);
        }
    }

    /**
     * 绘制播放进度指示线
     *
     * <p>在当前播放位置绘制一条白色竖线，表示播放进度。</p>
     */
    private void drawProgressLine() {
        if (timerXPosition >= 0 && timerXPosition < width) {  // 优化：检查边界
            gc.setFill(PROGRESS_LINE_COLOR);
            gc.fillRect(timerXPosition, 0, 1, height);  // 使用 fillRect 而不是 fillOval 提高性能
        }
    }

    /**
     * 绘制鼠标指示线
     *
     * <p>在鼠标位置绘制半透明指示线，帮助用户精确定位。
     * 包含阴影效果增强视觉效果。</p>
     */
    private void drawMouseIndicatorLine() {
        if (mouseXPosition != MOUSE_POSITION_OUTSIDE && mouseXPosition >= 0 && mouseXPosition < width) {
            // 优化：增加边界检查

            // 绘制阴影效果（左侧阴影）
            gc.setFill(mouseXShadowColor);
            gc.fillRect(Math.max(0, mouseXPosition - 1), 0, 3, height);  // 边界处理

            // 绘制主指示线
            gc.setFill(mouseXColor);
            gc.fillRect(mouseXPosition, 0, 2, height);  // 使用 fillRect 提高性能
        }
    }

    /**
     * 获取关联的 WaveVisualization 实例
     *
     * <p>用于检查动画状态等。</p>
     *
     * @return WaveVisualization 实例或 null
     */
    public WaveVisualization getWaveVisualization() {
        return waveVisualization;
    }

    /**
     * 设置关联的 WaveVisualization
     *
     * <p>通常在构造或初始化时由外部注入。</p>
     *
     * @param waveVisualization 外部可视化控制对象
     */
    public void setWaveVisualization(WaveVisualization waveVisualization) {
        this.waveVisualization = waveVisualization;
    }
}
