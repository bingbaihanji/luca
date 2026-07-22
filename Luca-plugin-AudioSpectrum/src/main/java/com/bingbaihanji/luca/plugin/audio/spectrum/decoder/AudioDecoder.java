package com.bingbaihanji.luca.plugin.audio.spectrum.decoder;

import java.io.File;
import java.io.IOException;

/**
 * 音频解码器接口，定义音频解码的标准行为。
 * 所有音频格式解码器插件必须实现此接口。
 */
public interface AudioDecoder {

    /**
     * 获取此解码器支持的音频格式
     *
     * @return 格式名称（如 "wav", "mp3", "flac"）
     */
    String getSupportedFormat();

    /**
     * 获取此解码器支持的文件扩展名列表
     *
     * @return 扩展名数组（如 ["wav", "wave"]）
     */
    String[] getSupportedExtensions();

    /**
     * 检测是否可以解码指定文件
     *
     * @param file 待检测的音频文件
     * @return true 表示可以解码，false 表示不支持
     */
    boolean canDecode(File file);

    /**
     * 将音频文件解码为标准 PCM WAV 格式
     *
     * <p>解码后的 WAV 文件必须符合以下格式规范：</p>
     * <ul>
     *   <li><b>编码格式</b>：PCM_SIGNED（有符号PCM）</li>
     *   <li><b>采样率</b>：44100 Hz</li>
     *   <li><b>位深度</b>：16 bit</li>
     *   <li><b>声道数</b>：2（立体声）</li>
     *   <li><b>字节序</b>：Little Endian（小端序）</li>
     *   <li><b>帧大小</b>：4 字节（2 声道 × 16 bit / 8）</li>
     * </ul>
     *
     * <p>此标准格式确保后续的振幅提取和波形生成能够正确处理音频数据。</p>
     *
     * <p><b>临时文件管理</b>：返回的文件是由解码器创建的临时文件，调用方在使用完毕后
     * 应负责删除该文件。临时文件已标记为 deleteOnExit，JVM 退出时会自动清理。</p>
     *
     * @param sourceFile 源音频文件（可以是任何解码器支持的格式）
     * @param listener   进度监听器（可选，传入 null 则不监听进度）
     * @return 解码后的标准格式 WAV 文件（临时文件）
     * @throws DecoderException 解码过程中发生错误（格式不支持、文件损坏、磁盘空间不足等）
     * @throws IOException      IO 错误（文件读写失败等）
     */
    File decode(File sourceFile, DecoderProgressListener listener)
            throws DecoderException, IOException;

    /**
     * 获取解码器元数据信息
     *
     * @return 解码器元数据
     */
    DecoderMetadata getMetadata();
}
