package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.spectrum;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.audio.AudioBuffer;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.AudioWidget;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.WidgetViewModel;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.math.fft.CooleyTukeyFFT;
import com.bingbaihanji.luca.plugin.audio.spectrum.utils.signal.SignalProcessing;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 频谱分析器 Widget
 * <p>
 * 实时 FFT 频谱显示
 */
public class SpectrumWidget implements AudioWidget {

    private static final Logger logger = LoggerFactory.getLogger(SpectrumWidget.class);
    private static final double SAMPLE_RATE = 48000.0;
    private static final double OVERLAP = 0.75;
    private static final double ALPHA = 0.2;  // 平滑系数
    private final SpectrumViewModel viewModel = new SpectrumViewModel();
    private final SpectrumView view = new SpectrumView(viewModel);
    private AudioBuffer audioBuffer;
    private CooleyTukeyFFT fft;
    private boolean isPaused = false;
    // FFT 配置
    private int fftSize = 2048;
    private long oldIndex = 0;
    // 平滑滤波状态
    private double[] smoothedSpectrum = new double[fftSize / 2 + 1];

    public SpectrumWidget() {
        this.fft = new CooleyTukeyFFT(fftSize);
    }

    @Override
    public WidgetViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public Region getView() {
        return view;
    }

    @Override
    public void setBuffer(AudioBuffer buffer) {
        this.audioBuffer = buffer;

        buffer.getNewDataProperty().addListener((obs, oldVal, newVal) -> {
            if (!isPaused) {
                handleNewData();
            }
        });

        logger.info("SpectrumWidget 已连接到 AudioBuffer");
    }

    @Override
    public void handleNewData() {
        try {
            long currentIndex = audioBuffer.getCurrentOffset();
            long available = currentIndex - oldIndex;
            int needed = (int) (fftSize * (1.0 - OVERLAP));

            // 检查是否有足够的数据进行 FFT
            if (available >= needed && currentIndex >= fftSize) {
                // 获取足够的样本进行 FFT
                double[] samples = audioBuffer.dataIndexed(oldIndex, fftSize);

                // FFT 分析
                double[] spectrum = fft.calculate(samples);

                // 指数平滑
                smoothedSpectrum = exponentialSmooth(spectrum, smoothedSpectrum, ALPHA);

                // 转换为 dB
                double[] dBSpectrum = SignalProcessing.spectrumToDecibels(smoothedSpectrum);

                // 查找峰值
                double peakFreq = findPeakFrequency(dBSpectrum, 20.0, 20000.0);

                // 更新 ViewModel
                viewModel.updateSpectrum(dBSpectrum, getFreqScale(), peakFreq);

                oldIndex += needed;

                // 标记需要重绘
                view.markDirty();
            }
        } catch (Exception e) {
            logger.error("处理频谱数据时出错", e);
        }
    }

    @Override
    public void canvasUpdate() {
        // 触发重绘（只在 isDirty 时真正渲染）
        view.render();
    }

    @Override
    public void pause() {
        isPaused = true;
        logger.info("SpectrumWidget 已暂停");
    }

    @Override
    public void restart() {
        isPaused = false;
        logger.info("SpectrumWidget 已重启");
    }

    /**
     * 指数平滑滤波
     */
    private double[] exponentialSmooth(double[] newSpectrum, double[] oldSpectrum, double alpha) {
        double[] result = new double[newSpectrum.length];
        for (int i = 0; i < newSpectrum.length; i++) {
            result[i] = alpha * newSpectrum[i] + (1.0 - alpha) * oldSpectrum[i];
        }
        return result;
    }

    /**
     * 生成频率刻度
     */
    private double[] getFreqScale() {
        int size = fftSize / 2 + 1;
        double[] freqScale = new double[size];
        for (int i = 0; i < size; i++) {
            freqScale[i] = i * SAMPLE_RATE / fftSize;
        }
        return freqScale;
    }

    /**
     * 查找频谱峰值
     */
    private double findPeakFrequency(double[] spectrum, double minFreq, double maxFreq) {
        double[] freqScale = getFreqScale();
        int peakIndex = 0;
        double peakValue = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < spectrum.length; i++) {
            double freq = freqScale[i];
            if (freq >= minFreq && freq <= maxFreq && spectrum[i] > peakValue) {
                peakValue = spectrum[i];
                peakIndex = i;
            }
        }
        return freqScale[peakIndex];
    }

    public int getFftSize() {
        return fftSize;
    }

    /**
     * 设置 FFT 大小
     */
    public void setFftSize(int newSize) {
        if (newSize <= 0 || (newSize & (newSize - 1)) != 0) {
            throw new IllegalArgumentException("FFT 大小必须是 2 的幂");
        }

        fftSize = newSize;
        fft = new CooleyTukeyFFT(newSize);
        smoothedSpectrum = new double[fftSize / 2 + 1];
        logger.info("频谱 FFT 大小设置为 {}", fftSize);
    }
}
