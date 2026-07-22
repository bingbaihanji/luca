package com.bingbaihanji.luca.core.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * IDEA 风格底部状态栏（固定 22px 高）。
 * <p>
 * 左侧：状态消息；右侧：插件通过 {@code StatusBarContributionApi} 注入的组件。
 */
public class IdeStatusBar extends HBox {

    private final HBox leftArea = new HBox(4);
    private final HBox rightArea = new HBox(4);
    private final Label statusLabel;

    public IdeStatusBar() {
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 6, 0, 6));
        setMinHeight(22);
        setPrefHeight(22);
        setMaxHeight(22);
        getStyleClass().add("ide-status-bar");

        statusLabel = new Label("就绪");
        statusLabel.getStyleClass().add("ide-status-bar-widget");

        leftArea.setAlignment(Pos.CENTER_LEFT);
        leftArea.getChildren().add(statusLabel);

        rightArea.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(leftArea, spacer, rightArea);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void addLeftWidget(Node widget) {
        leftArea.getChildren().add(widget);
    }

    public void addRightWidget(Node widget) {
        rightArea.getChildren().add(widget);
    }
}
