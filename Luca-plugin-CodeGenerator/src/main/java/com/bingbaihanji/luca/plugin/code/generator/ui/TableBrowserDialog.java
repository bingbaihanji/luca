package com.bingbaihanji.luca.plugin.code.generator.ui;

import com.bingbaihanji.luca.plugin.code.generator.db.ColumnMeta;
import com.bingbaihanji.luca.plugin.code.generator.db.DbConfig;
import com.bingbaihanji.luca.plugin.code.generator.db.DbMetadataService;
import com.bingbaihanji.luca.plugin.code.generator.db.TableMeta;
import com.bingbaihanji.luca.plugin.code.generator.util.FXTools;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 表浏览与选择对话框：模态窗口，左右分栏布局。
 * 左侧展示数据库表列表（支持搜索、全选、反选），右侧展示选中表的列结构详情。
 * 底部提供「配置并生成代码」按钮，打开 GeneratorConfigDialog 执行代码生成。
 */
public class TableBrowserDialog extends Stage {

    // ── 外部传入的上下文 ─────────────────────────────────────
    private final Connection connection;     // 数据库连接
    private final DbConfig dbConfig;         // 数据库配置
    private final DbMetadataService dbService;

    // ── 左侧表列表相关 ───────────────────────────────────────
    private final ObservableList<TableMeta> allTables = FXCollections.observableArrayList();          // 全部表数据
    private final FilteredList<TableMeta> filteredTables = new FilteredList<>(allTables, t -> true);   // 带搜索过滤的表数据
    private final ListView<TableMeta> tableListView = new ListView<>(filteredTables);                  // 表列表视图
    private final Set<TableMeta> selectedSet = new LinkedHashSet<>();                                  // 用户勾选的表集合（保持插入顺序）

    // ── 右侧列结构相关 ───────────────────────────────────────
    private final TableView<ColumnMeta> columnTableView = new TableView<>();   // 列详情表格

    // ── 底部状态栏相关 ───────────────────────────────────────
    private final Label tableCountLabel = new Label("共 0 张");
    private final Label selectionLabel = new Label("已选 0 张");
    private final Label columnHeaderLabel = new Label("请在左侧选中一张表查看列结构");
    private final Button generateBtn;
    private final ProgressIndicator loadingIndicator = new ProgressIndicator(-1);   // 加载表列表时的旋转指示器

    @SuppressWarnings("deprecation")
    public TableBrowserDialog(Connection connection, DbConfig dbConfig, DbMetadataService dbService) {
        this.connection = connection;
        this.dbConfig = dbConfig;
        this.dbService = dbService;

        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.EXTENDED);

        Label title = new Label("代码生成器 — 选择表");

        HeaderBar headerBar = FXTools.createpopUpBoxHeaderBar(title);


        generateBtn = new Button("配置并生成代码  ›");
        generateBtn.getStyleClass().addAll("button", "btn-primary");
        generateBtn.setDisable(true);   // 初始未选表，禁用生成按钮

        // ── Left panel ───────────────────────────────────────
        VBox leftPanel = buildLeftPanel();

        // ── Right panel ──────────────────────────────────────
        VBox rightPanel = buildRightPanel();

        // ── SplitPane ────────────────────────────────────────
        SplitPane splitPane = new SplitPane(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.28);   // 左侧占比约 28%
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // ── Footer / status bar ──────────────────────────────
        tableCountLabel.getStyleClass().addAll("label", "label-count");
        selectionLabel.getStyleClass().add("label-selected");

        generateBtn.setOnAction(e -> openGeneratorConfig());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(10, selectionLabel, spacer, generateBtn);
        footer.getStyleClass().add("status-bar");
        footer.setAlignment(Pos.CENTER_LEFT);

        // ── Root ─────────────────────────────────────────────
        VBox root = new VBox(headerBar, splitPane, footer);
        root.setStyle("-fx-background-color: #2b2b2b;");

        Scene scene = new Scene(root, 920, 580);
        loadCss(scene);
        setScene(scene);

        // 左侧表选中事件：选中某表时，在右侧加载该表的列结构
        tableListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        loadColumns(newVal);
                    }
                });

        setOnCloseRequest(e -> closeConnection());   // 窗口关闭时释放数据库连接
        loadTables();                                 // 初始化时异步加载表列表
    }

    // ── Left panel ────────────────────────────────────────────

    /**
     * 构建左侧面板：包含标题栏（表数量、全选/反选按钮）、搜索框、表列表。
     */
    private VBox buildLeftPanel() {
        // Header：标题 + 表数量 + 弹性区域 + 全选/反选按钮
        Label title = new Label("数据库表");
        title.getStyleClass().add("label-title");
        tableCountLabel.getStyleClass().addAll("label", "label-count");

        Button selectAllBtn = new Button("全选");
        selectAllBtn.getStyleClass().addAll("button", "btn-sm");
        Button invertBtn = new Button("反选");
        invertBtn.getStyleClass().addAll("button", "btn-sm");

        selectAllBtn.setOnAction(e -> {
            allTables.forEach(t -> selectedSet.add(t));   // 将所有表加入选中集合
            tableListView.refresh();
            updateSelectionInfo();
        });
        invertBtn.setOnAction(e -> {
            allTables.forEach(t -> {
                if (selectedSet.contains(t)) {
                    selectedSet.remove(t);
                } else {
                    selectedSet.add(t);
                }
            });
            tableListView.refresh();
            updateSelectionInfo();
        });

        Region flex = new Region();
        HBox.setHgrow(flex, Priority.ALWAYS);
        HBox headerRow = new HBox(6, title, tableCountLabel, flex, selectAllBtn, invertBtn);
        headerRow.getStyleClass().add("left-panel-header");
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Search box：实时过滤表名与注释
        TextField searchField = new TextField();
        searchField.setPromptText("搜索表名...");
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox searchBox = new HBox(searchField);
        searchBox.setPadding(new Insets(8, 10, 6, 10));
        searchBox.setStyle("-fx-background-color: #2b2b2b;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, old, val) -> {
            String lower = val.trim().toLowerCase();
            filteredTables.setPredicate(t -> lower.isEmpty()
                    || t.getTableName().toLowerCase().contains(lower)
                    || (t.getRemarks() != null && t.getRemarks().toLowerCase().contains(lower)));
        });

        // Table list with CheckBox cells：每行带复选框，点击切换选中状态
        tableListView.setCellFactory(lv -> new TableListCell());
        tableListView.setStyle("-fx-background-color: #2b2b2b;");
        VBox.setVgrow(tableListView, Priority.ALWAYS);

        // Loading indicator overlay：列表加载时显示旋转动画，加载完成后隐藏
        loadingIndicator.setMaxSize(32, 32);
        loadingIndicator.setVisible(false);
        StackPane listArea = new StackPane(tableListView, loadingIndicator);
        VBox.setVgrow(listArea, Priority.ALWAYS);

        VBox panel = new VBox(headerRow, searchBox, listArea);
        panel.setStyle("-fx-background-color: #2b2b2b;");
        panel.setMinWidth(220);
        return panel;
    }

    // ── Right panel ───────────────────────────────────────────

    /**
     * 构建右侧面板：包含列结构表头与列详情表格
     */
    private VBox buildRightPanel() {
        columnHeaderLabel.getStyleClass().addAll("label", "label-secondary");
        HBox rightHeader = new HBox(columnHeaderLabel);
        rightHeader.getStyleClass().add("right-panel-header");
        rightHeader.setAlignment(Pos.CENTER_LEFT);

        buildColumnTable();
        VBox.setVgrow(columnTableView, Priority.ALWAYS);

        VBox panel = new VBox(rightHeader, columnTableView);
        panel.setStyle("-fx-background-color: #2b2b2b;");
        return panel;
    }

    /**
     * 配置列详情表格的列定义：列名、Java 字段、Java 类型、JDBC 类型、长度、标记、备注
     */
    private void buildColumnTable() {
        columnTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columnTableView.setPlaceholder(buildPlaceholder("选中左侧表格后显示列信息"));

        columnTableView.getColumns().addAll(
                strCol("列名", 120, ColumnMeta::getColumnName),
                strCol("Java 字段", 120, ColumnMeta::getJavaField),
                strCol("Java 类型", 95, ColumnMeta::getJavaShortType),
                strCol("JDBC 类型", 85, ColumnMeta::getJdbcTypeName),
                strCol("长度", 55, m -> String.valueOf(m.getSize())),
                badgeCol(),   // 标记列：PK / AI / NULL 徽章
                strCol("备注", -1, m -> m.getRemarks() != null ? m.getRemarks() : "")
        );
    }

    /**
     * 创建字符串类型表格列：指定标题、固定宽度（-1 表示自适应）、取值函数
     */
    private TableColumn<ColumnMeta, String> strCol(String title, double width,
                                                   Function<ColumnMeta, String> extractor) {
        TableColumn<ColumnMeta, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new SimpleStringProperty(extractor.apply(d.getValue())));
        if (width > 0) {
            col.setPrefWidth(width);
            col.setMinWidth(width);
            col.setMaxWidth(width);
        }
        return col;
    }

    /**
     * 创建标记徽章列：根据主键、自增、可空性动态渲染 PK / AI / NULL 标签
     */
    private TableColumn<ColumnMeta, ColumnMeta> badgeCol() {
        TableColumn<ColumnMeta, ColumnMeta> col = new TableColumn<>("标记");
        col.setPrefWidth(72);
        col.setMinWidth(72);
        col.setMaxWidth(72);
        col.setCellValueFactory(f -> new SimpleObjectProperty<>(f.getValue()));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(ColumnMeta item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                HBox badges = new HBox(4);
                badges.setAlignment(Pos.CENTER_LEFT);
                if (item.isPrimaryKey()) {
                    Label pk = new Label("PK");
                    pk.getStyleClass().add("badge-pk");
                    badges.getChildren().add(pk);
                }
                if (item.isAutoIncrement()) {
                    Label ai = new Label("AI");
                    ai.getStyleClass().add("badge-ai");
                    badges.getChildren().add(ai);
                }
                if (item.isNullable() && !item.isPrimaryKey()) {
                    Label nl = new Label("NULL");
                    nl.getStyleClass().add("badge-nullable");
                    badges.getChildren().add(nl);
                }
                setGraphic(badges);
                setText(null);
            }
        });
        return col;
    }

    // ── Data loading ──────────────────────────────────────────

    /**
     * 异步加载数据库表列表，加载完成后刷新左侧列表与计数
     */
    private void loadTables() {
        loadingIndicator.setVisible(true);
        tableListView.setVisible(false);

        Task<List<TableMeta>> task = new Task<>() {
            @Override
            protected List<TableMeta> call() throws Exception {
                return dbService.listTables(connection, dbConfig.getSchemaName());
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    List<TableMeta> tables = getValue();
                    allTables.setAll(tables);
                    tableCountLabel.setText("共 " + tables.size() + " 张");
                    loadingIndicator.setVisible(false);
                    tableListView.setVisible(true);
                    updateSelectionInfo();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    tableListView.setVisible(true);
                    showError("加载表列表失败", getException());
                });
            }
        };
        new Thread(task).start();
    }

    /**
     * 异步加载指定表的列结构，加载完成后刷新右侧表格
     */
    private void loadColumns(TableMeta table) {
        columnHeaderLabel.setText("列结构 — " + table.getTableName());
        columnTableView.setPlaceholder(buildPlaceholder("加载中..."));
        columnTableView.getItems().clear();

        Task<List<ColumnMeta>> task = new Task<>() {
            @Override
            protected List<ColumnMeta> call() throws Exception {
                return dbService.listColumns(connection, dbConfig.getSchemaName(), table.getTableName());
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    columnTableView.getItems().setAll(getValue());
                    columnTableView.setPlaceholder(buildPlaceholder("该表没有列信息"));
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> showError("加载列信息失败", getException()));
            }
        };
        new Thread(task).start();
    }

    /**
     * 更新底部已选数量标签与生成按钮状态
     */
    private void updateSelectionInfo() {
        int n = selectedSet.size();
        generateBtn.setDisable(n == 0);   // 未选表时禁用生成按钮
        if (n == 0) {
            selectionLabel.setText("已选 0 张表");
            selectionLabel.setStyle(null);
        } else {
            selectionLabel.setText("已选 " + n + " 张: " + buildSelectedNames());
            selectionLabel.setStyle("-fx-text-fill: #4b91e2;");
        }
        tableListView.refresh();
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * 拼接已选表名摘要，超过 60 字符截断
     */
    private String buildSelectedNames() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (TableMeta t : selectedSet) {
            if (i++ > 0) {
                sb.append(", ");
            }
            if (sb.length() > 60) {
                sb.append("...");
                break;
            }
            sb.append(t.getTableName());
        }
        return sb.toString();
    }

    private Label buildPlaceholder(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("label", "label-secondary");
        return l;
    }

    /**
     * 打开代码生成配置对话框，传入已选表列表
     */
    private void openGeneratorConfig() {
        List<TableMeta> checked = new ArrayList<>(selectedSet);
        new GeneratorConfigDialog(connection, dbConfig, dbService, checked).show();
    }

    private void showError(String title, Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(ex != null ? ex.getMessage() : "未知错误");
        loadCss(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    private void loadCss(Scene scene) {
        var url = getClass().getResource("/code-generator.css");
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }

    /**
     * 窗口关闭时安全释放数据库连接
     */
    private void closeConnection() {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }

    // ── Cell factory for table list ───────────────────────────

    /**
     * 表列表自定义单元格：每行包含一个 CheckBox（显示表名）与右侧注释标签。
     * 通过 selectedSet 维护选中状态，实现跨搜索过滤的选中记忆。
     */
    private class TableListCell extends ListCell<TableMeta> {
        private final CheckBox checkBox = new CheckBox();
        private final Label remarksLabel = new Label();
        private final HBox hbox;

        TableListCell() {
            remarksLabel.getStyleClass().addAll("label", "label-secondary");
            Region flex = new Region();
            HBox.setHgrow(flex, Priority.ALWAYS);
            hbox = new HBox(6, checkBox, flex, remarksLabel);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(0, 4, 0, 0));
            setStyle("-fx-padding: 4 6 4 6;");
        }

        @Override
        protected void updateItem(TableMeta item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            checkBox.setText(item.getTableName());
            checkBox.setSelected(selectedSet.contains(item));
            String remarks = item.getRemarks();
            remarksLabel.setText(remarks != null && !remarks.isEmpty() ? remarks : "");

            // 先清除旧监听器，避免复用单元格时事件叠加
            checkBox.setOnAction(null);
            checkBox.setOnAction(e -> {
                if (checkBox.isSelected()) {
                    selectedSet.add(item);
                } else {
                    selectedSet.remove(item);
                }
                updateSelectionInfo();
            });
            setGraphic(hbox);
            setText(null);
        }
    }
}
