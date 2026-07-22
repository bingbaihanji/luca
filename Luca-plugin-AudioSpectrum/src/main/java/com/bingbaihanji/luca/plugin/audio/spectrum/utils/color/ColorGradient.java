package com.bingbaihanji.luca.plugin.audio.spectrum.utils.color;

import javafx.scene.paint.Color;

/**
 * 颜色渐变工具类，用于平滑的颜色过渡
 *
 * <p>此类提供预计算的颜色渐变，以便在渲染时高效地查找颜色
 *
 * @author bingbaihanji
 */
public class ColorGradient {

    private final Color[] colors;

    /**
     * 根据给定的颜色数组创建颜色渐变
     *
     * @param colors 渐变颜色数组
     * @throws NullPointerException     如果 colors 为 null
     * @throws IllegalArgumentException 如果 colors 数组为空
     */
    public ColorGradient(Color[] colors) {
        if (colors == null) {
            throw new NullPointerException("Colors array cannot be null");
        }
        if (colors.length == 0) {
            throw new IllegalArgumentException("Colors array cannot be empty");
        }
        this.colors = colors.clone();
    }

    /**
     * 创建火焰渐变（绿色 → 黄色 → 红色）
     *
     * <p>这是一个常用于频谱可视化的 256 色渐变：
     * <ul>
     *   <li>0-127：深绿到亮绿</li>
     *   <li>128-191：绿到黄</li>
     *   <li>192-255：黄到红</li>
     * </ul>
     *
     * @return 火焰渐变
     */
    public static ColorGradient createFireGradient() {
        Color[] colors = new Color[256];

        // 0-127：深绿到亮绿
        for (int i = 0; i < 128; i++) {
            colors[i] = Color.rgb(0, (i >> 1) + 192, 0);
        }

        // 128-191：绿到黄
        for (int i = 0; i < 64; i++) {
            colors[i + 128] = Color.rgb(i << 2, 255, 0);
        }

        // 192-255：黄到红
        for (int i = 0; i < 64; i++) {
            colors[i + 192] = Color.rgb(255, 255 - (i << 2), 0);
        }

        return new ColorGradient(colors);
    }

    /**
     * 使用 HSB 颜色空间创建彩虹渐变
     *
     * @param colorCount 渐变中的颜色数量
     * @return 彩虹渐变
     */
    public static ColorGradient createRainbowGradient(int colorCount) {
        if (colorCount <= 0) {
            colorCount = 360;
        }

        Color[] colors = new Color[colorCount];
        for (int i = 0; i < colorCount; i++) {
            double hue = (double) i / colorCount * 360.0;
            colors[i] = Color.hsb(hue, 1.0, 1.0);
        }

        return new ColorGradient(colors);
    }

    /**
     * 创建蓝色渐变
     *
     * @return 蓝色渐变
     */
    public static ColorGradient createBlueGradient() {
        Color[] colors = new Color[256];
        for (int i = 0; i < 256; i++) {
            colors[i] = Color.rgb(0, i / 2, i);
        }
        return new ColorGradient(colors);
    }

    /**
     * 创建灰度渐变
     *
     * @return 灰度渐变
     */
    public static ColorGradient createGrayscaleGradient() {
        Color[] colors = new Color[256];
        for (int i = 0; i < 256; i++) {
            colors[i] = Color.rgb(i, i, i);
        }
        return new ColorGradient(colors);
    }

    /**
     * 创建热图渐变（黑色 → 紫色 → 红色 → 黄色 → 白色）
     *
     * @return 热图渐变
     */
    public static ColorGradient createHeatmapGradient() {
        return CMRMap.getGradient();
    }

    /**
     * 根据两种颜色创建自定义渐变
     *
     * @param startColor 起始颜色
     * @param endColor   结束颜色
     * @param steps      渐变步数
     * @return 自定义渐变
     */
    public static ColorGradient createGradient(Color startColor, Color endColor, int steps) {
        if (steps <= 0) {
            steps = 256;
        }

        Color[] colors = new Color[steps];
        for (int i = 0; i < steps; i++) {
            double t = (double) i / (steps - 1);
            colors[i] = startColor.interpolate(endColor, t);
        }

        return new ColorGradient(colors);
    }

    /**
     * 根据多个颜色创建多段渐变
     *
     * @param colorPoints 颜色点数组，每个元素为 {位置, 颜色}
     * @param steps       总步数
     * @return 多段渐变
     */
    public static ColorGradient createMultiGradient(ColorPoint[] colorPoints, int steps) {
        if (colorPoints == null || colorPoints.length < 2) {
            throw new IllegalArgumentException("至少需要两个颜色点");
        }
        if (steps <= 0) {
            steps = 256;
        }

        Color[] colors = new Color[steps];

        for (int i = 0; i < steps; i++) {
            double position = (double) i / (steps - 1);
            colors[i] = interpolateColor(colorPoints, position);
        }

        return new ColorGradient(colors);
    }

    /**
     * 在颜色点之间插值
     */
    private static Color interpolateColor(ColorPoint[] points, double position) {
        // 找到位置所在的区间
        int index = 0;
        while (index < points.length - 1 && position > points[index + 1].position()) {
            index++;
        }

        if (index >= points.length - 1) {
            return points[points.length - 1].color();
        }

        ColorPoint p1 = points[index];
        ColorPoint p2 = points[index + 1];
        double t = (position - p1.position()) / (p2.position() - p1.position());

        return p1.color().interpolate(p2.color(), t);
    }

    /**
     * 将颜色转换为 ARGB int (用于高性能渲染)
     *
     * @param color JavaFX Color
     * @return ARGB int
     */
    public static int colorToArgb(Color color) {
        int a = (int) (color.getOpacity() * 255);
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 获取渐变中指定位置的颜色
     *
     * <p>位置会被限制在 [0.0, 1.0] 范围内
     *
     * @param position 渐变中的位置（0.0 到 1.0）
     * @return 指定位置的颜色
     */
    public Color getColor(double position) {
        // 限制位置范围
        position = Math.max(0.0, Math.min(1.0, position));

        // 映射到颜色索引
        int index = (int) (position * (colors.length - 1));
        index = Math.min(index, colors.length - 1);

        return colors[index];
    }

    /**
     * 获取渐变中指定位置的颜色
     *
     * <p>这是为 float 类型位置提供的便捷方法
     *
     * @param position 渐变中的位置（0.0 到 1.0）
     * @return 指定位置的颜色
     */
    public Color getColor(float position) {
        return getColor((double) position);
    }

    /**
     * 获取渐变中的颜色数量
     *
     * @return 颜色数量
     */
    public int getColorCount() {
        return colors.length;
    }

    /**
     * 获取颜色数组的拷贝
     *
     * @return 颜色数组拷贝
     */
    public Color[] getColors() {
        return colors.clone();
    }
}
