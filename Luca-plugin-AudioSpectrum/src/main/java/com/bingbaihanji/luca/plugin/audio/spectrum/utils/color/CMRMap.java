package com.bingbaihanji.luca.plugin.audio.spectrum.utils.color;

import javafx.scene.paint.Color;

/**
 * CMRMap 配色方案
 *
 * <p>参考自 Python 版 Friture 的 generated_cmrmap.py
 * 配色: 黑色 -> 紫色 -> 红色 -> 黄色 -> 白色
 * 设计用于单色兼容，产生单调灰度色阶
 *
 * @author bingbaihanji
 */
public class CMRMap {

    private static final Object LOCK = new Object();
    // 懒加载的渐变实例
    private static volatile ColorGradient GRADIENT;

    private CMRMap() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    /**
     * 获取 CMRMap ColorGradient 实例（懒加载）
     *
     * @return ColorGradient 实例
     */
    public static ColorGradient getGradient() {
        if (GRADIENT == null) {
            synchronized (LOCK) {
                if (GRADIENT == null) {
                    GRADIENT = createCMRGradient();
                }
            }
        }
        return GRADIENT;
    }

    /**
     * 创建 CMRMap 渐变
     */
    private static ColorGradient createCMRGradient() {
        // 关键颜色点定义
        double[][] keyColors = {
                {0.000, 0.020, 0.020, 0.100},  // 深蓝紫（提亮）
                {0.078, 0.080, 0.070, 0.250},  // 蓝紫（提亮）
                {0.157, 0.150, 0.080, 0.350},  // 紫色（提亮）
                {0.235, 0.250, 0.050, 0.380},  // 深红紫（提亮）
                {0.314, 0.380, 0.030, 0.300},  // 深红
                {0.392, 0.520, 0.040, 0.180},  // 红色
                {0.471, 0.670, 0.060, 0.060},  // 亮红
                {0.549, 0.810, 0.110, 0.035},  // 橙红
                {0.627, 0.910, 0.210, 0.025},  // 橙色
                {0.706, 0.960, 0.420, 0.025},  // 黄橙
                {0.784, 0.975, 0.620, 0.025},  // 黄色
                {0.863, 0.985, 0.810, 0.420},  // 浅黄
                {0.941, 0.992, 0.910, 0.720},  // 浅白
                {1.000, 1.000, 1.000, 1.000}   // 白色
        };

        ColorPoint[] points = new ColorPoint[keyColors.length];
        for (int i = 0; i < keyColors.length; i++) {
            double[] kc = keyColors[i];
            points[i] = new ColorPoint(kc[0], kc[1], kc[2], kc[3]);
        }
        return ColorGradient.createMultiGradient(points, 256);
    }

    /**
     * 将 [0, 1] 范围的值映射到 CMRMap 颜色
     *
     * @param value 归一化值 [0, 1]
     * @return Color 对象
     */
    public static Color getColor(double value) {
        return getGradient().getColor(value);
    }

    /**
     * 预生成 256 色查找表
     *
     * @return Color 数组
     */
    public static Color[] generateLookupTable() {
        return getGradient().getColors();
    }

    /**
     * 将颜色转换为 ARGB int (用于高性能渲染)
     *
     * @param color JavaFX Color
     * @return ARGB int
     */
    public static int colorToArgb(Color color) {
        return ColorGradient.colorToArgb(color);
    }

    /**
     * 预生成 ARGB 查找表
     *
     * @return ARGB int 数组
     */
    public static int[] generateArgbLookupTable() {
        Color[] colors = generateLookupTable();
        int[] table = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            table[i] = colorToArgb(colors[i]);
        }
        return table;
    }
}
