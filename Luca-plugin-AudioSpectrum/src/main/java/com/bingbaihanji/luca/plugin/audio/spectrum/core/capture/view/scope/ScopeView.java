package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.scope;


import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.WidgetView;
import javafx.scene.paint.Color;

/**
 * Scope Widget 的 View（渲染层）
 */
public class ScopeView extends WidgetView {

    private final ScopeViewModel viewModel;

    public ScopeView(ScopeViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void render() {
        // 优化：只在有新数据时才重绘
        if (!isDirty()) {
            return;
        }

        double[] waveform = viewModel.getWaveform();
        if (waveform == null || waveform.length == 0) {
            clearCanvas();
            setDirty(false);
            return;
        }

        // 清空画布
        clearCanvas();

        // 绘制背景网格
        drawGrid();

        // 绘制波形
        drawWaveform(waveform);

        // 绘制刻度标签
        drawLabels();

        setDirty(false);
    }

    /**
     * 绘制波形
     */
    private void drawWaveform(double[] waveform) {
        gc.setStroke(Color.LIME);
        gc.setLineWidth(2.0);
        gc.beginPath();

        for (int i = 0; i < waveform.length; i++) {
            double sample = waveform[i];
            double x = i * getCanvasWidth() / waveform.length;
            // 将 [-1, 1] 映射到画布高度
            double y = getCanvasHeight() / 2 * (1 - sample);

            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }

        gc.stroke();
    }

    /**
     * 绘制刻度标签
     */
    private void drawLabels() {
        gc.setFill(Color.WHITE);
        gc.fillText("时间范围: " + viewModel.getTimeRangeMs() + " ms", 10, 20);

        // Y轴标签
        gc.fillText("+1.0", 10, 30);
        gc.fillText("0.0", 10, getCanvasHeight() / 2);
        gc.fillText("-1.0", 10, getCanvasHeight() - 10);
    }
}
