package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.scope;

import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.audio.AudioBackend;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.audio.AudioBuffer;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.AudioWidget;
import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base.WidgetViewModel;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 示波器 Widget
 * <p>
 * 显示时域波形，支持触发功能
 */
public class ScopeWidget implements AudioWidget {

    private static final Logger logger = LoggerFactory.getLogger(ScopeWidget.class);

    private final ScopeViewModel viewModel = new ScopeViewModel();
    private final ScopeView view = new ScopeView(viewModel);

    private AudioBuffer audioBuffer;
    private boolean isPaused = false;

    // 配置参数
    private double timeRangeMs = 20.0;  // 时间范围（毫秒）

    public ScopeWidget() {
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

        // 监听新数据到达
        buffer.getNewDataProperty().addListener((obs, oldVal, newVal) -> {
            if (!isPaused) {
                handleNewData();
            }
        });

        logger.info("ScopeWidget 已连接到 AudioBuffer");
    }

    @Override
    public void handleNewData() {
        try {
            int samplesNeeded = (int) (timeRangeMs * 1e-3 * AudioBackend.SAMPLE_RATE);

            // 检查是否有足够的数据
            long available = audioBuffer.getRingBuffer().availableSamples();
            if (available < samplesNeeded * 2) {
                return;  // 数据不足，等待下次
            }

            // 获取 2× 时间范围的数据用于触发检测
            int totalSamples = samplesNeeded * 2;
            double[] samples = audioBuffer.data(totalSamples);

            // 在中间区域查找触发点
            double[] middle = new double[samplesNeeded];
            System.arraycopy(samples, samplesNeeded / 2, middle, 0, samplesNeeded);

            double threshold = max(middle) * 2.0 / 3.0;
            int triggerIndex = findRisingEdge(middle, threshold);

            // 提取触发后的波形
            int startIndex = samplesNeeded / 2 + triggerIndex - samplesNeeded / 2;
            int endIndex = Math.min(startIndex + samplesNeeded, samples.length);

            double[] waveform = new double[endIndex - startIndex];
            System.arraycopy(samples, startIndex, waveform, 0, endIndex - startIndex);

            // 更新 ViewModel
            viewModel.updateWaveform(waveform);

            // 标记需要重绘
            view.markDirty();
        } catch (Exception e) {
            logger.error("处理示波器数据时出错", e);
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
        logger.info("ScopeWidget 已暂停");
    }

    @Override
    public void restart() {
        isPaused = false;
        logger.info("ScopeWidget 已重启");
    }

    /**
     * 查找数组中的最大值
     */
    private double max(double[] array) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : array) {
            if (v > max) {
                max = v;
            }
        }
        return max == Double.NEGATIVE_INFINITY ? 0.0 : max;
    }

    /**
     * 查找上升沿触发点
     *
     * @param data      数据数组
     * @param threshold 阈值
     * @return 触发点索引
     */
    private int findRisingEdge(double[] data, double threshold) {
        for (int i = 0; i < data.length - 1; i++) {
            if (data[i] < threshold && data[i + 1] >= threshold) {
                return i;
            }
        }
        return 0;  // 未找到触发点，返回起始位置
    }

    public double getTimeRange() {
        return timeRangeMs;
    }

    /**
     * 设置时间范围
     */
    public void setTimeRange(double rangeMs) {
        this.timeRangeMs = rangeMs;
        viewModel.setTimeRangeMs(rangeMs);
        logger.info("示波器时间范围设置为 {} ms", rangeMs);
    }
}
