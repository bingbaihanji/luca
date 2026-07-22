package com.bingbaihanji.luca.plugin.audio.spectrum.decoder.impl;


import com.bingbaihanji.luca.plugin.audio.spectrum.decoder.DecoderMetadata;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * FLAC 格式音频解码器
 *
 * <p>使用 FFmpeg 通过 JAVE 库将 FLAC (Free Lossless Audio Codec)
 * 无损音频文件解码为标准 PCM WAV 格式。</p>
 *
 * <p>FLAC 是一种开放的、免版税的无损音频编解码标准，
 * 支持无损压缩，可以恢复到原始比特完全相同的音频数据。</p>
 *
 * <p>支持的格式: FLAC (Free Lossless Audio Codec)</p>
 */
public class FlacDecoder extends AbstractFFmpegDecoder {

    private static final String FORMAT_NAME = "flac";
    private static final String[] SUPPORTED_EXTENSIONS = {"flac", "FLAC"};

    /**
     * FLAC 文件的魔数（ASCII: "fLaC"）
     */
    private static final byte[] FLAC_MAGIC = {'f', 'L', 'a', 'C'};

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

            return isFlacMagic(header);

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查是否是 FLAC 文件头
     *
     * <p>FLAC 文件以 4 字节的魔数 "fLaC" (66 4C 61 43) 开头</p>
     *
     * @param header 4 字节的文件头
     * @return true 如果是有效的 FLAC 文件头
     */
    private boolean isFlacMagic(byte[] header) {
        return header[0] == FLAC_MAGIC[0]
                && header[1] == FLAC_MAGIC[1]
                && header[2] == FLAC_MAGIC[2]
                && header[3] == FLAC_MAGIC[3];
    }

    @Override
    public DecoderMetadata getMetadata() {
        return new DecoderMetadata(
                "FLAC Decoder",
                "1.0.0",
                "bingbaihanji@gmail.com",
                "FLAC 无损音频解码器"
        );
    }
}
