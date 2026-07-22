package com.bingbaihanji.luca.core.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * IDEA 风格底部工具面板。
 * <p>
 * 顶部是标签栏（来自 {@code BottomPanelApi} 注入的多个标签页），
 * 右侧有隐藏按钮(▼)。点击标签切换内容区。
 */
public class IdeBottomPanel extends BorderPane {

    private final HBox tabBar = new HBox(0);
    private final StackPane contentArea = new StackPane();
    private final List<TabEntry> entries = new ArrayList<>();
    private final Runnable onHideRequest;
    private int activeIndex = -1;
    public IdeBottomPanel(Runnable onHideRequest) {
        this.onHideRequest = onHideRequest;
        buildView();
    }

    private void buildView() {
        setTop(buildHeader());
        contentArea.getStyleClass().add("ide-tool-window-content");
        setCenter(contentArea);
        getStyleClass().add("ide-tool-window");
        setMinHeight(80);
    }

    private HBox buildHeader() {
        tabBar.setAlignment(Pos.CENTER_LEFT);
        tabBar.getStyleClass().add("ide-bottom-tab-bar");
        tabBar.setMinHeight(30);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button hideBtn = new Button("▼");
        hideBtn.getStyleClass().add("ide-tool-window-action-btn");
        hideBtn.setTooltip(new Tooltip("隐藏面板"));
        hideBtn.setFocusTraversable(false);
        hideBtn.setOnAction(e -> {
            if (onHideRequest != null) {
                onHideRequest.run();
            }
        });

        HBox header = new HBox(0, tabBar, spacer, hideBtn);
        HBox.setHgrow(tabBar, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 6, 0, 0));
        header.setMinHeight(30);
        header.setPrefHeight(30);
        header.getStyleClass().add("ide-bottom-panel-header");
        return header;
    }

    /**
     * 添加一个标签页（BottomPanelApi 注入）。
     */
    public void addTab(String title, Node content) {
        int idx = entries.size();
        entries.add(new TabEntry(title, content));

        Label tab = new Label(title);
        tab.getStyleClass().add("ide-bottom-tab");
        tab.setPadding(new Insets(4, 14, 4, 14));
        tab.setMinHeight(30);
        final int tabIdx = idx;
        tab.setOnMouseClicked(e -> setActiveIndex(tabIdx));
        tabBar.getChildren().add(tab);

        if (activeIndex < 0) {
            setActiveIndex(0);
        }
    }

    public boolean hasTabs() {
        return !entries.isEmpty();
    }

    private void setActiveIndex(int idx) {
        activeIndex = idx;
        for (int i = 0; i < tabBar.getChildren().size(); i++) {
            Node n = tabBar.getChildren().get(i);
            n.getStyleClass().removeAll("ide-bottom-tab-active");
            if (i == idx) {
                n.getStyleClass().add("ide-bottom-tab-active");
            }
        }
        contentArea.getChildren().setAll(entries.get(idx).content());
    }

    private record TabEntry(String title, Node content) {
    }
}
