package com.bingbaihanji.luca.plugin.audio.spectrum.decoder;

/**
 * 解码器元数据，描述解码器的基本信息
 */


public record DecoderMetadata(
        /*
          解码器的名称（例如：mp3Decoder ）
         */
        String name,
        /*
          解码器的版本号（遵循语义化版本规范，例如：1.0.0、2.1.5-beta）
         */
        String version,
        /*
           作者信息
         */
        String author,
        /*
          解码器的功能描述
         */
        String description) {


    @Override
    public String toString() {
        return String.format("%s v%s by %s - %s", name, version, author, description);
    }
}