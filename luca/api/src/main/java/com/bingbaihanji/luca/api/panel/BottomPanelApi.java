package com.bingbaihanji.luca.api.panel;

import com.bingbaihanji.sunsen.api.annotation.ExtensionPoint;
import javafx.scene.Node;

/**
 * 底部工具窗口标签页扩展点。
 * 每个实现对应底部面板中的一个标签页（如终端、运行、问题等）。
 */
@ExtensionPoint(id = "bottomPanel:tab", description = "底部面板标签页扩展")
public interface BottomPanelApi {
    /**
     * 标签页的内容节点
     */
    Node getContent();

    /**
     * 标签页显示标题
     */
    String getTabTitle();

    /**
     * 标签页图标（Unicode 字符，可选）
     */
    default String getTabIcon() {
        return "";
    }
}
