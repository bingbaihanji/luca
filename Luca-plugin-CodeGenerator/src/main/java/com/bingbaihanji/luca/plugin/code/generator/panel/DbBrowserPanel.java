package com.bingbaihanji.luca.plugin.code.generator.panel;

import com.bingbaihanji.luca.plugin.code.generator.db.DbConfig;
import com.bingbaihanji.luca.plugin.code.generator.db.DbMetadataService;
import com.bingbaihanji.luca.plugin.code.generator.db.TableMeta;
import com.bingbaihanji.luca.plugin.code.generator.ui.DbConnectionDialog;
import com.bingbaihanji.luca.plugin.code.generator.ui.TableBrowserDialog;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.util.List;

/**
 * 数据库浏览器面板：显示在 luca 框架左侧边栏，提供数据库连接与表列表浏览功能。
 * 外观风格模仿 IDEA（深色主题、紧凑布局、圆角标签）。
 */
public class DbBrowserPanel extends VBox {

    // 数据库元数据服务，负责连接与表结构查询
    private final DbMetadataService dbService = new DbMetadataService();
    // ── UI regions ────────────────────────────────────────────
    private final StackPane contentArea;    // 内容区域：可切换显示「未连接提示 / 加载中 / 表列表」
    private final Label tableCountLabel;    // 右上角显示表数量的标签
    // ── State ─────────────────────────────────────────────────
    private Connection activeConnection;    // 当前活跃的数据库连接
    private DbConfig activeConfig;          // 当前连接的配置

    public DbBrowserPanel() {
        setSpacing(0);
        setStyle("-fx-background-color: #2b2b2b;");   // 整体深色背景

        // ── Panel title bar (matches IDEA style) ─────────
        Label title = new Label("数据库");
        title.setStyle("-fx-font-size: 13px; -fx-text-fill: #bbbbbb; -fx-font-weight: bold;");

        // 表数量标签：初始隐藏，连接成功后显示
        tableCountLabel = new Label("");
        tableCountLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9da0a8; "
                + "-fx-background-color: #3c3f41; -fx-background-radius: 8; -fx-padding: 1 7 1 7;");
        tableCountLabel.setVisible(false);

        // 连接按钮：自定义深色样式，带 hover 效果
        Button connectBtn = new Button("连接");
        connectBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9da0a8;"
                        + "-fx-border-color: #4b4b4b; -fx-border-radius: 3; -fx-background-radius: 3;"
                        + "-fx-font-size: 12px; -fx-padding: 2 8 2 8; -fx-cursor: hand;");
        connectBtn.setOnMouseEntered(e -> connectBtn.setStyle(
                "-fx-background-color: #3c3f41; -fx-text-fill: #dfe1e5;"
                        + "-fx-border-color: #5c5c5c; -fx-border-radius: 3; -fx-background-radius: 3;"
                        + "-fx-font-size: 12px; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
        connectBtn.setOnMouseExited(e -> connectBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9da0a8;"
                        + "-fx-border-color: #4b4b4b; -fx-border-radius: 3; -fx-background-radius: 3;"
                        + "-fx-font-size: 12px; -fx-padding: 2 8 2 8; -fx-cursor: hand;"));
        connectBtn.setOnAction(e -> openConnectionDialog());   // 点击打开数据库连接对话框

        // 弹性区域，将连接按钮推到右侧
        Region flex = new Region();
        HBox.setHgrow(flex, Priority.ALWAYS);
        HBox titleBar = new HBox(6, title, tableCountLabel, flex, connectBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(8, 10, 8, 12));
        titleBar.setStyle("-fx-background-color: #3c3f41; "
                + "-fx-border-color: transparent transparent #4b4b4b transparent;");

        // ── Content area (placeholder / table list) ──────
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #2b2b2b;");
        VBox.setVgrow(contentArea, Priority.ALWAYS);   // 垂直方向自动占满剩余空间
        showDisconnectedState();                        // 初始显示「未连接」提示

        getChildren().addAll(titleBar, contentArea);
    }

    // ── States ────────────────────────────────────────────────

    /**
     * 显示「未连接数据库」占位状态
     */
    private void showDisconnectedState() {
        Label hint = new Label("未连接数据库");
        hint.setStyle("-fx-text-fill: #6b6e75; -fx-font-size: 12px;");
        Label hint2 = new Label("点击右上角「连接」");
        hint2.setStyle("-fx-text-fill: #6b6e75; -fx-font-size: 11px;");
        VBox placeholder = new VBox(4, hint, hint2);
        placeholder.setAlignment(Pos.CENTER);
        contentArea.getChildren().setAll(placeholder);
    }

    /**
     * 显示「加载中」状态（异步查询表列表时展示）
     */
    private void showLoadingState() {
        ProgressIndicator pi = new ProgressIndicator(-1);
        pi.setMaxSize(28, 28);
        pi.setStyle("-fx-progress-color: #4b8bbf;");
        Label label = new Label("加载表列表...");
        label.setStyle("-fx-text-fill: #9da0a8; -fx-font-size: 12px;");
        VBox loading = new VBox(8, pi, label);
        loading.setAlignment(Pos.CENTER);
        contentArea.getChildren().setAll(loading);
    }

    /**
     * 显示表列表：构建 ListView，绑定双击打开表结构浏览对话框。
     *
     * @param tables 从数据库查询到的表元数据列表
     */
    private void showTableList(List<TableMeta> tables) {
        tableCountLabel.setText(tables.size() + " 张");
        tableCountLabel.setVisible(true);

        ListView<String> listView = new ListView<>();
        listView.setStyle("-fx-background-color: #2b2b2b; -fx-border-color: transparent;");

        for (TableMeta tm : tables) {
            listView.getItems().add(tm.getTableName());
        }

        // 自定义单元格样式：深色文本、紧凑内边距
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                    return;
                }
                setText("  " + item);
                setStyle("-fx-text-fill: #dfe1e5; -fx-font-size: 12px; "
                        + "-fx-padding: 4 6 4 6; -fx-cursor: hand;");
            }
        });
        // 双击打开 TableBrowserDialog 浏览表结构（随后释放当前 connection 引用，避免重复关闭）
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && activeConnection != null) {
                new TableBrowserDialog(activeConnection, activeConfig, dbService).show();
                activeConnection = null;
            }
        });

        contentArea.getChildren().setAll(listView);
    }

    // ── Connection flow ───────────────────────────────────────

    /**
     * 打开数据库连接对话框
     */
    private void openConnectionDialog() {
        DbConnectionDialog dialog = new DbConnectionDialog();
        dialog.show();
    }

    /**
     * 连接成功后的回调：切换到加载状态，在后台线程拉取表列表，完成后刷新 UI。
     *
     * @param conn   已建立的数据库连接
     * @param config 连接配置（含 schemaName）
     */
    void onConnected(Connection conn, DbConfig config) {
        this.activeConnection = conn;
        this.activeConfig = config;
        showLoadingState();

        Task<List<TableMeta>> task = new Task<>() {
            @Override
            protected List<TableMeta> call() throws Exception {
                return dbService.listTables(conn, config.getSchemaName());   // 后台查询表列表
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> showTableList(getValue()));          // 成功后在 JavaFX 线程刷新列表
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showDisconnectedState();
                    tableCountLabel.setVisible(false);
                });
            }
        };
        new Thread(task).start();   // 启动后台线程，避免阻塞 UI
    }
}
