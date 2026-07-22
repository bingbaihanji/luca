package com.bingbaihanji.luca.plugin.audio.spectrum.decoder.impl;


import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderMetadata;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * APE 格式音频解码器
 *
 * <p>使用 FFmpeg 通过 JAVE 库将 APE (Monkey's Audio)
 * 无损音频文件解码为标准 PCM WAV 格式。</p>
 *
 * <p>Monkey's Audio（APE）是一种高效率的音频编解码器，
 * 提供无损音频压缩，支持更高的压缩率和播放性能。</p>
 *
 * <p>支持的格式: APE (Monkey's Audio)</p>
 */
public class ApeDecoder extends AbstractFFmpegDecoder {

    private static final String FORMAT_NAME = "ape";
    private static final String[] SUPPORTED_EXTENSIONS = {"ape", "APE"};

    /**
     * APE 文件的魔数（ASCII: "MAC "）
     */
    private static final byte[] APE_MAGIC = {'M', 'A', 'C', ' '};

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

        // 验证文件头魔数
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[4];
            if (raf.read(header) != 4) {
                return false;
            }

            return isApeMagic(header);

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查是否是 APE 文件头
     *
     * <p>APE 文件以 4 字节的魔数 "MAC " (4D 41 43 20) 开头。
     * 注意最后一个字符是空格(0x20)。</p>
     *
     * @param header 4 字节的文件头
     * @return true 如果是有效的 APE 文件头
     */
    private boolean isApeMagic(byte[] header) {
        return header[0] == APE_MAGIC[0]
                && header[1] == APE_MAGIC[1]
                && header[2] == APE_MAGIC[2]
                && header[3] == APE_MAGIC[3];
    }

    @Override
    public DecoderMetadata getMetadata() {
        return new DecoderMetadata(
                "APE Decoder",
                "1.0.0",
                "bingbaihanji@gmail.com",
                "Monkey's Audio (APE) 无损音频解码器"
        );
    }
}
