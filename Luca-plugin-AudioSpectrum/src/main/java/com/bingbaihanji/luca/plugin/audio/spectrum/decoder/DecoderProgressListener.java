package com.bingbaihanji.luca.plugin.audio.spectrum.decoder;

/**
 * 解码进度监听器，用于接收解码过程中的进度和状态信息
 */
public interface DecoderProgressListener {

    /**
     * 进度更新回调
     *
     * @param progress 进度值，范围 0.0 ~ 1.0
     */
    void onProgress(double progress);

    /**
     * 消息回调
     *
     * @param message 消息内容
     */
    void onMessage(String message);

    /**
     * 解码开始回调
     *
     * @param sourceFile 源文件名
     */
    void onStart(String sourceFile);

    /**
     * 解码完成回调
     *
     * @param destinationFile 目标文件名
     */
    void onComplete(String destinationFile);

    /**
     * 解码失败回调
     *
     * @param error 错误信息
     */
    void onError(String error);
}
