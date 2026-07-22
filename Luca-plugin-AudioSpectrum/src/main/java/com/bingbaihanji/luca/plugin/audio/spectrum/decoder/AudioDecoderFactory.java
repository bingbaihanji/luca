package com.bingbaihanji.luca.plugin.audio.spectrum.decoder;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 音频解码器工厂，负责管理和选择合适的解码器
 * 使用单例模式，支持运行时动态注册解码器
 */
public class AudioDecoderFactory {

    private static final AudioDecoderFactory INSTANCE = new AudioDecoderFactory();

    // 使用线程安全的列表存储解码器
    private final List<AudioDecoder> decoders = new CopyOnWriteArrayList<>();

    // 按格式名称索引解码器（加速查找）
    private final Map<String, AudioDecoder> decodersByFormat = new HashMap<>();

    private AudioDecoderFactory() {
        // 私有构造函数
    }

    public static AudioDecoderFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 注册解码器
     *
     * @param decoder 解码器实例
     */
    public void registerDecoder(AudioDecoder decoder) {
        if (decoder == null) {
            throw new IllegalArgumentException("Decoder cannot be null");
        }

        // 添加到列表
        decoders.add(decoder);

        // 更新格式索引
        String format = decoder.getSupportedFormat().toLowerCase();
        decodersByFormat.put(format, decoder);

        System.out.println("已注册解码器: " + decoder.getMetadata().name());
    }

    /**
     * 注销解码器
     *
     * @param decoder 解码器实例
     */
    public void unregisterDecoder(AudioDecoder decoder) {
        decoders.remove(decoder);
        decodersByFormat.values().removeIf(d -> d == decoder);
    }

    /**
     * 获取支持指定文件的解码器
     *
     * @param file 音频文件
     * @return 合适的解码器，如果没有找到返回 null
     */
    public AudioDecoder getDecoder(File file) {
        if (file == null) {
            return null;
        }

        // 遍历所有解码器，返回第一个能够解码的
        for (AudioDecoder decoder : decoders) {
            if (decoder.canDecode(file)) {
                return decoder;
            }
        }

        return null;
    }

    /**
     * 根据格式名称获取解码器
     *
     * @param format 格式名称（如 "wav", "mp3"）
     * @return 对应的解码器，如果没有找到返回 null
     */
    public AudioDecoder getDecoderByFormat(String format) {
        if (format == null) {
            return null;
        }
        return decodersByFormat.get(format.toLowerCase());
    }

    /**
     * 根据文件扩展名获取解码器
     *
     * @param extension 文件扩展名（不含点号）
     * @return 合适的解码器，如果没有找到返回 null
     */
    public AudioDecoder getDecoderByExtension(String extension) {
        if (extension == null) {
            return null;
        }

        String ext = extension.toLowerCase();
        for (AudioDecoder decoder : decoders) {
            for (String supportedExt : decoder.getSupportedExtensions()) {
                if (supportedExt.equalsIgnoreCase(ext)) {
                    return decoder;
                }
            }
        }

        return null;
    }

    /**
     * 获取所有已注册的解码器
     *
     * @return 解码器列表（不可修改）
     */
    public List<AudioDecoder> getAllDecoders() {
        return Collections.unmodifiableList(decoders);
    }

    /**
     * 获取所有支持的格式
     *
     * @return 格式名称列表
     */
    public List<String> getSupportedFormats() {
        List<String> formats = new ArrayList<>();
        for (AudioDecoder decoder : decoders) {
            formats.add(decoder.getSupportedFormat());
        }
        return formats;
    }

    /**
     * 检查是否支持指定格式
     *
     * @param format 格式名称
     * @return true 表示支持
     */
    public boolean isFormatSupported(String format) {
        return decodersByFormat.containsKey(format.toLowerCase());
    }

    /**
     * 清空所有解码器
     */
    public void clear() {
        decoders.clear();
        decodersByFormat.clear();
    }
}
