package com.bingbaihanji.luca.plugin.audio.spectrum.decoder;

import lombok.Getter;

/**
 * 音频解码异常
 */
@Getter
public class DecoderException extends Exception {

    private final ErrorCode errorCode;

    public DecoderException(String message) {
        this(message, ErrorCode.UNKNOWN);
    }

    public DecoderException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public DecoderException(String message, Throwable cause) {
        this(message, ErrorCode.UNKNOWN, cause);
    }

    public DecoderException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 错误代码枚举
     */
    public enum ErrorCode {
        UNSUPPORTED_FORMAT,      // 不支持的格式
        CORRUPTED_FILE,          // 文件损坏
        INSUFFICIENT_DISK_SPACE, // 磁盘空间不足
        DECODING_FAILED,         // 解码失败
        IO_ERROR,                // IO 错误
        UNKNOWN                  // 未知错误
    }
}
