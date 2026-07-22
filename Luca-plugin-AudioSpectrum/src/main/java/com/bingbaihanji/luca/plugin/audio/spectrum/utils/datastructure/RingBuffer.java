package com.bingbaihanji.luca.plugin.audio.spectrum.utils.datastructure;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程安全的环形缓冲区，用于存储音频数据
 *
 * <p>采用双缓冲技术避免取模运算，提高性能
 *
 * @author bingbaihanji
 */
public class RingBuffer {

    private final ReentrantLock lock = new ReentrantLock();

    // 双缓冲：实际分配 2 × bufferLength 大小
    private int bufferLength;
    private double[] buffer;

    // 当前写入偏移量（全局计数器）
    private long offset = 0;

    // 最后一次写入的时间戳
    private double offsetTime = 0.0;

    /**
     * 创建指定初始大小的环形缓冲区
     *
     * @param initialSize 初始缓冲区大小
     */
    public RingBuffer(int initialSize) {
        this.bufferLength = initialSize;
        this.buffer = new double[bufferLength * 2];
    }

    /**
     * 将新数据推入环形缓冲区
     *
     * @param data      要推入的数据
     * @param timestamp 数据时间戳
     */
    public void push(double[] data, double timestamp) {
        lock.lock();
        try {
            int length = data.length;

            // 如果需要扩容
            if (length > bufferLength / 2) {
                growIfNeeded(length * 2);
            }

            // 计算在缓冲区中的位置
            int pos = (int) (offset % bufferLength);

            // 如果数据会跨越边界，分两段写入
            if (pos + length <= bufferLength) {
                // 不跨越边界，直接写入
                System.arraycopy(data, 0, buffer, pos, length);
                System.arraycopy(data, 0, buffer, pos + bufferLength, length);
            } else {
                // 跨越边界，分两段
                int firstPart = bufferLength - pos;
                int secondPart = length - firstPart;

                // 第一段
                System.arraycopy(data, 0, buffer, pos, firstPart);
                System.arraycopy(data, 0, buffer, pos + bufferLength, firstPart);

                // 第二段（环绕到开头）
                System.arraycopy(data, firstPart, buffer, 0, secondPart);
                System.arraycopy(data, firstPart, buffer, bufferLength, secondPart);
            }

            offset += length;
            offsetTime = timestamp;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将新数据推入环形缓冲区（无时间戳版本）
     *
     * @param data 要推入的数据
     */
    public void push(double[] data) {
        push(data, System.currentTimeMillis() / 1000.0);
    }

    /**
     * 获取最新的 length 个样本
     *
     * @param length 要获取的样本数
     * @return 样本数组
     */
    public double[] data(int length) {
        lock.lock();
        try {
            if (length > bufferLength) {
                throw new IllegalArgumentException(
                        "Requested length " + length + " exceeds buffer size " + bufferLength);
            }

            long start = Math.max(0L, offset - length);
            return dataIndexed(start, length);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从指定索引获取 length 个样本
     *
     * @param startIndex 起始索引（全局）
     * @param length     要获取的样本数
     * @return 样本数组
     */
    public double[] dataIndexed(long startIndex, int length) {
        lock.lock();
        try {
            if (length > bufferLength) {
                throw new IllegalArgumentException(
                        "Requested length " + length + " exceeds buffer size " + bufferLength);
            }
            if (startIndex < 0) {
                throw new IllegalArgumentException("Start index must be non-negative, got " + startIndex);
            }
            if (startIndex + length > offset) {
                throw new IllegalArgumentException(
                        "Requested range [" + startIndex + ", " + (startIndex + length) +
                                ") exceeds offset " + offset);
            }

            // 双缓冲的好处：无需担心环绕，直接取
            int pos = (int) (startIndex % bufferLength);
            double[] result = new double[length];
            System.arraycopy(buffer, pos, result, 0, length);

            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取历史数据（相对于当前位置向前 delaySamples）
     *
     * @param length       要获取的样本数
     * @param delaySamples 延迟样本数
     * @return 样本数组
     */
    public double[] dataOlder(int length, int delaySamples) {
        lock.lock();
        try {
            long start = Math.max(0L, offset - length - delaySamples);
            return dataIndexed(start, length);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 动态扩容缓冲区
     *
     * @param minLength 最小需要的长度
     */
    private void growIfNeeded(int minLength) {
        if (minLength <= bufferLength) {
            return;
        }

        // 扩容为 1.5 倍
        int newLength = (int) (minLength * 1.5);

        double[] newBuffer = new double[newLength * 2];

        // 复制现有数据
        int pos = (int) (offset % bufferLength);
        int validLength = Math.min(buffer.length, bufferLength);

        // 复制旧数据到新缓冲区
        System.arraycopy(buffer, pos, newBuffer, 0, validLength - pos);
        if (pos > 0) {
            System.arraycopy(buffer, 0, newBuffer, validLength - pos, pos);
        }

        // 双缓冲：复制第二份
        System.arraycopy(newBuffer, 0, newBuffer, newLength, newLength);

        buffer = newBuffer;
        bufferLength = newLength;
    }

    /**
     * 清空缓冲区
     */
    public void clear() {
        lock.lock();
        try {
            offset = 0;
            offsetTime = 0.0;
            java.util.Arrays.fill(buffer, 0.0);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取缓冲区当前大小
     *
     * @return 缓冲区大小
     */
    public int size() {
        return bufferLength;
    }

    /**
     * 获取已存储的样本数
     *
     * @return 已存储的样本数
     */
    public long availableSamples() {
        return Math.min(offset, bufferLength);
    }

    /**
     * 获取当前写入偏移量
     *
     * @return 当前偏移量
     */
    public long getOffset() {
        lock.lock();
        try {
            return offset;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取最后一次写入的时间戳
     *
     * @return 时间戳
     */
    public double getOffsetTime() {
        lock.lock();
        try {
            return offsetTime;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检查缓冲区是否为空
     *
     * @return 如果缓冲区为空返回 true
     */
    public boolean isEmpty() {
        return offset == 0;
    }

    /**
     * 获取缓冲区中最新数据的拷贝
     *
     * @return 最新数据的拷贝
     */
    public double[] getLatest(int count) {
        return data(Math.min(count, (int) availableSamples()));
    }
}
