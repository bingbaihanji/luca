package com.bingbaihanji.luca.api.panel;

import com.bingbaihanji.sunsen.api.annotation.ExtensionPoint;
import javafx.scene.Node;

@ExtensionPoint(id = "rightPanel:content", description = "右侧面板内容扩展")
public interface RightPanelApi {
    Node getContent();

    default String getPanelTitle() {
        return "右侧面板";
    }

    default String getActivityIcon() {
        return "◈";
    }
}
