package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Widget View 基类
 * <p>
 * 提供 Canvas 绘制功能，优化版本使用脏标记
 */
public abstract class WidgetView extends Region {

    protected final Canvas canvas = new Canvas();
    protected final GraphicsContext gc = canvas.getGraphicsContext2D();

    // 脏标记：标识是否需要重绘
    protected volatile boolean isDirty = true;

    public WidgetView() {
        getChildren().add(canvas);

        // Canvas 大小绑定到 Region 大小
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // 监听大小变化，重新渲染
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            isDirty = true;
            render();
        });
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            isDirty = true;
            render();
        });
    }

    /**
     * 渲染函数，由子类实现
     * 只在 isDirty = true 时才真正渲染
     */
    public abstract void render();

    /**
     * 标记为脏，下次渲染时会重绘
     */
    public void markDirty() {
        isDirty = true;
    }

    /**
     * 清空 Canvas
     */
    protected void clearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * 绘制网格
     */
    protected void drawGrid(int horizontalLines, int verticalLines, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(0.5);

        // 水平线
        for (int i = 1; i < horizontalLines; i++) {
            double y = i * canvas.getHeight() / horizontalLines;
            gc.strokeLine(0, y, canvas.getWidth(), y);
        }

        // 垂直线
        for (int i = 1; i < verticalLines; i++) {
            double x = i * canvas.getWidth() / verticalLines;
            gc.strokeLine(x, 0, x, canvas.getHeight());
        }

        // 中心十字线
        gc.setLineWidth(1.0);
        gc.strokeLine(0, canvas.getHeight() / 2, canvas.getWidth(), canvas.getHeight() / 2);
        gc.strokeLine(canvas.getWidth() / 2, 0, canvas.getWidth() / 2, canvas.getHeight());
    }

    /**
     * 使用默认参数绘制网格
     */
    protected void drawGrid() {
        drawGrid(5, 5, Color.DARKGRAY);
    }

    protected boolean isDirty() {
        return isDirty;
    }

    protected void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    protected double getCanvasWidth() {
        return canvas.getWidth();
    }

    protected double getCanvasHeight() {
        return canvas.getHeight();
    }
}
