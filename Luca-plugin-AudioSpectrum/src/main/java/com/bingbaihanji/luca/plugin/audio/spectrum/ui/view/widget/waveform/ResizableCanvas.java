package com.bingbaihanji.luca.plugin.audio.spectrum.ui.view.widget.waveform;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 可调整大小的 Canvas，随父容器尺寸变化而自动调整
 *
 * <p>通过覆盖 Canvas 的尺寸相关方法，使其在布局时能扩展到可用区域。
 * JavaFX 的默认 Canvas 不支持自动调整大小，本类通过实现 isResizable()
 * 和 resize() 方法使 Canvas 能够响应父容器的大小变化。</p>
 *
 * <p>作者: GOXR3PLUS</p>
 */
public class ResizableCanvas extends Canvas {

    /**
     * 图形上下文，用于在画布上进行绘制操作
     * 通过此对象可以绘制线条、矩形、文本等图形元素
     */
    public final GraphicsContext gc = getGraphicsContext2D();

    /**
     * 重绘画布示例方法：清除内容并绘制两条对角线
     *
     * <p>这是一个演示方法，展示如何使用 GraphicsContext 进行绘制。
     * 子类应该根据实际需求覆盖此方法，实现自定义的绘制逻辑。</p>
     */
    public void redraw() {
        // 设置线条宽度
        gc.setLineWidth(3);
        // 清空整个画布区域
        gc.clearRect(0, 0, getWidth(), getHeight());

        // 绘制红色对角线作为示例
        gc.setStroke(Color.RED);
        gc.strokeLine(0, 0, getWidth(), getHeight());
        gc.strokeLine(0, getHeight(), getWidth(), 0);
    }

    /**
     * 返回最小高度
     *
     * <p>此处设为 1，表示几乎无最小限制，允许画布缩小到极小尺寸</p>
     *
     * @param width 参考宽度（本实现中未使用）
     * @return 最小高度值
     */
    @Override
    public double minHeight(double width) {
        return 1;
    }

    /**
     * 返回最小宽度
     *
     * <p>此处设为 1，允许画布在水平方向缩小到极小尺寸</p>
     *
     * @param height 参考高度（本实现中未使用）
     * @return 最小宽度值
     */
    @Override
    public double minWidth(double height) {
        return 1;
    }

    /**
     * 返回首选宽度
     *
     * <p>返回最小宽度，表示画布宽度完全由父容器决定</p>
     *
     * @param width 参考宽度
     * @return 首选宽度值
     */
    @Override
    public double prefWidth(double width) {
        return minWidth(width);
    }

    /**
     * 返回首选高度
     *
     * <p>返回最小高度，表示画布高度完全由父容器决定</p>
     *
     * @param width 参考宽度
     * @return 首选高度值
     */
    @Override
    public double prefHeight(double width) {
        return minHeight(width);
    }

    /**
     * 返回最大宽度
     *
     * <p>返回 Double.MAX_VALUE，表示画布可以扩展到任意大小</p>
     *
     * @param height 参考高度（本实现中未使用）
     * @return 最大宽度值
     */
    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    /**
     * 返回最大高度
     *
     * <p>返回 Double.MAX_VALUE，表示画布在垂直方向可以扩展到任意大小</p>
     *
     * @param width 参考宽度（本实现中未使用）
     * @return 最大高度值
     */
    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }

    /**
     * 指示此 Canvas 是否可调整大小
     *
     * <p>返回 true 使布局管理器在布局时会调用 resize() 方法，
     * 从而实现 Canvas 随容器大小自动调整</p>
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isResizable() {
        return true;
    }

    /**
     * 当布局管理器调整画布尺寸时调用
     *
     * <p>此方法由 JavaFX 布局系统自动调用，设置 Canvas 的新宽度和高度。
     * 子类可以在尺寸变化后调用 redraw() 或其他绘制方法来重新绘制内容。</p>
     *
     * @param width  新的宽度
     * @param height 新的高度
     */
    @Override
    public void resize(double width, double height) {
        // 设置新的宽度和高度
        super.setWidth(width);
        super.setHeight(height);

        // 注意：如需自动重绘，可以在此调用 redraw() 或其他绘制方法
        // 例如：redraw();
    }
}
