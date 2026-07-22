package com.bingbaihanji.luca.core.ui.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * IDEA 风格工具窗口。
 * <p>
 * 包含：
 * <ul>
 *   <li>标题栏：标题文字 + 浮动按钮(⊟) + 隐藏按钮(×)</li>
 *   <li>内容区：由外部注入的任意 Node</li>
 *   <li>拖拽浮动：拖拽标题栏超过阈值后自动弹出独立 Stage；关闭 Stage 时自动回停</li>
 * </ul>
 */
public class IdeToolWindow extends BorderPane {

    private static final double DRAG_THRESHOLD = 12.0;
    private final String title;
    private final BooleanProperty floating = new SimpleBooleanProperty(false);
    private final Runnable onHideRequest;
    private Node content;
    private Stage floatingStage = null;
    // 拖拽浮动的临时状态
    private double dragStartX, dragStartY;
    private boolean dragDetected = false;

    public IdeToolWindow(String title, Node content, Runnable onHideRequest) {
        this.title = title;
        this.onHideRequest = onHideRequest;
        this.content = (content != null) ? content : makePlaceholder("暂无内容");
        buildView();
    }

    private static Node makePlaceholder(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("ide-placeholder-label");
        StackPane pane = new StackPane(lbl);
        pane.getStyleClass().add("ide-tool-window-content");
        return pane;
    }

    private void buildView() {
        setTop(buildHeader());
        setCenter(wrapContent(this.content));
        getStyleClass().add("ide-tool-window");
        setMinWidth(80);
    }

    private static StackPane wrapContent(Node node) {
        StackPane wrapper = new StackPane(node);
        wrapper.getStyleClass().add("ide-tool-window-content");
        StackPane.setAlignment(node, javafx.geometry.Pos.TOP_LEFT);
        return wrapper;
    }

    private HBox buildHeader() {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("ide-tool-window-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button floatBtn = makeActionButton("⊟", "在浮动窗口中打开", this::toggleFloat);
        Button hideBtn = makeActionButton("×", "隐藏面板", () -> {
            if (onHideRequest != null) {
                onHideRequest.run();
            }
        });

        // 防止按钮上的 press 事件冒泡到 header 触发拖拽
        floatBtn.setOnMousePressed(Event::consume);
        hideBtn.setOnMousePressed(Event::consume);

        HBox header = new HBox(4, titleLabel, spacer, floatBtn, hideBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 6, 0, 8));
        header.setMinHeight(30);
        header.setPrefHeight(30);
        header.getStyleClass().add("ide-tool-window-header");

        // 拖拽标题栏 → 浮动
        header.setOnMousePressed(e -> {
            dragStartX = e.getScreenX();
            dragStartY = e.getScreenY();
            dragDetected = false;
        });
        header.setOnMouseDragged(e -> {
            if (!dragDetected && !floating.get()) {
                double dx = Math.abs(e.getScreenX() - dragStartX);
                double dy = Math.abs(e.getScreenY() - dragStartY);
                if (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD) {
                    dragDetected = true;
                    detachToFloat();
                }
            }
            if (dragDetected && floatingStage != null) {
                floatingStage.setX(e.getScreenX() - floatingStage.getWidth() / 2);
                floatingStage.setY(e.getScreenY() - 15);
            }
        });

        return header;
    }

    // ── 浮动 / 回停 ──────────────────────────────────────────────

    private Button makeActionButton(String text, String tip, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("ide-tool-window-action-btn");
        btn.setTooltip(new Tooltip(tip));
        btn.setFocusTraversable(false);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void toggleFloat() {
        if (floating.get()) {
            dockBack();
        } else {
            detachToFloat();
        }
    }

    @SuppressWarnings("deprecation")
    private void detachToFloat() {
        floating.set(true);
        Node floatedContent = this.content;
        setCenter(makePlaceholder("已在浮动窗口中打开"));

        floatingStage = new Stage();
        floatingStage.setTitle(title);
        floatingStage.initStyle(StageStyle.EXTENDED);
        HeaderBar headerBar = new HeaderBar();

        BorderPane root = new BorderPane(floatedContent);
        root.setTop(headerBar);
        root.getStyleClass().add("ide-tool-window");

        Scene scene = new Scene(root, 520, 420);
        if (getScene() != null) {
            scene.getStylesheets().addAll(getScene().getStylesheets());
        }
        floatingStage.setScene(scene);
        floatingStage.show();
        floatingStage.setOnCloseRequest(e -> dockBack());

        if (onHideRequest != null) {
            onHideRequest.run();
        }
    }

    // ── 公共 API ─────────────────────────────────────────────────

    private void dockBack() {
        if (floatingStage != null) {
            floatingStage.setOnCloseRequest(null);
            // 取回内容节点（从浮动窗口 wrapper 里取）
            Node floatRoot = ((BorderPane) floatingStage.getScene().getRoot()).getCenter();
            if (floatRoot instanceof StackPane sp && !sp.getChildren().isEmpty()) {
                this.content = sp.getChildren().getFirst();
            } else if (floatRoot != null) {
                this.content = floatRoot;
            }
            floatingStage.close();
            floatingStage = null;
        }
        setCenter(wrapContent(this.content));
        floating.set(false);
    }

    public void setContent(Node newContent) {
        this.content = (newContent != null) ? newContent : makePlaceholder("暂无内容");
        if (!floating.get()) {
            setCenter(wrapContent(this.content));
        } else if (floatingStage != null) {
            ((BorderPane) floatingStage.getScene().getRoot()).setCenter(wrapContent(this.content));
        }
    }

    public boolean isFloating() {
        return floating.get();
    }

    // ── 辅助 ─────────────────────────────────────────────────────

    public BooleanProperty floatingProperty() {
        return floating;
    }
}
