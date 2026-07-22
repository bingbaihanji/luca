package com.bingbaihanji.luca.plugin.audio.spectrum.utils.color;

import javafx.scene.paint.Color;

/**
 * 颜色点类，用于多段渐变
 *
 * <p>表示渐变中的一个颜色关键点，包含位置和颜色信息
 *
 * @author bingbaihanji
 */
public record ColorPoint(double position, Color color) {

    /**
     * 创建颜色点
     *
     * @param position 位置（0.0 到 1.0）
     * @param color    颜色
     */
    public ColorPoint(double position, Color color) {
        this.position = Math.max(0.0, Math.min(1.0, position));
        this.color = color;
    }

    /**
     * 创建颜色点（使用 RGB 值）
     *
     * @param position 位置（0.0 到 1.0）
     * @param r        红色分量（0.0 到 1.0）
     * @param g        绿色分量（0.0 到 1.0）
     * @param b        蓝色分量（0.0 到 1.0）
     */
    public ColorPoint(double position, double r, double g, double b) {
        this(position, Color.color(
                Math.max(0.0, Math.min(1.0, r)),
                Math.max(0.0, Math.min(1.0, g)),
                Math.max(0.0, Math.min(1.0, b))
        ));
    }
}
