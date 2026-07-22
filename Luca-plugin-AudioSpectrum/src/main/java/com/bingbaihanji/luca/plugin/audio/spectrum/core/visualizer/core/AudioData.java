package com.bingbaihanji.luca.plugin.audio.spectrum.core.visualizer.core;

/**
 * 线程安全的不可变音频数据容器
 *
 * <p>此类封装左右声道音频采样，自动计算立体声合并数据用于单声道可视化
 * 所有数组都进行防御性拷贝以确保不可变性
 *
 * @author 音频可视化组件
 */
public final class AudioData {

    private final float[] leftChannel; // 左声道
    private final float[] rightChannel; // 右声道
    private final float[] stereoMerge; // 立体声合并

    /**
     * 使用给定的声道采样创建新的 AudioData 实例
     *
     * @param left  左声道采样（-1.0 到 1.0）
     * @param right 右声道采样（-1.0 到 1.0）
     * @throws NullPointerException     如果任一声道为 null
     * @throws IllegalArgumentException 如果声道长度不同
     */
    public AudioData(float[] left, float[] right) {
        if (left == null || right == null) {
            throw new NullPointerException("Audio channels cannot be null");
        }
        if (left.length != right.length) {
            throw new IllegalArgumentException("Left and right channels must have the same length");
        }

        // 防御性拷贝，确保外部修改不影响内部状态
        this.leftChannel = left.clone();
        this.rightChannel = right.clone();

        // 预计算立体声合并，避免每帧重复计算
        this.stereoMerge = new float[left.length];
        for (int i = 0; i < left.length; i++) {
            stereoMerge[i] = (left[i] + right[i]) / 2.0f;
        }
    }

    /**
     * 返回左声道采样
     *
     * @return 左声道数组（只读引用）
     */
    public float[] getLeftChannel() {
        return leftChannel;
    }

    /**
     * 返回右声道采样
     *
     * @return 右声道数组（只读引用）
     */
    public float[] getRightChannel() {
        return rightChannel;
    }

    /**
     * 返回合并的立体声采样
     *
     * @return 立体声合并数组（只读引用）
     */
    public float[] getStereoMerge() {
        return stereoMerge;
    }

    /**
     * 返回每个声道的采样数量
     *
     * @return 采样数量
     */
    public int getSampleCount() {
        return leftChannel.length;
    }

    @Override
    public String toString() {
        return String.format("AudioData[samples=%d]", getSampleCount());
    }
}
