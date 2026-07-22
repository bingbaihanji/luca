package com.bingbaihanji.luca.plugin.audio.spectrum.utils.math.fft;

/**
 * 快速傅里叶变换处理器的接口
 *
 * <p>该接口的实现类对音频样本执行 FFT 计算，生成用于频谱分析的频域数据
 *
 * @author bingbaihanji
 */
public interface FFTProcessor {

    /**
     * 对给定的样本执行 FFT 计算
     *
     * <p>输入样本通常是时域音频数据，输出是频域数据（幅度谱）
     *
     * @param samples 输入的音频样本
     * @return 频谱幅度
     */
    float[] calculate(float[] samples);

    /**
     * 对给定的样本执行 FFT 计算（double 版本）
     *
     * @param samples 输入的音频样本
     * @return 频谱幅度
     */
    double[] calculate(double[] samples);

    /**
     * 获取 FFT 样本大小
     *
     * <p>通常为 2 的幂（例如 512、1024、2048）
     *
     * @return FFT 样本大小
     */
    int getSampleSize();

    /**
     * 获取输出中的频率箱数量
     *
     * <p>通常为样本大小的一半
     *
     * @return 频率箱数量
     */
    default int getFrequencyBinCount() {
        return getSampleSize() / 2;
    }
}
