package com.bingbaihanji.luca.api.menu;

import javafx.scene.Node;

/**
 * 插件通过此接口操作宿主的中心标签页面板，避免 api 模块直接依赖 core。
 */
public interface TabPaneAccessor {

    /**
     * 若 id 对应的标签页不存在则创建并激活，否则仅激活（聚焦）。
     */
    void addOrSelect(String id, String title, Node content);

    /** 返回当前激活标签的 id，不存在时返回 null。 */
    String getActiveId();

    /** 返回指定 id 的标签页是否已存在。 */
    boolean containsTab(String id);
}
