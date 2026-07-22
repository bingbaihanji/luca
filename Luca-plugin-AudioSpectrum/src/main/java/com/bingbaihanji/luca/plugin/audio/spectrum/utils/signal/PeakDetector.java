package com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal;

/**
 * 峰值检测器，用于在频谱可视化中跟踪和显示峰值
 *
 * <p>此类为每个频带维护峰值，并提供平滑的下降动画
 *
 * @author bingbaihanji
 */
public class PeakDetector {

    private final float[] peaks;
    private final int[] peakDelays;
    private final int peakHoldFrames;
    private final float fallSpeed;

    /**
     * 创建一个新的峰值检测器
     *
     * @param bandCount      要跟踪的频带数量
     * @param peakHoldFrames 峰值在下降前保持的帧数
     * @param fallSpeed      每帧的下降速度（单位）
     */
    public PeakDetector(int bandCount, int peakHoldFrames, float fallSpeed) {
        this.peaks = new float[bandCount];
        this.peakDelays = new int[bandCount];
        this.peakHoldFrames = Math.max(1, peakHoldFrames);
        this.fallSpeed = Math.max(0, fallSpeed);
    }

    /**
     * 使用默认设置创建一个新的峰值检测器
     *
     * @param bandCount 要跟踪的频带数量
     */
    public PeakDetector(int bandCount) {
        this(bandCount, 20, 2.0f);
    }

    /**
     * 更新并返回指定频带的峰值
     *
     * <p>如果当前值超过峰值，则更新峰值并保持否则，峰值根据下降速度逐渐下降
     *
     * @param band         频带索引
     * @param currentValue 当前频带的值
     * @return 峰值
     */
    public float updateAndGetPeak(int band, float currentValue) {
        if (band < 0 || band >= peaks.length) {
            return 0;
        }

        if (currentValue > peaks[band]) {
            // 达到新的峰值
            peaks[band] = currentValue;
            peakDelays[band] = peakHoldFrames;
        } else {
            // 减少延迟计数器
            peakDelays[band] -= 1;

            // 保持期结束后开始下降
            if (peakDelays[band] <= 0) {
                peaks[band] -= fallSpeed;
                if (peaks[band] < 0) {
                    peaks[band] = 0;
                }
            }
        }

        return peaks[band];
    }

    /**
     * 批量更新所有频带的峰值
     *
     * @param currentValues 当前频带值数组
     * @return 更新后的峰值数组
     */
    public float[] updateAndGetAllPeaks(float[] currentValues) {
        float[] result = new float[peaks.length];
        int count = Math.min(currentValues.length, peaks.length);
        for (int i = 0; i < count; i++) {
            result[i] = updateAndGetPeak(i, currentValues[i]);
        }
        return result;
    }

    /**
     * 获取指定频带的当前峰值（不更新）
     *
     * @param band 频带索引
     * @return 峰值
     */
    public float getPeak(int band) {
        if (band < 0 || band >= peaks.length) {
            return 0;
        }
        return peaks[band];
    }

    /**
     * 获取所有峰值（不更新）
     *
     * @return 峰值数组的拷贝
     */
    public float[] getAllPeaks() {
        return peaks.clone();
    }

    /**
     * 将所有峰值重置为零
     */
    public void reset() {
        for (int i = 0; i < peaks.length; i++) {
            peaks[i] = 0;
            peakDelays[i] = 0;
        }
    }

    /**
     * 重置指定频带的峰值
     *
     * @param band 频带索引
     */
    public void resetBand(int band) {
        if (band >= 0 && band < peaks.length) {
            peaks[band] = 0;
            peakDelays[band] = 0;
        }
    }

    /**
     * 获取被跟踪的频带数量
     *
     * @return 频带数量
     */
    public int getBandCount() {
        return peaks.length;
    }

    /**
     * 获取峰值保持的帧数
     *
     * @return 峰值保持帧数
     */
    public int getPeakHoldFrames() {
        return peakHoldFrames;
    }

    /**
     * 获取下降速度
     *
     * @return 每帧的下降速度（单位）
     */
    public float getFallSpeed() {
        return fallSpeed;
    }
}
