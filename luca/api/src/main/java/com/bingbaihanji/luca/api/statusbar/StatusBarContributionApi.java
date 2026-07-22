package com.bingbaihanji.luca.api.statusbar;

import com.bingbaihanji.sunsen.api.annotation.ExtensionPoint;
import javafx.scene.Node;

/**
 * 状态栏组件扩展点。
 * 插件可向底部状态栏的左侧或右侧注入自定义 Node（如标签、按钮）。
 */
@ExtensionPoint(id = "statusBar:widget", description = "状态栏组件扩展")
public interface StatusBarContributionApi {
    /**
     * 返回要插入状态栏的 JavaFX 节点
     */
    Node getWidget();

    /**
     * true 则插入状态栏右侧区域，false 则插入左侧区域
     */
    default boolean isRightAligned() {
        return true;
    }
}
