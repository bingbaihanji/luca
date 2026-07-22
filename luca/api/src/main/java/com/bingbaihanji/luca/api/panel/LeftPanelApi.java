package com.bingbaihanji.luca.api.panel;

import com.bingbaihanji.sunsen.api.annotation.ExtensionPoint;
import javafx.scene.Node;

@ExtensionPoint(id = "leftPanel:content", description = "左侧面板内容扩展")
public interface LeftPanelApi {
    /**
     * 返回插件提供的左侧面板内容节点。
     * 宿主在所有插件 start 完成后调用此方法注入内容。
     */
    Node getContent();

    /**
     * 面板标题，显示在工具窗口标题栏
     */
    default String getPanelTitle() {
        return "项目";
    }

    /**
     * 活动栏图标（Unicode 字符）
     */
    default String getActivityIcon() {
        return "☰";
    }
}
