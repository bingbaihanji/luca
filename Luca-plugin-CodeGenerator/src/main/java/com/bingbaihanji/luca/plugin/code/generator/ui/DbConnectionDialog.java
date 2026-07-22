package com.bingbaihanji.luca.plugin.code.generator.ui;

import com.bingbaihanji.luca.plugin.code.generator.db.DbConfig;
import com.bingbaihanji.luca.plugin.code.generator.db.DbMetadataService;
import com.bingbaihanji.luca.plugin.code.generator.util.FXTools;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.Connection;

/**
 * 数据库连接配置对话框：模态窗口，提供数据库类型选择、连接参数输入、高级 JDBC 配置。
 * 支持「测试连接」与「连接并浏览表」两个动作。
 */
public class DbConnectionDialog extends Stage {

    // 数据库元数据服务，用于测试连接与建立连接
    private final DbMetadataService dbService = new DbMetadataService();
    // ── 表单字段 ─────────────────────────────────────────────
    private final ComboBox<String> dbTypeCombo;   // 数据库类型下拉框：MySQL / PostgreSQL / 自定义
    private final TextField hostField;            // 主机地址
    private final TextField portField;            // 端口号
    private final TextField dbNameField;          // 数据库名
    private final TextField userField;            // 用户名
    private final PasswordField passField;        // 密码
    private final TextField urlField;             // JDBC URL（MySQL/PostgreSQL 下自动填充，自定义可编辑）
    private final TextField driverField;          // 驱动类名
    private final Label statusLabel;              // 底部状态提示标签（连接成功/失败信息）

    @SuppressWarnings("deprecation")
    public DbConnectionDialog() {
        initModality(Modality.APPLICATION_MODAL);   // 设置为模态对话框，阻塞父窗口交互
        setTitle("代码生成器");
        initStyle(StageStyle.EXTENDED); // 标题栏设置

        // ── Header bar ──────────────────────────────────────
        Label headerTitle = new Label("数据库连接配置");
        headerTitle.getStyleClass().add("label-title");


        HeaderBar headerBar = FXTools.createpopUpBoxHeaderBar(headerTitle);


        // ── Fields ──────────────────────────────────────────
        dbTypeCombo = new ComboBox<>();
        dbTypeCombo.getItems().addAll("MySQL", "PostgreSQL", "自定义");
        dbTypeCombo.setValue("MySQL");
        dbTypeCombo.setMaxWidth(Double.MAX_VALUE);

        hostField = styledField("localhost");
        portField = styledField("3306");
        dbNameField = styledField(null);
        dbNameField.setPromptText("数据库名称");
        userField = styledField("root");
        passField = new PasswordField();
        passField.setMaxWidth(Double.MAX_VALUE);
        urlField = styledField(null);
        urlField.setPromptText("自动生成，或手动输入");
        driverField = styledField("com.mysql.cj.jdbc.Driver");

        // ── Section 1: 连接参数 ──────────────────────────────
        GridPane basicGrid = new GridPane();
        basicGrid.setHgap(10);
        basicGrid.setVgap(10);
        configureColumns(basicGrid, 80);

        basicGrid.add(formLabel("数据库类型:"), 0, 0);
        basicGrid.add(dbTypeCombo, 1, 0, 3, 1);

        HBox hostRow = new HBox(8, hostField, formLabel("端口:"), portField);
        HBox.setHgrow(hostField, Priority.ALWAYS);
        hostRow.setAlignment(Pos.CENTER_LEFT);
        basicGrid.add(formLabel("Host:"), 0, 1);
        basicGrid.add(hostRow, 1, 1, 3, 1);

        basicGrid.add(formLabel("数据库名:"), 0, 2);
        basicGrid.add(dbNameField, 1, 2, 3, 1);
        basicGrid.add(formLabel("用户名:"), 0, 3);
        basicGrid.add(userField, 1, 3, 3, 1);
        basicGrid.add(formLabel("密码:"), 0, 4);
        basicGrid.add(passField, 1, 4, 3, 1);

        VBox section1 = sectionCard("连接参数", basicGrid);

        // ── Section 2: 高级配置 ──────────────────────────────
        GridPane advGrid = new GridPane();
        advGrid.setHgap(10);
        advGrid.setVgap(10);
        configureColumns(advGrid, 80);
        advGrid.add(formLabel("JDBC URL:"), 0, 0);
        advGrid.add(urlField, 1, 0);
        advGrid.add(formLabel("Driver 类:"), 0, 1);
        advGrid.add(driverField, 1, 1);

        VBox section2 = sectionCard("高级配置 (JDBC)", advGrid);

        // ── Status label ─────────────────────────────────────
        statusLabel = new Label();
        statusLabel.setVisible(false);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        // ── Content area ─────────────────────────────────────
        VBox content = new VBox(12, section1, section2, statusLabel);
        content.setPadding(new Insets(16, 20, 12, 20));
        content.setStyle("-fx-background-color: #2b2b2b;");

        // ── Footer buttons ───────────────────────────────────
        Button testBtn = new Button("测试连接");
        testBtn.getStyleClass().addAll("button", "btn-ghost");

        Button connectBtn = new Button("连接并浏览表  ›");
        connectBtn.getStyleClass().addAll("button", "btn-primary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(8, testBtn, spacer, connectBtn);
        footer.getStyleClass().add("dialog-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        // ── Root ─────────────────────────────────────────────
        VBox root = new VBox(headerBar, content, footer);
        root.setStyle("-fx-background-color: #2b2b2b;");

        Scene scene = new Scene(root, 520, -1);   // 宽度固定 520，高度自适应内容
        loadCss(scene);
        setScene(scene);
        sizeToScene();
        setResizable(false);   // 禁止用户调整窗口大小

        // ── Wire listeners ────────────────────────────────────
        // Host / Port / 数据库名 变化时自动更新 JDBC URL
        ChangeListener<String> urlUpdater = (o, ov, nv) -> updateUrl();
        hostField.textProperty().addListener(urlUpdater);
        portField.textProperty().addListener(urlUpdater);
        dbNameField.textProperty().addListener(urlUpdater);
        // 数据库类型切换时更新默认端口、驱动、URL 可编辑状态
        dbTypeCombo.setOnAction(e -> onDbTypeChanged());
        onDbTypeChanged();   // 初始化时执行一次，确保 URL 与驱动匹配当前类型

        testBtn.setOnAction(e -> testConnection());
        connectBtn.setOnAction(e -> connectAndBrowse());
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * 创建统一样式的文本输入框
     */
    private TextField styledField(String text) {
        TextField f = text != null ? new TextField(text) : new TextField();
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    /**
     * 创建表单标签：右对齐，应用 CSS 样式
     */
    private Label formLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("label", "label-form");
        l.setAlignment(Pos.CENTER_RIGHT);
        return l;
    }

    /**
     * 配置 GridPane 列约束：第一列固定宽度右对齐，第二列自动拉伸
     */
    private void configureColumns(GridPane grid, double labelWidth) {
        ColumnConstraints lc = new ColumnConstraints(labelWidth);
        lc.setHalignment(HPos.RIGHT);
        ColumnConstraints fc = new ColumnConstraints();
        fc.setHgrow(Priority.ALWAYS);
        fc.setFillWidth(true);
        grid.getColumnConstraints().addAll(lc, fc);
    }

    /**
     * 构建带标题的卡片区域（Section）
     */
    private VBox sectionCard(String title, Node content) {
        Label lbl = new Label(title);
        lbl.getStyleClass().add("section-header");
        VBox card = new VBox(8, lbl, content);
        card.getStyleClass().add("section-card");
        return card;
    }

    /**
     * 加载暗色主题 CSS
     */
    private void loadCss(Scene scene) {
        var url = getClass().getResource("/code-generator.css");
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }

    // ── Logic ─────────────────────────────────────────────────

    /**
     * 数据库类型切换回调：
     * MySQL -> 端口 3306、驱动固定、URL 自动生成且不可编辑
     * PostgreSQL -> 端口 5432、驱动固定、URL 自动生成且不可编辑
     * 自定义 -> URL 与驱动均可手动编辑
     */
    private void onDbTypeChanged() {
        String type = dbTypeCombo.getValue();
        if ("MySQL".equals(type)) {
            portField.setText("3306");
            driverField.setText("com.mysql.cj.jdbc.Driver");
            urlField.setEditable(false);
            driverField.setEditable(false);
        } else if ("PostgreSQL".equals(type)) {
            portField.setText("5432");
            driverField.setText("org.postgresql.Driver");
            urlField.setEditable(false);
            driverField.setEditable(false);
        } else {
            urlField.setEditable(true);
            driverField.setEditable(true);
        }
        updateUrl();
    }

    /**
     * 根据当前数据库类型、主机、端口、库名自动拼接 JDBC URL
     */
    private void updateUrl() {
        String type = dbTypeCombo.getValue();
        String host = hostField.getText();
        String port = portField.getText();
        String db = dbNameField.getText();
        if ("MySQL".equals(type)) {
            urlField.setText("jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8");
        } else if ("PostgreSQL".equals(type)) {
            urlField.setText("jdbc:postgresql://" + host + ":" + port + "/" + db);
        }
    }

    /**
     * 从表单字段构建 DbConfig 对象
     */
    private DbConfig buildConfig() {
        DbConfig c = new DbConfig();
        c.setDriverClass(driverField.getText().trim());
        c.setUrl(urlField.getText().trim());
        c.setUsername(userField.getText().trim());
        c.setPassword(passField.getText());
        // PostgreSQL 的 schemaName 固定为 "public"，MySQL 使用数据库名
        c.setSchemaName("PostgreSQL".equals(dbTypeCombo.getValue())
                ? "public" : dbNameField.getText().trim());
        return c;
    }

    /**
     * 在 JavaFX 线程更新底部状态标签的文本与颜色
     */
    private void setStatus(String msg, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            statusLabel.setVisible(true);
            sizeToScene();   // 窗口高度随状态文本自适应
        });
    }

    /**
     * 测试连接：后台线程建立连接后立即关闭，UI 线程显示结果
     */
    private void testConnection() {
        setStatus("正在测试连接...", "#9da0a8");
        DbConfig config = buildConfig();
        Task<Connection> task = new Task<>() {
            @Override
            protected Connection call() throws Exception {
                return dbService.connect(config);   // 后台建立 JDBC 连接
            }

            @Override
            protected void succeeded() {
                try {
                    getValue().close();             // 测试成功后立即关闭连接，仅验证连通性
                } catch (Exception ignored) {
                }
                setStatus("✓  连接成功", "#6bbd6b");
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                setStatus("✗  " + (ex != null ? ex.getMessage() : "连接失败"), "#e05c5c");
            }
        };
        new Thread(task).start();
    }

    /**
     * 连接并浏览：后台线程建立连接，成功后关闭当前对话框并打开表浏览窗口
     */
    private void connectAndBrowse() {
        setStatus("正在连接...", "#9da0a8");
        DbConfig config = buildConfig();
        Task<Connection> task = new Task<>() {
            @Override
            protected Connection call() throws Exception {
                return dbService.connect(config);
            }

            @Override
            protected void succeeded() {
                Connection conn = getValue();
                Platform.runLater(() -> {
                    close();   // 关闭连接配置对话框
                    new TableBrowserDialog(conn, config, dbService).show();   // 打开表浏览与代码生成向导
                });
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                setStatus("✗  " + (ex != null ? ex.getMessage() : "连接失败"), "#e05c5c");
            }
        };
        new Thread(task).start();
    }
}
