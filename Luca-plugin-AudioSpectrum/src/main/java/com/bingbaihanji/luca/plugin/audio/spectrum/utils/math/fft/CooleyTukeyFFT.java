package com.bingbaihanji.luca.plugin.audio.spectrum.utils.math.fft;

/**
 * 使用 Cooley-Tukey 算法的快速傅里叶变换实现
 *
 * <p>该实现通过以下方式优化性能：
 * <ul>
 *   <li>预计算的正弦和余弦表</li>
 *   <li>缓存的位反转表</li>
 *   <li>可重用的工作数组</li>
 *   <li>支持 Hann 窗函数</li>
 * </ul>
 *
 * <p>样本大小必须是 2 的幂
 *
 * <p>基于 Kris Fudalewski 的 KJFFT 实现，并参考 capture 包的 AudioProcessor 优化
 *
 * @author bingbaihanji
 */
public class CooleyTukeyFFT implements FFTProcessor {

    private final int sampleSize;
    private final int halfSize;
    private final int numStages;

    // 工作数组（重用以避免分配）
    private final float[] xre;
    private final float[] xim;
    private final float[] mag;

    // 预计算表
    private final float[] fftSin;
    private final float[] fftCos;
    private final int[] fftBr;

    // Hann 窗函数
    private final float[] window;
    private boolean useWindow = true;

    /**
     * 创建一个新的 CooleyTukeyFFT 处理器
     *
     * @param sampleSize FFT 样本大小（必须是 2 的幂）
     * @throws IllegalArgumentException 如果样本大小不是 2 的幂
     */
    public CooleyTukeyFFT(int sampleSize) {
        this(sampleSize, true);
    }

    /**
     * 创建一个新的 CooleyTukeyFFT 处理器
     *
     * @param sampleSize FFT 样本大小（必须是 2 的幂）
     * @param useWindow  是否使用 Hann 窗函数
     * @throws IllegalArgumentException 如果样本大小不是 2 的幂
     */
    public CooleyTukeyFFT(int sampleSize, boolean useWindow) {
        // 验证是否为 2 的幂
        if (sampleSize <= 0 || (sampleSize & (sampleSize - 1)) != 0) {
            throw new IllegalArgumentException("Sample size must be a power of 2, got: " + sampleSize);
        }

        this.sampleSize = sampleSize;
        this.halfSize = sampleSize >> 1;
        this.numStages = (int) (Math.log(sampleSize) / Math.log(2));
        this.useWindow = useWindow;

        // 分配工作数组
        this.xre = new float[sampleSize];
        this.xim = new float[sampleSize];
        this.mag = new float[halfSize + 1]; // +1 for Nyquist frequency

        // 预计算 FFT 表
        int tableSize = numStages * halfSize;
        this.fftSin = new float[tableSize];
        this.fftCos = new float[tableSize];
        this.fftBr = new int[sampleSize];

        // 初始化 Hann 窗
        this.window = new float[sampleSize];
        initializeWindow();

        prepareFFTTables();
    }

    /**
     * 初始化 Hann 窗函数
     */
    private void initializeWindow() {
        for (int i = 0; i < sampleSize; i++) {
            window[i] = (float) (0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (sampleSize - 1))));
        }
    }

    @Override
    public float[] calculate(float[] samples) {
        int n2 = halfSize;

        // 如果需要，对输入进行降采样，并应用窗函数
        int samplesPerFFT = samples.length / sampleSize;
        for (int a = 0, b = 0; a < samples.length && b < sampleSize; a += samplesPerFFT, b++) {
            float sample = samples[a];
            if (useWindow) {
                sample *= window[b];
            }
            xre[b] = sample;
            xim[b] = 0.0f;
        }

        // FFT 蝶形运算
        float tr, ti, c, s;
        int k, kn2, x = 0;

        for (int l = 1; l <= numStages; l++) {
            k = 0;

            while (k < sampleSize) {
                for (int i = 1; i <= n2; i++) {
                    // 使用预计算的正弦/余弦
                    c = fftCos[x];
                    s = fftSin[x];

                    kn2 = k + n2;

                    tr = xre[kn2] * c + xim[kn2] * s;
                    ti = xim[kn2] * c - xre[kn2] * s;

                    xre[kn2] = xre[k] - tr;
                    xim[kn2] = xim[k] - ti;
                    xre[k] += tr;
                    xim[k] += ti;

                    k++;
                    x++;
                }

                k += n2;
            }

            n2 >>= 1;
        }

        // 使用位反转重新排序输出
        int r;
        for (k = 0; k < sampleSize; k++) {
            r = fftBr[k];

            if (r > k) {
                tr = xre[k];
                ti = xim[k];

                xre[k] = xre[r];
                xim[k] = xim[r];
                xre[r] = tr;
                xim[r] = ti;
            }
        }

        // 计算幅度谱（包含 Nyquist 频率）
        float scale = 1.0f / sampleSize;
        mag[0] = (float) Math.sqrt(xre[0] * xre[0] + xim[0] * xim[0]) * scale;

        for (int i = 1; i < halfSize; i++) {
            mag[i] = 2 * (float) Math.sqrt(xre[i] * xre[i] + xim[i] * xim[i]) * scale;
        }

        // Nyquist frequency
        mag[halfSize] = (float) Math.sqrt(xre[halfSize] * xre[halfSize] +
                xim[halfSize] * xim[halfSize]) * scale;

        return mag;
    }

    @Override
    public double[] calculate(double[] samples) {
        float[] floatSamples = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            floatSamples[i] = (float) samples[i];
        }

        float[] result = calculate(floatSamples);

        double[] doubleResult = new double[result.length];
        for (int i = 0; i < result.length; i++) {
            doubleResult[i] = result[i];
        }
        return doubleResult;
    }

    @Override
    public int getSampleSize() {
        return sampleSize;
    }

    /**
     * 获取 Nyquist 频率索引
     */
    public int getNyquistIndex() {
        return halfSize;
    }

    /**
     * 检查是否使用 Hann 窗函数
     */
    public boolean isUseWindow() {
        return useWindow;
    }

    /**
     * 设置是否使用 Hann 窗函数
     */
    public void setUseWindow(boolean useWindow) {
        this.useWindow = useWindow;
    }

    /**
     * 用于 FFT 重排序的位反转函数
     *
     * @param j  输入索引
     * @param nu 位数
     * @return 位反转后的索引
     */
    private int bitrev(int j, int nu) {
        int j1 = j;
        int j2;
        int k = 0;

        for (int i = 1; i <= nu; i++) {
            j2 = j1 >> 1;
            k = (k << 1) + j1 - (j2 << 1);
            j1 = j2;
        }

        return k;
    }

    /**
     * 预计算正弦/余弦表和位反转表
     */
    private void prepareFFTTables() {
        int n2 = halfSize;
        int nu1 = numStages - 1;

        float p, arg;
        int k = 0, x = 0;

        // 准备正弦/余弦表
        for (int l = 1; l <= numStages; l++) {
            while (k < sampleSize) {
                for (int i = 1; i <= n2; i++) {
                    p = bitrev(k >> nu1, numStages);
                    arg = 2 * (float) Math.PI * p / sampleSize;

                    fftSin[x] = (float) Math.sin(arg);
                    fftCos[x] = (float) Math.cos(arg);

                    k++;
                    x++;
                }
                k += n2;
            }

            k = 0;
            nu1--;
            n2 >>= 1;
        }

        // 准备位反转表
        for (k = 0; k < sampleSize; k++) {
            fftBr[k] = bitrev(k, numStages);
        }
    }
}
