package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.spectrum;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.WidgetViewModel;

/**
 * Spectrum Widget 的 ViewModel
 */
public class SpectrumViewModel extends WidgetViewModel {

    private double[] spectrum;
    private double[] freqScale;
    private Double peakFreq;

    public SpectrumViewModel() {
        setTitle("频谱分析");
    }

    public double[] getSpectrum() {
        return spectrum;
    }

    public double[] getFreqScale() {
        return freqScale;
    }

    public Double getPeakFreq() {
        return peakFreq;
    }

    public void updateSpectrum(double[] spec, double[] freq, double peak) {
        this.spectrum = spec;
        this.freqScale = freq;
        this.peakFreq = peak;
    }
}
