package com.bingbaihanji.luca.api.activity;

import com.bingbaihanji.sunsen.api.annotation.ExtensionPoint;
import javafx.scene.Node;

/**
 * 活动栏自定义图标项扩展点。
 * 插件可向左侧/右侧活动栏注入图标按钮，点击后显示对应工具窗口。
 */
@ExtensionPoint(id = "activityBar:item", description = "活动栏自定义项扩展")
public interface ActivityBarItemApi {
    /**
     * 唯一标识符
     */
    String getId();

    /**
     * 图标（Unicode 字符，如 ☰ ⚙ 等）
     */
    String getIcon();

    /**
     * 悬停提示文本
     */
    String getTooltip();

    /**
     * 工具窗口内容
     */
    Node getContent();

    /**
     * 工具窗口标题栏显示的名称
     */
    String getPanelTitle();

    /**
     * 显示在哪个活动栏
     */
    default Side getSide() {
        return Side.LEFT;
    }

    /**
     * 排序权重，数值越小越靠前
     */
    default int getOrder() {
        return 100;
    }

    enum Side {LEFT, RIGHT, BOTTOM}
}
