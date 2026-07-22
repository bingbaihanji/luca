package com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.view.base;


import com.bingbaihanji.luca.plugin.audio.spectrum.core.capture.audio.AudioBuffer;
import javafx.scene.layout.Region;

/**
 * AudioWidget 接口
 * <p>
 * 所有音频可视化 Widget 都应实现此接口
 */
public interface AudioWidget {

    /**
     * 获取 ViewModel
     */
    WidgetViewModel getViewModel();

    /**
     * 获取视图
     */
    Region getView();

    /**
     * 设置音频缓冲区
     */
    void setBuffer(AudioBuffer buffer);

    /**
     * 处理新音频数据到达
     * 在 JavaFX 线程调用
     */
    void handleNewData();

    /**
     * Canvas 渲染更新
     * 由 AnimationTimer 在 60 FPS 调用
     */
    void canvasUpdate();

    /**
     * 暂停 Widget
     */
    void pause();

    /**
     * 重启 Widget
     */
    void restart();
}
