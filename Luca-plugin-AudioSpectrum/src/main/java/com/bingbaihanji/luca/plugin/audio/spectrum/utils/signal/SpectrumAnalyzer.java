package com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal;


import com.bingbaihanji.luca.plugin.audio.spectrum.utils.math.fft.CooleyTukeyFFT;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.math.fft.FFTProcessor;

/**
 * 频谱分析器，将音频样本转换为频带
 *
 * <p>该分析器执行以下操作：
 * <ol>
 *   <li>FFT 计算，将时域转换为频域</li>
 *   <li>频带分组，将频率箱减少到所需的频带数量</li>
 *   <li>对数滤波，模拟人耳听觉响应</li>
 *   <li>衰减处理，实现平滑动画</li>
 *   <li>A-weighting 滤波（可选）</li>
 * </ol>
 *
 * @author bingbaihanji
 */
public class SpectrumAnalyzer {

    private final FFTProcessor fft;
    private final int bandCount;
    private final float decay;
    private final float multiplier;
    private final double sampleRate;

    // 衰减状态（前一帧的值）
    private final float[] oldValues;

    // A-weighting 缓存
    private final float[] aWeightingCache;
    private boolean useAWeighting = false;

    /**
     * 创建一个新的频谱分析器
     *
     * @param fftSize    FFT 样本大小（必须是 2 的幂）
     * @param bandCount  输出的频带数量
     * @param decay      平滑衰减率（0.0-1.0）
     * @param sampleRate 采样率（Hz）
     */
    public SpectrumAnalyzer(int fftSize, int bandCount, float decay, double sampleRate) {
        this.fft = new CooleyTukeyFFT(fftSize);
        this.bandCount = bandCount;
        this.decay = Math.max(0.0f, Math.min(1.0f, decay));
        this.multiplier = (float) (fft.getFrequencyBinCount() / (double) bandCount);
        this.oldValues = new float[fft.getFrequencyBinCount()];
        this.sampleRate = sampleRate;
        this.aWeightingCache = new float[fft.getFrequencyBinCount()];
        initializeAWeightingCache();
    }

    /**
     * 创建一个新的频谱分析器（默认采样率 48000Hz）
     *
     * @param fftSize   FFT 样本大小（必须是 2 的幂）
     * @param bandCount 输出的频带数量
     * @param decay     平滑衰减率（0.0-1.0）
     */
    public SpectrumAnalyzer(int fftSize, int bandCount, float decay) {
        this(fftSize, bandCount, decay, 48000.0);
    }

    /**
     * 初始化 A-weighting 缓存
     */
    private void initializeAWeightingCache() {
        double[] freqScale = getFreqScale();
        for (int i = 0; i < aWeightingCache.length; i++) {
            double freq = freqScale[i];
            if (freq > 0) {
                aWeightingCache[i] = (float) Math.pow(10, aWeighting(freq) / 20.0);
            } else {
                aWeightingCache[i] = 1.0f;
            }
        }
    }

    /**
     * 分析音频样本并返回频带强度
     *
     * <p>返回的数组包含归一化值（0.0-1.0），表示每个频带的强度
     *
     * @param samples 要分析的音频样本
     * @return 频带强度数组（0.0-1.0）
     */
    public float[] analyze(float[] samples) {
        return analyze(samples, 1.0f);
    }

    /**
     * 分析音频样本并返回频带强度（double 版本）
     *
     * @param samples 要分析的音频样本
     * @return 频带强度数组（0.0-1.0）
     */
    public float[] analyze(double[] samples) {
        float[] floatSamples = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            floatSamples[i] = (float) samples[i];
        }
        return analyze(floatSamples, 1.0f);
    }

    /**
     * 分析音频样本，并进行帧率补偿
     *
     * <p>帧率比例提示用于在实际帧率与目标帧率不同时调整衰减率
     *
     * @param samples            要分析的音频样本
     * @param frameRateRatioHint 当前 FPS 与目标 FPS 的比率
     * @return 频带强度数组（0.0-1.0）
     */
    public float[] analyze(float[] samples, float frameRateRatioHint) {
        // 执行 FFT
        float[] fftData = fft.calculate(samples);

        // 应用 A-weighting（如果启用）
        if (useAWeighting) {
            for (int i = 0; i < fftData.length; i++) {
                fftData[i] *= aWeightingCache[i];
            }
        }

        // 准备输出数组
        float[] bands = new float[bandCount];

        // 根据帧率调整衰减
        float adjustedDecay = decay * Math.abs(frameRateRatioHint);

        // 处理每个频带
        for (int band = 0; band < bandCount; band++) {
            int startBin = (int) (band * multiplier);
            float value = 0;

            // 平均相邻的频率箱
            int binCount = (int) Math.ceil(multiplier);
            for (int i = 0; i < binCount && (startBin + i) < fftData.length; i++) {
                value += fftData[startBin + i];
            }
            value /= binCount;

            // 应用对数滤波（模拟人耳听觉）
            value = (float) (value * Math.log(band + 2.0));
            value = Math.min(value, 1.0f);

            // 应用衰减实现平滑动画
            if (value >= (oldValues[startBin] - adjustedDecay)) {
                // 值增加或缓慢减少
                oldValues[startBin] = value;
            } else {
                // 值快速减少 - 应用衰减
                oldValues[startBin] -= adjustedDecay;
                if (oldValues[startBin] < 0) {
                    oldValues[startBin] = 0;
                }
                value = oldValues[startBin];
            }

            bands[band] = value;
        }

        return bands;
    }

    /**
     * 获取频带数量
     *
     * @return 频带数量
     */
    public int getBandCount() {
        return bandCount;
    }

    /**
     * 获取 FFT 样本大小
     *
     * @return FFT 样本大小
     */
    public int getFFTSize() {
        return fft.getSampleSize();
    }

    /**
     * 获取衰减率
     *
     * @return 衰减率（0.0-1.0）
     */
    public float getDecay() {
        return decay;
    }

    /**
     * 生成频率刻度
     *
     * @return 频率数组 (Hz)，长度 = fftSize/2 + 1
     */
    public double[] getFreqScale() {
        int size = fft.getFrequencyBinCount();
        double[] freqScale = new double[size];
        double factor = sampleRate / fft.getSampleSize();
        for (int i = 0; i < size; i++) {
            freqScale[i] = i * factor;
        }
        return freqScale;
    }

    /**
     * 查找频谱峰值
     *
     * @param spectrum 频谱数组
     * @param minFreq  最小频率 (Hz)
     * @param maxFreq  最大频率 (Hz)
     * @return 峰值频率 (Hz)
     */
    public double findPeakFrequency(float[] spectrum, double minFreq, double maxFreq) {
        double[] freqScale = getFreqScale();

        int peakIndex = 0;
        float peakValue = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < spectrum.length; i++) {
            double freq = freqScale[i];
            if (freq >= minFreq && freq <= maxFreq && spectrum[i] > peakValue) {
                peakValue = spectrum[i];
                peakIndex = i;
            }
        }

        return freqScale[peakIndex];
    }

    /**
     * 查找频谱峰值（默认频率范围）
     *
     * @param spectrum 频谱数组
     * @return 峰值频率 (Hz)
     */
    public double findPeakFrequency(float[] spectrum) {
        return findPeakFrequency(spectrum, 20.0, 20000.0);
    }

    /**
     * A-weighting frequency response
     * 用于模拟人耳对不同频率的敏感度
     *
     * @param freq 频率 (Hz)
     * @return A-weighting 系数 (dB)
     */
    public double aWeighting(double freq) {
        double f2 = freq * freq;
        double num = 12194.0 * 12194.0 * f2 * f2;
        double den = (f2 + 20.6 * 20.6) *
                Math.sqrt((f2 + 107.7 * 107.7) * (f2 + 737.9 * 737.9)) *
                (f2 + 12194.0 * 12194.0);
        return 20.0 * Math.log10(num / den) + 2.0;  // +2dB 补偿
    }

    /**
     * 检查是否使用 A-weighting
     */
    public boolean isUseAWeighting() {
        return useAWeighting;
    }

    /**
     * 启用/禁用 A-weighting
     */
    public void setUseAWeighting(boolean use) {
        this.useAWeighting = use;
    }

    /**
     * 重置衰减状态
     *
     * <p>这会清除所有前一帧的值，使下一次分析从头开始，不进行平滑处理
     */
    public void reset() {
        for (int i = 0; i < oldValues.length; i++) {
            oldValues[i] = 0;
        }
    }
}
