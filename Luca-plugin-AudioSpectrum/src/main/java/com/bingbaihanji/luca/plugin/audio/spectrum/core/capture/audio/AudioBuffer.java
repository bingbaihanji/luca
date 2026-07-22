package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.audio;

import com.bingbaihanji.luca.plugin.audio.spectrum.utils.datastructure.RingBuffer;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * 音频缓冲区包装器，桥接 AudioBackend 和 Widgets
 * <p>
 * 提供观察者模式接口，让多个 Widget 可以共享同一音频数据
 */
public class AudioBuffer {

    private final RingBuffer ringBuffer;

    /**
     * 新数据可用属性，用于通知 Widget 更新
     * 使用布尔值切换来触发监听器
     */
    private final SimpleBooleanProperty newDataProperty = new SimpleBooleanProperty(false);

    /**
     * 创建音频缓冲区
     *
     * @param ringBuffer 底层环形缓冲区
     */
    public AudioBuffer(RingBuffer ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    /**
     * 获取环形缓冲区
     */
    public RingBuffer getRingBuffer() {
        return ringBuffer;
    }

    /**
     * 获取新数据属性（用于监听）
     */
    public SimpleBooleanProperty getNewDataProperty() {
        return newDataProperty;
    }

    /**
     * 处理新音频数据到达
     *
     * @param samples   音频样本数组
     * @param timestamp 时间戳
     */
    public void handleNewData(double[] samples, double timestamp) {
        // 数据已经在 AudioBackend 中推送到 RingBuffer
        // 这里只需要触发属性变化，通知所有监听的 Widget
        newDataProperty.set(!newDataProperty.get());
    }

    /**
     * 获取最新的 length 个样本
     *
     * @param length 样本数
     * @return 样本数组
     */
    public double[] data(int length) {
        return ringBuffer.data(length);
    }

    /**
     * 从指定索引获取样本
     *
     * @param startIndex 起始索引
     * @param length     样本数
     * @return 样本数组
     */
    public double[] dataIndexed(long startIndex, int length) {
        return ringBuffer.dataIndexed(startIndex, length);
    }

    /**
     * 获取历史数据
     *
     * @param length       样本数
     * @param delaySamples 延迟样本数
     * @return 样本数组
     */
    public double[] dataOlder(int length, int delaySamples) {
        return ringBuffer.dataOlder(length, delaySamples);
    }

    /**
     * 获取当前偏移量
     */
    public long getCurrentOffset() {
        return ringBuffer.getOffset();
    }

    /**
     * 获取当前时间戳
     */
    public double getCurrentTimestamp() {
        return ringBuffer.getOffsetTime();
    }
}
