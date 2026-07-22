package com.bingbaihanji.luca.plugin.audio.spectrum.decoder.impl;


import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.AudioDecoder;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderException;
import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderProgressListener;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.IOException;

/**
 * 基于 FFmpeg 的音频解码器抽象基类
 *
 * <p>提供了使用 JAVE (Java Audio Video Encoder) 库进行音频解码的公共实现。
 * 支持的格式包括 MP3、FLAC、APE 等通过 FFmpeg 支持的格式。</p>
 *
 * <p>子类只需要提供:
 * <ul>
 *   <li>支持的格式名称</li>
 *   <li>支持的文件扩展名</li>
 *   <li>文件格式检测逻辑（canDecode 方法）</li>
 *   <li>解码器元数据信息</li>
 * </ul>
 * </p>
 *
 * <p>解码规格:
 * <ul>
 *   <li>输出编码: PCM 16-bit Little Endian (pcm_s16le)</li>
 *   <li>采样率: 44100 Hz</li>
 *   <li>声道数: 2 (立体声)</li>
 * </ul>
 * </p>
 */
public abstract class AbstractFFmpegDecoder implements AudioDecoder {

    //  音频规格常量

    /**
     * 输出采样率 (44.1 kHz)
     */
    protected static final int OUTPUT_SAMPLE_RATE = 44100;

    /**
     * 输出声道数 (立体声)
     */
    protected static final int OUTPUT_CHANNELS = 2;

    /**
     * 输出编码格式 (PCM 16-bit Little Endian)
     */
    protected static final String OUTPUT_CODEC = "pcm_s16le";

    @Override
    public File decode(File sourceFile, DecoderProgressListener listener)
            throws DecoderException, IOException {

        // 验证源文件
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("源文件不存在: " + sourceFile);
        }

        // 创建临时输出文件
        File targetFile = File.createTempFile("decoded_", ".wav");
        targetFile.deleteOnExit();

        // 配置音频参数 (标准 PCM WAV 格式)
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec(OUTPUT_CODEC);          // PCM_SIGNED, 16bit, Little Endian
        audio.setSamplingRate(OUTPUT_SAMPLE_RATE);  // 44.1kHz
        audio.setChannels(OUTPUT_CHANNELS);    // Stereo

        // 配置编码参数
        EncodingAttributes encodingAttrs = new EncodingAttributes();
        encodingAttrs.setAudioAttributes(audio);

        Encoder encoder = new Encoder(new FFmpegLocator());

        try {
            if (listener != null) {
                // 有进度监听器：带进度回调的编码
                encoder.encode(
                        new MultimediaObject(sourceFile),
                        targetFile,
                        encodingAttrs,
                        createProgressAdapter(listener)
                );
            } else {
                // 无进度监听器：直接编码
                encoder.encode(
                        new MultimediaObject(sourceFile),
                        targetFile,
                        encodingAttrs
                );
            }
        } catch (EncoderException e) {
            // 转换为项目内的异常类型
            throw new DecoderException(
                    "音频解码失败: " + e.getMessage(),
                    DecoderException.ErrorCode.DECODING_FAILED,
                    e
            );
        }

        return targetFile;
    }

    /**
     * 创建从 FFmpeg 进度回调到项目监听器的适配器
     *
     * @param listener 项目监听器
     * @return FFmpeg 兼容的进度监听器
     */
    private ws.schild.jave.progress.EncoderProgressListener createProgressAdapter(
            DecoderProgressListener listener) {
        return new ws.schild.jave.progress.EncoderProgressListener() {
            @Override
            public void sourceInfo(ws.schild.jave.info.MultimediaInfo info) {
                // FFmpeg 源信息（如时长等）可在此处理
            }

            @Override
            public void progress(int permil) {
                // permil: 0 ~ 1000
                listener.onProgress(permil / 1000.0);
            }

            @Override
            public void message(String message) {
                // FFmpeg 输出日志（可选）
            }
        };
    }
}
