package com.bingbaihanji.luca.core.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IDEA 风格编辑器标签页面板。
 * <p>
 * 功能：
 * <ul>
 *   <li>标签栏：每个标签含文件名 + 悬停时可见的关闭按钮(×)</li>
 *   <li>点击切换内容；鼠标中键或单击关闭按钮关闭标签</li>
 *   <li>右键上下文菜单：关闭 / 关闭其他 / 关闭全部</li>
 *   <li>无标签时显示欢迎占位界面</li>
 *   <li>同一 id 重复调用 {@link #addTab} 时直接激活已有标签</li>
 * </ul>
 */
public class LucaTabPane extends BorderPane {

    private final HBox tabBar = new HBox(0);
    private final StackPane contentArea = new StackPane();
    private final List<TabEntry> tabs = new ArrayList<>();
    private final Map<String, HBox> tabNodeMap = new LinkedHashMap<>();
    private String activeId = null;

    public LucaTabPane() {
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.getStyleClass().add("ide-tab-bar");
        tabBar.setMinHeight(32);

        ScrollPane tabScroll = new ScrollPane(tabBar);
        tabScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tabScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tabScroll.setFitToHeight(true);
        tabScroll.setMinHeight(32);
        tabScroll.setPrefHeight(32);
        tabScroll.setMaxHeight(32);
        tabScroll.getStyleClass().add("ide-tab-scroll");

        contentArea.getStyleClass().add("ide-editor-content");

        setTop(tabScroll);
        setCenter(contentArea);
        getStyleClass().add("ide-tab-pane");

        showWelcome();
    }

    public void addTab(String id, String title, Node content) {
        if (tabNodeMap.containsKey(id)) {
            setActiveTab(id);
            return;
        }
        tabs.add(new TabEntry(id, title, content));
        HBox tabNode = buildTabNode(id, title);
        tabBar.getChildren().add(tabNode);
        tabNodeMap.put(id, tabNode);
        setActiveTab(id);
    }

    // ── 公共 API ─────────────────────────────────────────────────

    public boolean containsTab(String id) {
        return tabNodeMap.containsKey(id);
    }

    public void closeTab(String id) {
        TabEntry entry = tabs.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
        if (entry == null) {
            return;
        }
        int idx = tabs.indexOf(entry);
        tabs.remove(entry);
        HBox node = tabNodeMap.remove(id);
        if (node != null) {
            tabBar.getChildren().remove(node);
        }

        if (id.equals(activeId)) {
            if (!tabs.isEmpty()) {
                setActiveTab(tabs.get(Math.min(idx, tabs.size() - 1)).id());
            } else {
                activeId = null;
                showWelcome();
            }
        }
    }

    public void setActiveTab(String id) {
        activeId = id;
        tabNodeMap.forEach((tabId, node) -> {
            node.getStyleClass().removeAll("ide-tab-active");
            if (tabId.equals(id)) {
                node.getStyleClass().add("ide-tab-active");
            }
        });
        tabs.stream().filter(t -> t.id().equals(id)).findFirst()
                .ifPresent(t -> contentArea.getChildren().setAll(t.content()));
    }

    public String getActiveId() {
        return activeId;
    }

    private HBox buildTabNode(String id, String title) {
        Label label = new Label(title);
        label.getStyleClass().add("ide-tab-label");
        label.setMaxWidth(160);

        Label closeBtn = new Label("×");
        closeBtn.getStyleClass().add("ide-tab-close");
        closeBtn.setOnMouseClicked(e -> {
            e.consume();
            closeTab(id);
        });
        closeBtn.setOnMousePressed(e -> e.consume());

        HBox tab = new HBox(4, label, closeBtn);
        tab.setAlignment(Pos.CENTER_LEFT);
        tab.setPadding(new Insets(0, 6, 0, 12));
        tab.setMinHeight(32);
        tab.setPrefHeight(32);
        tab.getStyleClass().add("ide-tab");

        tab.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                closeTab(id);
            } else if (e.getButton() == MouseButton.PRIMARY) {
                setActiveTab(id);
            } else if (e.getButton() == MouseButton.SECONDARY) {
                buildContextMenu(id).show(tab,
                        tab.localToScreen(tab.getBoundsInLocal()).getMinX(),
                        tab.localToScreen(tab.getBoundsInLocal()).getMaxY());
            }
        });

        return tab;
    }

    // ── 构建标签节点 ──────────────────────────────────────────────

    private ContextMenu buildContextMenu(String id) {
        MenuItem close = new MenuItem("关闭");
        close.setOnAction(e -> closeTab(id));

        MenuItem closeOthers = new MenuItem("关闭其他标签页");
        closeOthers.setOnAction(e -> {
            List<String> others = tabs.stream().map(TabEntry::id)
                    .filter(tid -> !tid.equals(id)).toList();
            others.forEach(this::closeTab);
        });

        MenuItem closeAll = new MenuItem("关闭全部标签页");
        closeAll.setOnAction(e -> {
            List<String> all = tabs.stream().map(TabEntry::id).toList();
            all.forEach(this::closeTab);
        });

        ContextMenu menu = new ContextMenu(close, new SeparatorMenuItem(), closeOthers, closeAll);
        menu.getStyleClass().add("ide-context-menu");
        return menu;
    }

    private void showWelcome() {
        Label hint = new Label("在左侧面板中选择文件，或通过插件打开编辑器");
        hint.getStyleClass().add("ide-placeholder-label");
        contentArea.getChildren().setAll(hint);
    }

    private record TabEntry(String id, String title, Node content) {
    }
}
