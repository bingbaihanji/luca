package com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal;

/**
 * 信号处理工具类
 *
 * <p>提供各种信号处理算法，包括平滑滤波、归一化等
 *
 * @author bingbaihanji
 */
public final class SignalProcessing {

    private SignalProcessing() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    /**
     * 指数平滑滤波
     *
     * @param newValue 新值
     * @param oldValue 旧值
     * @param alpha    平滑系数 (0-1)，越大响应越快
     * @return 平滑后的值
     */
    public static double exponentialSmooth(double newValue, double oldValue, double alpha) {
        alpha = Math.max(0.0, Math.min(1.0, alpha));
        return alpha * newValue + (1.0 - alpha) * oldValue;
    }

    /**
     * 指数平滑滤波（float 版本）
     *
     * @param newValue 新值
     * @param oldValue 旧值
     * @param alpha    平滑系数 (0-1)
     * @return 平滑后的值
     */
    public static float exponentialSmooth(float newValue, float oldValue, float alpha) {
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        return alpha * newValue + (1.0f - alpha) * oldValue;
    }

    /**
     * 对数组进行指数平滑滤波
     *
     * @param newValues 新值数组
     * @param oldValues 旧值数组（会被修改）
     * @param alpha     平滑系数
     * @return 平滑后的数组（即修改后的 oldValues）
     */
    public static double[] exponentialSmoothArray(double[] newValues, double[] oldValues, double alpha) {
        if (newValues.length != oldValues.length) {
            throw new IllegalArgumentException("数组长度必须相同");
        }
        for (int i = 0; i < newValues.length; i++) {
            oldValues[i] = exponentialSmooth(newValues[i], oldValues[i], alpha);
        }
        return oldValues;
    }

    /**
     * 对数组进行指数平滑滤波（float 版本）
     */
    public static float[] exponentialSmoothArray(float[] newValues, float[] oldValues, float alpha) {
        if (newValues.length != oldValues.length) {
            throw new IllegalArgumentException("数组长度必须相同");
        }
        for (int i = 0; i < newValues.length; i++) {
            oldValues[i] = exponentialSmooth(newValues[i], oldValues[i], alpha);
        }
        return oldValues;
    }

    /**
     * 将值归一化到 [0, 1] 范围
     *
     * @param value 原始值
     * @param min   最小值
     * @param max   最大值
     * @return 归一化后的值
     */
    public static double normalize(double value, double min, double max) {
        if (max == min) {
            return 0;
        }
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    /**
     * 将值归一化到 [0, 1] 范围（float 版本）
     */
    public static float normalize(float value, float min, float max) {
        if (max == min) {
            return 0;
        }
        return Math.max(0.0f, Math.min(1.0f, (value - min) / (max - min)));
    }

    /**
     * 将值从 [0, 1] 范围映射到 [targetMin, targetMax]
     *
     * @param value     [0, 1] 范围内的值
     * @param targetMin 目标最小值
     * @param targetMax 目标最大值
     * @return 映射后的值
     */
    public static double map(double value, double targetMin, double targetMax) {
        return targetMin + value * (targetMax - targetMin);
    }

    /**
     * 将值从 [0, 1] 范围映射到 [targetMin, targetMax]（float 版本）
     */
    public static float map(float value, float targetMin, float targetMax) {
        return targetMin + value * (targetMax - targetMin);
    }

    /**
     * 计算 dB 值
     *
     * @param linear 线性值
     * @return dB 值
     */
    public static double toDecibels(double linear) {
        return 10.0 * Math.log10(linear + 1e-30);  // 加小常数避免 log(0)
    }

    /**
     * 计算 dB 值（float 版本）
     */
    public static float toDecibels(float linear) {
        return (float) (10.0 * Math.log10(linear + 1e-30f));
    }

    /**
     * 将 dB 值转回线性值
     *
     * @param db dB 值
     * @return 线性值
     */
    public static double fromDecibels(double db) {
        return Math.pow(10.0, db / 10.0);
    }

    /**
     * 将 dB 值转回线性值（float 版本）
     */
    public static float fromDecibels(float db) {
        return (float) Math.pow(10.0, db / 10.0);
    }

    /**
     * 对数组应用对数压缩（模拟人耳听觉）
     *
     * @param values 输入数组
     * @return 压缩后的数组
     */
    public static float[] logCompress(float[] values) {
        float[] result = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            // 对数压缩 + 限制范围
            result[i] = Math.min(1.0f, (float) (values[i] * Math.log(i + 2.0)));
        }
        return result;
    }

    /**
     * 计算数组的平均值
     */
    public static double mean(double[] values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    /**
     * 计算数组的平均值（float 版本）
     */
    public static float mean(float[] values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        float sum = 0;
        for (float v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    /**
     * 查找数组中的最大值和索引
     *
     * @return [最大值, 索引]
     */
    public static double[] findMax(double[] values) {
        if (values == null || values.length == 0) {
            return new double[]{0, -1};
        }
        double max = values[0];
        int index = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
                index = i;
            }
        }
        return new double[]{max, index};
    }

    /**
     * 查找数组中的最大值和索引（float 版本）
     */
    public static float[] findMax(float[] values) {
        if (values == null || values.length == 0) {
            return new float[]{0, -1};
        }
        float max = values[0];
        int index = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
                index = i;
            }
        }
        return new float[]{max, index};
    }

    /**
     * 将功率谱转换为 dB 数组
     *
     * @param spectrum 功率谱
     * @return dB 数组
     */
    public static double[] spectrumToDecibels(double[] spectrum) {
        double[] db = new double[spectrum.length];
        for (int i = 0; i < spectrum.length; i++) {
            db[i] = toDecibels(spectrum[i]);
        }
        return db;
    }

    /**
     * 将功率谱转换为 dB 数组（float 版本）
     */
    public static float[] spectrumToDecibels(float[] spectrum) {
        float[] db = new float[spectrum.length];
        for (int i = 0; i < spectrum.length; i++) {
            db[i] = toDecibels(spectrum[i]);
        }
        return db;
    }
}
