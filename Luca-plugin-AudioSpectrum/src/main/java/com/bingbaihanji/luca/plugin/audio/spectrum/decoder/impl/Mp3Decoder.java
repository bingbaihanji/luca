package com.bingbaihanji.luca.plugin.audio.spectrum.decoder.impl;

import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderMetadata;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * MP3 格式音频解码器
 *
 * <p>使用 FFmpeg 通过 JAVE 库将 MP3 文件解码为标准 PCM WAV 格式。</p>
 *
 * <p>支持的格式: MP3 (MPEG-1/MPEG-2 Layer III Audio)</p>
 */
public class Mp3Decoder extends AbstractFFmpegDecoder {

    private static final String FORMAT_NAME = "mp3";
    private static final String[] SUPPORTED_EXTENSIONS = {"mp3", "MP3"};

    @Override
    public String getSupportedFormat() {
        return FORMAT_NAME;
    }

    @Override
    public String[] getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public boolean canDecode(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[3];
            if (raf.read(header) != 3) {
                return false;
            }

            // MP3 文件的魔数检测
            // 1. 检查 ID3v2 标签 (用于元数据)
            if (isId3v2Header(header)) {
                return true;
            }

            // 2. 检查 MPEG 帧同步 (实际音频数据开始处)
            if (isMpegFrameSync(header)) {
                return true;
            }

        } catch (IOException e) {
            return false;
        }

        return false;
    }

    /**
     * 检查是否是 ID3v2 标签头
     *
     * <p>ID3v2 标签以 "ID3" 字符开头</p>
     *
     * @param header 3 字节的文件头
     * @return true 如果是 ID3v2 标签
     */
    private boolean isId3v2Header(byte[] header) {
        return header[0] == 'I' && header[1] == 'D' && header[2] == '3';
    }

    /**
     * 检查是否是 MPEG 帧同步
     *
     * <p>MPEG 帧同步位为 0xFFE 或 0xFFF。检查方法:
     * <ul>
     *   <li>第 1 字节为 0xFF</li>
     *   <li>第 2 字节的高 4 位为 0xE 或 0xF</li>
     * </ul>
     * </p>
     *
     * @param header 3 字节的文件头
     * @return true 如果是 MPEG 帧同步
     */
    private boolean isMpegFrameSync(byte[] header) {
        int firstByte = header[0] & 0xFF;
        int secondByte = header[1] & 0xFF;

        // 检查第一个字节为 0xFF，且第二字节的高 4 位为 0xE 或 0xF
        return firstByte == 0xFF && ((secondByte & 0xE0) == 0xE0);
    }

    @Override
    public DecoderMetadata getMetadata() {
        return new DecoderMetadata(
                "MP3 Decoder",
                "1.0.0",
                "bingbaihanji@gmail.com",
                "MPEG-1/MPEG-2 Layer III 格式解码器"
        );
    }
}
