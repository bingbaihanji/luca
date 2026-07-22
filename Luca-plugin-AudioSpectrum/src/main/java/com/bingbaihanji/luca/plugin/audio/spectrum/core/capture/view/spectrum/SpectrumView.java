package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.spectrum;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.WidgetView;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Spectrum Widget 的 View（渲染层）
 */
public class SpectrumView extends WidgetView {

    private static final double MIN_FREQ = 20.0;
    private static final double MAX_FREQ = 20000.0;
    private static final double MIN_DB = -100.0;
    private static final double MAX_DB = 0.0;
    private final SpectrumViewModel viewModel;

    public SpectrumView(SpectrumViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void render() {
        // 优化：只在有新数据时才重绘
        if (!isDirty()) {
            return;
        }

        double[] spectrum = viewModel.getSpectrum();
        double[] freqScale = viewModel.getFreqScale();

        if (spectrum == null || freqScale == null) {
            clearCanvas();
            setDirty(false);
            return;
        }

        clearCanvas();

        // 绘制背景网格
        drawSpectrumGrid();

        // 绘制频谱曲线
        drawSpectrum(spectrum, freqScale);

        // 绘制峰值标记
        Double peakFreq = viewModel.getPeakFreq();
        if (peakFreq != null) {
            drawPeakMarker(peakFreq);
        }

        // 绘制刻度
        drawFrequencyLabels();

        setDirty(false);
    }

    /**
     * 绘制频谱曲线
     */
    private void drawSpectrum(double[] spectrum, double[] freqScale) {
        List<double[]> points = new ArrayList<>();

        // 1. 收集有效点
        for (int i = 0; i < spectrum.length; i++) {
            double freq = freqScale[i];
            if (freq >= MIN_FREQ && freq <= MAX_FREQ) {
                double x = freqToX(freq);
                double y = dBToY(spectrum[i]);
                points.add(new double[]{x, y});
            }
        }

        if (points.size() < 2) {
            return;
        }

        // 2. 拆分为数组（高性能方式）
        int n = points.size();
        double[] xs = new double[n];
        double[] ys = new double[n];

        for (int i = 0; i < n; i++) {
            double[] p = points.get(i);
            xs[i] = p[0];
            ys[i] = p[1];
        }

        // 3. 填充区域
        gc.setFill(Color.CYAN.deriveColor(0, 1, 1, 0.5));

        gc.beginPath();
        gc.moveTo(xs[0], getCanvasHeight());
        gc.lineTo(xs[0], ys[0]);

        // 使用平滑曲线绘制上边界
        for (int i = 0; i < n - 1; i++) {
            double x0 = i > 0 ? xs[i - 1] : xs[i];
            double y0 = i > 0 ? ys[i - 1] : ys[i];
            double x1 = xs[i];
            double y1 = ys[i];
            double x2 = xs[i + 1];
            double y2 = ys[i + 1];
            double x3 = i + 2 < n ? xs[i + 2] : xs[i + 1];
            double y3 = i + 2 < n ? ys[i + 2] : ys[i + 1];

            double cp1x = x1 + (x2 - x0) / 6.0;
            double cp1y = y1 + (y2 - y0) / 6.0;
            double cp2x = x2 - (x3 - x1) / 6.0;
            double cp2y = y2 - (y3 - y1) / 6.0;

            gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x2, y2);
        }

        gc.lineTo(xs[n - 1], getCanvasHeight());
        gc.closePath();
        gc.fill();

        // 4. 描边（平滑曲线）
        gc.setStroke(Color.valueOf("#3AEA8D"));
        gc.setLineWidth(2.0);

        drawSmoothLine(gc, xs, ys);
    }

    /**
     * 绘制平滑曲线
     */
    private void drawSmoothLine(GraphicsContext gc, double[] x, double[] y) {
        int n = x.length;
        if (n < 2 || y.length != n) {
            return;
        }

        gc.beginPath();
        gc.moveTo(x[0], y[0]);

        for (int i = 0; i < n - 1; i++) {
            double x0 = i > 0 ? x[i - 1] : x[i];
            double y0 = i > 0 ? y[i - 1] : y[i];
            double x1 = x[i];
            double y1 = y[i];
            double x2 = x[i + 1];
            double y2 = y[i + 1];
            double x3 = i + 2 < n ? x[i + 2] : x[i + 1];
            double y3 = i + 2 < n ? y[i + 2] : y[i + 1];

            // Catmull-Rom → Bezier 控制点
            double cp1x = x1 + (x2 - x0) / 6.0;
            double cp1y = y1 + (y2 - y0) / 6.0;
            double cp2x = x2 - (x3 - x1) / 6.0;
            double cp2y = y2 - (y3 - y1) / 6.0;

            gc.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x2, y2);
        }

        gc.stroke();
    }

    /**
     * 绘制频谱网格
     */
    private void drawSpectrumGrid() {
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(0.5);

        // 水平线 (每 20dB)
        for (int db = -80; db <= 0; db += 20) {
            double y = dBToY(db);
            gc.strokeLine(0, y, getCanvasWidth(), y);
        }

        // 垂直线 (对数频率)
        double[] freqs = {50.0, 100.0, 500.0, 1000.0, 5000.0, 10000.0};
        for (double freq : freqs) {
            double x = freqToX(freq);
            gc.strokeLine(x, 0, x, getCanvasHeight());
        }
    }

    /**
     * 绘制峰值标记
     */
    private void drawPeakMarker(double peakFreq) {
        if (peakFreq < MIN_FREQ || peakFreq > MAX_FREQ) {
            return;
        }

        double x = freqToX(peakFreq);
        gc.setStroke(Color.RED);
        gc.setLineWidth(2.0);
        gc.strokeLine(x, 0, x, getCanvasHeight());

        // 频率标签
        gc.setFill(Color.RED);
        String label;
        if (peakFreq >= 1000) {
            label = String.format("%.1f kHz", peakFreq / 1000.0);
        } else {
            label = String.format("%d Hz", (int) peakFreq);
        }
        gc.fillText(label, x + 5, 20);
    }

    /**
     * 绘制频率标签
     */
    private void drawFrequencyLabels() {
        gc.setFill(Color.WHITE);

        // 频率刻度
        double[] freqs = {50.0, 100.0, 500.0, 1000.0, 5000.0, 10000.0};
        for (double freq : freqs) {
            double x = freqToX(freq);
            String label;
            if (freq >= 1000) {
                label = String.format("%dk", (int) (freq / 1000.0));
            } else {
                label = String.format("%d", (int) freq);
            }
            gc.fillText(label, x - 10, getCanvasHeight() - 5);
        }

        // dB 刻度
        for (int db = -80; db <= 0; db += 20) {
            double y = dBToY(db);
            gc.fillText(db + " dB", 5, y - 5);
        }
    }

    /**
     * 频率到 X 坐标（对数刻度）
     */
    private double freqToX(double freq) {
        double logMin = Math.log10(MIN_FREQ);
        double logMax = Math.log10(MAX_FREQ);
        double logFreq = Math.log10(freq);
        return (logFreq - logMin) / (logMax - logMin) * getCanvasWidth();
    }

    /**
     * dB 值到 Y 坐标
     */
    private double dBToY(double dB) {
        double normalized = (dB - MIN_DB) / (MAX_DB - MIN_DB);
        normalized = Math.max(0.0, Math.min(1.0, normalized));
        return getCanvasHeight() * (1 - normalized);
    }
}
