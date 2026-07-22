package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.scope;


import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.WidgetViewModel;

/**
 * Scope Widget 的 ViewModel
 */
public class ScopeViewModel extends WidgetViewModel {

    private double[] waveform;
    private double timeRangeMs = 50.0;

    public ScopeViewModel() {
        setTitle("示波器");
    }

    public double[] getWaveform() {
        return waveform;
    }

    public void updateWaveform(double[] data) {
        this.waveform = data;
    }

    public double getTimeRangeMs() {
        return timeRangeMs;
    }

    public void setTimeRangeMs(double timeRangeMs) {
        this.timeRangeMs = timeRangeMs;
    }
}
