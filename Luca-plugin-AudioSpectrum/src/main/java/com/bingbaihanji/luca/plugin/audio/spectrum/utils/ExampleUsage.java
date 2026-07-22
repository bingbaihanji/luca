package com.bingbaihanji.luca.plugin.audio.spectrum.utils;

import com.bingbaihanji.luca.plugin.audio.spectrum.utils.audio.AudioFormatUtils;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.color.CMRMap;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.color.ColorGradient;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.datastructure.RingBuffer;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.math.fft.CooleyTukeyFFT;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.math.fft.FFTProcessor;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal.PeakDetector;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal.SignalProcessing;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal.SpectrumAnalyzer;
import javafx.scene.paint.Color;

/**
 * 新工具包使用示例
 *
 * @author bingbaihanji
 */
public class ExampleUsage {

    /**
     * FFT 使用示例
     */
    public static void fftExample() {
        // 创建 FFT 处理器（样本大小必须是 2 的幂）
        FFTProcessor fft = new CooleyTukeyFFT(1024);

        // 准备时域音频数据
        float[] samples = new float[1024];
        // ... 填充音频数据 ...

        // 执行 FFT 转换到频域
        float[] spectrum = fft.calculate(samples);

        // spectrum 包含 fftSize/2 + 1 个频率分量的幅度值
        System.out.println("频谱长度: " + spectrum.length);
    }

    /**
     * 频谱分析器使用示例
     */
    public static void spectrumAnalyzerExample() {
        // 创建频谱分析器（FFT大小=1024，频带数=64，衰减率=0.3，采样率=48000Hz）
        SpectrumAnalyzer analyzer = new SpectrumAnalyzer(1024, 64, 0.3f, 48000.0);

        // 启用 A-weighting（模拟人耳听觉）
        analyzer.setUseAWeighting(true);

        // 准备音频样本
        float[] samples = new float[1024];
        // ... 填充音频数据 ...

        // 分析音频，获取频带强度
        float[] bands = analyzer.analyze(samples);

        // 查找峰值频率
        float[] fftData = new float[513]; // FFT大小/2+1
        double peakFreq = analyzer.findPeakFrequency(fftData, 20.0, 20000.0);
        System.out.println("峰值频率: " + peakFreq + " Hz");
    }

    /**
     * 峰值检测器使用示例
     */
    public static void peakDetectorExample() {
        // 创建峰值检测器（64个频带，保持20帧，下降速度2.0）
        PeakDetector detector = new PeakDetector(64, 20, 2.0f);

        // 更新并获取峰值
        float[] currentBands = new float[64];
        // ... 填充当前频带数据 ...

        for (int i = 0; i < 64; i++) {
            float peak = detector.updateAndGetPeak(i, currentBands[i]);
            System.out.println("频带 " + i + " 峰值: " + peak);
        }
    }

    /**
     * 环形缓冲区使用示例
     */
    public static void ringBufferExample() {
        // 创建环形缓冲区（初始大小4096）
        RingBuffer buffer = new RingBuffer(4096);

        // 写入音频数据
        double[] samples = new double[1024];
        // ... 填充数据 ...
        buffer.push(samples, System.currentTimeMillis() / 1000.0);

        // 读取最新的1024个样本
        double[] latest = buffer.data(1024);

        // 读取历史数据（延迟100个样本）
        double[] delayed = buffer.dataOlder(1024, 100);
    }

    /**
     * 颜色渐变使用示例
     */
    public static void colorGradientExample() {
        // 创建火焰渐变（绿->黄->红）
        ColorGradient fire = ColorGradient.createFireGradient();

        // 创建彩虹渐变
        ColorGradient rainbow = ColorGradient.createRainbowGradient(360);

        // 创建蓝色渐变
        ColorGradient blue = ColorGradient.createBlueGradient();

        // 创建热图渐变（CMRMap）
        ColorGradient heatmap = ColorGradient.createHeatmapGradient();

        // 获取指定位置的颜色（0.0 - 1.0）
        Color color = fire.getColor(0.5);

        // 直接获取 CMRMap 颜色
        Color heat = CMRMap.getColor(0.8);
    }

    /**
     * 音频格式工具使用示例
     */
    public static void audioFormatUtilsExample() {
        // 假设从音频输入获取PCM数据
        byte[] pcmData = new byte[2048];
        // ... 读取PCM数据 ...

        // 转换为float数组（16-bit PCM）
        float[] samples = AudioFormatUtils.bytesToFloatArray(pcmData, pcmData.length);

        // 计算 RMS 音量
        float rms = AudioFormatUtils.calculateRMS(samples);
        System.out.println("RMS: " + rms);

        // 解析立体声数据（需要 AudioFormat 对象）
        // AudioFormat format = ...;
        // float[] left = new float[sampleCount];
        // float[] right = new float[sampleCount];
        // AudioFormatUtils.decodeStereo(pcmData, pcmData.length, format, left, right);
    }

    /**
     * 信号处理工具使用示例
     */
    public static void signalProcessingExample() {
        // 指数平滑
        float smoothed = SignalProcessing.exponentialSmooth(0.8f, 0.5f, 0.3f);

        // 归一化到 [0, 1]
        float normalized = SignalProcessing.normalize(75f, 0f, 100f);

        // 映射到目标范围
        float mapped = SignalProcessing.map(0.5f, 0f, 255f);

        // 线性值转 dB
        float db = SignalProcessing.toDecibels(0.5f);

        // dB 转线性值
        float linear = SignalProcessing.fromDecibels(-6f);

        // 查找最大值
        float[] values = {0.1f, 0.5f, 0.8f, 0.3f};
        float[] result = SignalProcessing.findMax(values);
        float max = result[0];
        int index = (int) result[1];
    }

    public static void main(String[] args) {
        System.out.println("工具包使用示例");
        System.out.println("==============");
        System.out.println("请查看 ExampleUsage.java 中的示例代码");
    }
}
