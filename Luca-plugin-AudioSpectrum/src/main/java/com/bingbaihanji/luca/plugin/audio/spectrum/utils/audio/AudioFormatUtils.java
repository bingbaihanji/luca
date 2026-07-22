package com.bingbaihanji.luca.plugin.audio.spectrum.utils.audio;

import javax.sound.sampled.AudioFormat;

/**
 * PCM 音频解析工具类
 *
 * <p>提供音频格式相关的工具方法，包括 PCM 数据解码、样本归一化等
 *
 * @author bingbaihanji
 */
public final class AudioFormatUtils {

    private AudioFormatUtils() {
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    /**
     * 将 PCM 字节数据解析为左右声道浮点数组
     *
     * @param buffer    原始 PCM 数据
     * @param bytesRead 实际读取到的字节数
     * @param format    音频格式
     * @param left      左声道输出数组
     * @param right     右声道输出数组
     */
    public static void decodeStereo(byte[] buffer,
                                    int bytesRead,
                                    AudioFormat format,
                                    float[] left,
                                    float[] right) {
        if (buffer == null || format == null || left == null || right == null) {
            return;
        }

        int channels = format.getChannels();
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;

        if (channels <= 0 || sampleSizeInBytes <= 0) {
            clear(left, right);
            return;
        }

        // 目前按常见场景处理：8 位、16 位 PCM
        if (sampleSizeInBytes != 1 && sampleSizeInBytes != 2) {
            clear(left, right);
            return;
        }

        int frameSize = channels * sampleSizeInBytes;
        int sampleCount = Math.min(left.length, bytesRead / frameSize);

        for (int i = 0; i < sampleCount; i++) {
            int base = i * frameSize;

            left[i] = normalize(
                    readSample(buffer, base, sampleSizeInBytes, format.isBigEndian(), format.getEncoding()),
                    sampleSizeInBytes
            );

            if (channels >= 2 && base + sampleSizeInBytes * 2 <= bytesRead) {
                right[i] = normalize(
                        readSample(buffer, base + sampleSizeInBytes, sampleSizeInBytes, format.isBigEndian(), format.getEncoding()),
                        sampleSizeInBytes
                );
            } else {
                right[i] = left[i];
            }
        }

        // 未填充部分补零
        for (int i = sampleCount; i < left.length; i++) {
            left[i] = 0f;
            right[i] = 0f;
        }
    }

    /**
     * 将 PCM 字节数据解码为单声道浮点数组
     *
     * @param buffer    原始 PCM 数据
     * @param bytesRead 实际读取到的字节数
     * @param format    音频格式
     * @param output    输出数组
     */
    public static void decodeMono(byte[] buffer,
                                  int bytesRead,
                                  AudioFormat format,
                                  float[] output) {
        if (buffer == null || format == null || output == null) {
            return;
        }

        int channels = format.getChannels();
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;

        if (channels <= 0 || sampleSizeInBytes <= 0) {
            java.util.Arrays.fill(output, 0f);
            return;
        }

        int frameSize = channels * sampleSizeInBytes;
        int sampleCount = Math.min(output.length, bytesRead / frameSize);

        for (int i = 0; i < sampleCount; i++) {
            int base = i * frameSize;
            output[i] = normalize(
                    readSample(buffer, base, sampleSizeInBytes, format.isBigEndian(), format.getEncoding()),
                    sampleSizeInBytes
            );
        }

        // 未填充部分补零
        for (int i = sampleCount; i < output.length; i++) {
            output[i] = 0f;
        }
    }

    /**
     * 将 16-bit PCM 字节数组转换为 double 数组（归一化）
     *
     * @param byteArray 字节数组（16-bit signed PCM, little-endian）
     * @param length    有效字节数
     * @return 归一化的 Double 数组 [-1.0, 1.0]
     */
    public static double[] bytesToDoubleArray(byte[] byteArray, int length) {
        int sampleCount = length / 2;  // 16-bit = 2 bytes per sample
        double[] doubleArray = new double[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            // 读取 16-bit little-endian signed integer
            int low = byteArray[i * 2] & 0xFF;
            int high = byteArray[i * 2 + 1] << 8;

            short sample = (short) (high | low);  // 转换为有符号 16-bit

            // 归一化到 [-1.0, 1.0]
            doubleArray[i] = sample / 32768.0;
        }

        return doubleArray;
    }

    /**
     * 将 16-bit PCM 字节数组转换为 float 数组（归一化）
     *
     * @param byteArray 字节数组（16-bit signed PCM, little-endian）
     * @param length    有效字节数
     * @return 归一化的 float 数组 [-1.0, 1.0]
     */
    public static float[] bytesToFloatArray(byte[] byteArray, int length) {
        int sampleCount = length / 2;  // 16-bit = 2 bytes per sample
        float[] floatArray = new float[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            int low = byteArray[i * 2] & 0xFF;
            int high = byteArray[i * 2 + 1] << 8;
            short sample = (short) (high | low);
            floatArray[i] = sample / 32768.0f;
        }

        return floatArray;
    }

    /**
     * 计算 RMS (Root Mean Square) 音量
     *
     * @param samples 音频样本
     * @return RMS 值
     */
    public static double calculateRMS(double[] samples) {
        if (samples == null || samples.length == 0) {
            return 0;
        }
        double sum = 0;
        for (double sample : samples) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / samples.length);
    }

    /**
     * 计算 RMS (Root Mean Square) 音量（float 版本）
     *
     * @param samples 音频样本
     * @return RMS 值
     */
    public static float calculateRMS(float[] samples) {
        if (samples == null || samples.length == 0) {
            return 0;
        }
        float sum = 0;
        for (float sample : samples) {
            sum += sample * sample;
        }
        return (float) Math.sqrt(sum / samples.length);
    }

    /**
     * 将双声道合并为单声道
     *
     * @param left  左声道
     * @param right 右声道
     * @return 合并后的单声道数组
     */
    public static float[] mergeToMono(float[] left, float[] right) {
        if (left == null || right == null || left.length != right.length) {
            throw new IllegalArgumentException("左右声道必须非空且长度相同");
        }
        float[] result = new float[left.length];
        for (int i = 0; i < left.length; i++) {
            result[i] = (left[i] + right[i]) / 2.0f;
        }
        return result;
    }

    /**
     * 读取单个采样值
     */
    private static int readSample(byte[] buffer,
                                  int offset,
                                  int sampleSizeInBytes,
                                  boolean bigEndian,
                                  AudioFormat.Encoding encoding) {
        if (sampleSizeInBytes == 1) {
            int value = buffer[offset] & 0xFF;

            // PCM_UNSIGNED: 0~255 转为 -128~127
            if (AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)) {
                return value - 128;
            }

            // PCM_SIGNED: 直接按有符号字节处理
            return buffer[offset];
        }

        if (sampleSizeInBytes == 2) {
            int b0 = buffer[offset] & 0xFF;
            int b1 = buffer[offset + 1] & 0xFF;

            int value = bigEndian ? ((b0 << 8) | b1) : (b0 | (b1 << 8));

            // 符号扩展到 int
            if ((value & 0x8000) != 0) {
                value |= 0xFFFF0000;
            }
            return value;
        }

        return 0;
    }

    /**
     * 归一化到 [-1.0, 1.0]
     */
    private static float normalize(int sample, int sampleSizeInBytes) {
        if (sampleSizeInBytes == 1) {
            return sample / 128.0f;
        }
        if (sampleSizeInBytes == 2) {
            return sample / 32768.0f;
        }
        return 0f;
    }

    /**
     * 清空左右声道数组
     */
    private static void clear(float[] left, float[] right) {
        java.util.Arrays.fill(left, 0f);
        java.util.Arrays.fill(right, 0f);
    }
}
