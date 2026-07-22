package com.bingbaihanji.luca.plugin.code.generator.ui;

import com.bingbaihanji.luca.plugin.code.generator.db.ColumnMeta;
import com.bingbaihanji.luca.plugin.code.generator.db.DbConfig;
import com.bingbaihanji.luca.plugin.code.generator.db.DbMetadataService;
import com.bingbaihanji.luca.plugin.code.generator.db.TableMeta;
import com.bingbaihanji.luca.plugin.code.generator.engine.CodeGeneratorEngine;
import com.bingbaihanji.luca.plugin.code.generator.engine.GeneratorConfig;
import com.bingbaihanji.luca.plugin.code.generator.util.FXTools;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * 代码生成配置对话框：模态窗口，收集输出路径、包名、生成选项等配置后执行批量代码生成。
 * 提供进度条实时反馈，支持为已选的多张表一键生成全套代码。
 */
public class GeneratorConfigDialog extends Stage {

    // ── 外部传入的上下文 ─────────────────────────────────────
    private final Connection connection;           // 数据库连接（用于查询列结构）
    private final DbConfig dbConfig;               // 数据库配置（含 schemaName）
    private final DbMetadataService dbService;     // 元数据服务
    private final List<TableMeta> selectedTables;  // 用户在 TableBrowserDialog 中选中的表列表

    // ── 路径与包名表单字段 ───────────────────────────────────
    private final TextField outputPathField;       // Java 源码输出目录（src/main/java）
    private final TextField resourcesPathField;    // Resources 目录（src/main/resources）
    private final TextField basePackageField;      // 基础包名
    private final TextField tablePrefixField;      // 要去掉的表名前缀
    private final TextField authorField;           // 作者名

    // ── 生成选项复选框 ───────────────────────────────────────
    private final CheckBox entityCheck;            // 是否生成 Entity
    private final CheckBox mapperCheck;            // 是否生成 Mapper 接口
    private final CheckBox mapperXmlCheck;         // 是否生成 Mapper XML
    private final CheckBox serviceCheck;           // 是否生成 Service 接口
    private final CheckBox serviceImplCheck;       // 是否生成 ServiceImpl
    private final CheckBox controllerCheck;        // 是否生成 Controller
    private final CheckBox lombokCheck;            // 是否在 Entity 中使用 Lombok

    // ── 进度与操作控件 ───────────────────────────────────────
    private final ProgressBar progressBar;         // 进度条
    private final Label progressLabel;             // 进度描述文本（当前正在生成的表名）
    private final Label progressCountLabel;        // 进度计数（如 2 / 5）
    private final Button generateBtn;              // 生成按钮（生成期间禁用）

    @SuppressWarnings("deprecation")
    public GeneratorConfigDialog(Connection connection, DbConfig dbConfig,
                                 DbMetadataService dbService, List<TableMeta> selectedTables) {
        this.connection = connection;
        this.dbConfig = dbConfig;
        this.dbService = dbService;
        this.selectedTables = selectedTables;

        initModality(Modality.APPLICATION_MODAL);

        Label headLabel = new Label("代码生成器");

        initStyle(StageStyle.EXTENDED); // 标题栏设置
        HeaderBar headerBar = FXTools.createpopUpBoxHeaderBar(headLabel);


        // ──  top ──────────────────────────────────────
        Label titleLabel = new Label("代码生成配置");
        titleLabel.getStyleClass().add("label-title");
        HBox top = new HBox(titleLabel);
        top.getStyleClass().add("dialog-header");
        top.setAlignment(Pos.CENTER_LEFT);

        // ── Instantiate fields ───────────────────────────────
        outputPathField = styledField("", "src/main/java 目录路径");
        resourcesPathField = styledField("", "src/main/resources 目录路径");
        basePackageField = styledField("com.bingbaihanji.demo", null);
        tablePrefixField = styledField("", "如: t_ （为空则不去除）");
        authorField = styledField(System.getProperty("user.name", "bingbaihanji"), null);

        entityCheck = styledCheck("Entity", true);
        mapperCheck = styledCheck("Mapper", true);
        mapperXmlCheck = styledCheck("MapperXML", true);
        serviceCheck = styledCheck("Service", true);
        serviceImplCheck = styledCheck("ServiceImpl", true);
        controllerCheck = styledCheck("Controller", true);
        lombokCheck = styledCheck("Lombok 注解", true);

        // ── Section 1: 路径配置 ──────────────────────────────
        GridPane pathGrid = new GridPane();
        pathGrid.setHgap(10);
        pathGrid.setVgap(10);
        configureColumns(pathGrid, 110);

        Button outputBrowseBtn = browseBtn();
        outputBrowseBtn.setOnAction(e -> browse(outputPathField, "选择输出目录 (src/main/java)"));
        Button resBrowseBtn = browseBtn();
        resBrowseBtn.setOnAction(e -> browse(resourcesPathField, "选择 Resources 目录"));

        pathGrid.add(formLabel("Java 源码目录:"), 0, 0);
        addBrowseRow(pathGrid, 0, outputPathField, outputBrowseBtn);
        pathGrid.add(formLabel("Resources 目录:"), 0, 1);
        addBrowseRow(pathGrid, 1, resourcesPathField, resBrowseBtn);

        VBox section1 = sectionCard("路径配置", pathGrid);

        // ── Section 2: 包名配置 ──────────────────────────────
        GridPane pkgGrid = new GridPane();
        pkgGrid.setHgap(10);
        pkgGrid.setVgap(10);
        configureColumns(pkgGrid, 80);

        pkgGrid.add(formLabel("基础包名:"), 0, 0);
        pkgGrid.add(basePackageField, 1, 0);
        pkgGrid.add(formLabel("表名前缀:"), 0, 1);
        HBox prefixAuthorRow = new HBox(10, tablePrefixField, formLabel("作者:"), authorField);
        HBox.setHgrow(tablePrefixField, Priority.ALWAYS);
        HBox.setHgrow(authorField, Priority.ALWAYS);
        prefixAuthorRow.setAlignment(Pos.CENTER_LEFT);
        pkgGrid.add(prefixAuthorRow, 1, 1);

        VBox section2 = sectionCard("包名配置", pkgGrid);

        // ── Section 3: 生成选项 ──────────────────────────────
        Label fileTypeLabel = new Label("生成文件:");
        fileTypeLabel.getStyleClass().add("label-secondary");
        FlowPane fileTypePane = new FlowPane(Orientation.HORIZONTAL, 14, 8);
        fileTypePane.getChildren().addAll(
                entityCheck, mapperCheck, mapperXmlCheck,
                serviceCheck, serviceImplCheck, controllerCheck);

        Separator sep = new Separator();
        sep.setPadding(new Insets(4, 0, 4, 0));

        Label styleLabel = new Label("代码风格:");
        styleLabel.getStyleClass().add("label-secondary");
        FlowPane stylePane = new FlowPane(Orientation.HORIZONTAL, 14, 8);
        stylePane.getChildren().add(lombokCheck);

        VBox optContent = new VBox(6, fileTypeLabel, fileTypePane, sep, styleLabel, stylePane);
        VBox section3 = sectionCard("生成选项", optContent);

        // ── Table info bar ───────────────────────────────────
        String tableNames = buildTableNamesSummary();
        Label tablesInfo = new Label("将为以下 " + selectedTables.size() + " 张表生成代码: " + tableNames);
        tablesInfo.getStyleClass().addAll("label", "label-secondary");
        tablesInfo.setWrapText(true);
        tablesInfo.setMaxWidth(Double.MAX_VALUE);
        HBox infoBox = new HBox(tablesInfo);
        infoBox.setPadding(new Insets(2, 4, 2, 4));

        // ── Progress section ─────────────────────────────────
        progressLabel = new Label("准备就绪");
        progressLabel.getStyleClass().add("label-secondary");
        progressCountLabel = new Label("");
        progressCountLabel.getStyleClass().add("label-secondary");
        Region flex = new Region();
        HBox.setHgrow(flex, Priority.ALWAYS);
        HBox progressHeader = new HBox(progressLabel, flex, progressCountLabel);
        progressHeader.setAlignment(Pos.CENTER_LEFT);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);   // 初始隐藏，点击生成后显示
        VBox progressBox = new VBox(4, progressHeader, progressBar);

        // ── Content area ─────────────────────────────────────
        VBox content = new VBox(12, section1, section2, section3, infoBox, progressBox);
        content.setPadding(new Insets(16, 20, 12, 20));
        content.setStyle("-fx-background-color: #2b2b2b;");

        // ── Footer ───────────────────────────────────────────
        Button cancelBtn = new Button("取消");
        cancelBtn.getStyleClass().addAll("button", "btn-ghost");
        cancelBtn.setOnAction(e -> close());

        generateBtn = new Button("⚡  生成代码");
        generateBtn.getStyleClass().addAll("button", "btn-primary");
        generateBtn.setOnAction(e -> generateCode());

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(8, cancelBtn, footerSpacer, generateBtn);
        footer.getStyleClass().add("dialog-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        // ── Root ─────────────────────────────────────────────
        VBox root = new VBox(headerBar, top, content, footer);
        root.setStyle("-fx-background-color: #2b2b2b;");

        Scene scene = new Scene(root, 600, -1);
        loadCss(scene);
        setScene(scene);
        sizeToScene();
        setResizable(false);
    }

    // ── Code generation ───────────────────────────────────────

    /**
     * 执行代码生成：
     * 1) 校验输出路径与基础包名是否已填写
     * 2) 根据界面选项构建 GeneratorConfig
     * 3) 禁用生成按钮，显示进度条
     * 4) 在后台线程逐表查询列结构并调用 CodeGeneratorEngine 生成代码
     * 5) 完成后弹出成功提示并关闭对话框；失败则弹出错误提示并恢复按钮
     */
    private void generateCode() {
        String outputPath = outputPathField.getText().trim();
        String basePackage = basePackageField.getText().trim();
        if (outputPath.isEmpty()) {
            showWarn("请选择或输入 Java 源码输出目录");
            return;
        }
        if (basePackage.isEmpty()) {
            showWarn("请输入基础包名");
            return;
        }

        GeneratorConfig config = buildConfig(outputPath, basePackage);

        generateBtn.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        progressLabel.setText("正在生成...");
        progressCountLabel.setText("0 / " + selectedTables.size());

        CodeGeneratorEngine engine = new CodeGeneratorEngine();
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                List<String> done = new ArrayList<>();
                int total = selectedTables.size();
                for (int i = 0; i < total; i++) {
                    TableMeta table = selectedTables.get(i);
                    int finalI = i;
                    // 在 JavaFX 线程更新进度文本，避免跨线程异常
                    Platform.runLater(() -> {
                        progressLabel.setText("正在生成: " + table.getTableName());
                        progressCountLabel.setText(finalI + " / " + total);
                    });
                    // 查询当前表的所有列元数据
                    List<ColumnMeta> columns = dbService.listColumns(
                            connection, dbConfig.getSchemaName(), table.getTableName());
                    engine.generateAll(table, columns, config);   // 调用引擎生成该表的全部文件
                    updateProgress(i + 1, total);                  // 更新任务进度（绑定到 ProgressBar）
                    done.add(table.getTableName());
                }
                return done;
            }

            @Override
            @SuppressWarnings("deprecation")
            protected void succeeded() {
                Platform.runLater(() -> {
                    progressBar.progressProperty().unbind();
                    progressBar.setProgress(1);
                    progressLabel.setText("✓  生成完成");
                    progressCountLabel.setText(selectedTables.size() + " / " + selectedTables.size());

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);

                    alert.initStyle(StageStyle.EXTENDED);
                    alert.setTitle("生成成功");
                    alert.setHeaderText(null);
                    alert.setContentText("已为 " + getValue().size() + " 张表生成代码。\n输出目录: " + config.getOutputPath());
                    loadCss(alert.getDialogPane().getScene());
                    alert.showAndWait();
                    close();
                });
            }

            @Override
            @SuppressWarnings("deprecation")
            protected void failed() {
                Platform.runLater(() -> {
                    progressBar.progressProperty().unbind();
                    progressBar.setVisible(false);
                    progressLabel.setText("✗  生成失败");
                    generateBtn.setDisable(false);
                    Throwable ex = getException();
                    Alert alert = new Alert(Alert.AlertType.ERROR);

                    alert.initStyle(StageStyle.EXTENDED);
                    alert.setTitle("生成失败");
                    alert.setHeaderText(null);
                    alert.setContentText(ex != null ? ex.getMessage() : "未知错误");
                    loadCss(alert.getDialogPane().getScene());
                    alert.showAndWait();
                });
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());   // 进度条绑定任务进度
        new Thread(task).start();                                        // 启动后台线程执行生成
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * 从界面字段构建 GeneratorConfig 对象
     */
    private GeneratorConfig buildConfig(String outputPath, String basePackage) {
        GeneratorConfig c = new GeneratorConfig();
        c.setOutputPath(outputPath);
        // 若 Resources 目录未填写，默认将 outputPath 中的 /java 替换为 /resources（兼容 Windows 反斜杠）
        String res = resourcesPathField.getText().trim();
        c.setResourcesPath(res.isEmpty() ? outputPath.replace("/java", "/resources").replace("\\java", "\\resources") : res);
        c.setBasePackage(basePackage);
        c.setTablePrefix(tablePrefixField.getText().trim());
        c.setAuthor(authorField.getText().trim());
        c.setGenerateEntity(entityCheck.isSelected());
        c.setGenerateMapper(mapperCheck.isSelected());
        c.setGenerateMapperXml(mapperXmlCheck.isSelected());
        c.setGenerateService(serviceCheck.isSelected());
        c.setGenerateServiceImpl(serviceImplCheck.isSelected());
        c.setGenerateController(controllerCheck.isSelected());
        c.setUseLombok(lombokCheck.isSelected());
        return c;
    }

    /**
     * 构建已选表名摘要：拼接前几张表名，超过 80 字符截断并追加 "..."
     */
    private String buildTableNamesSummary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedTables.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (sb.length() > 80) {
                sb.append("...");
                break;
            }
            sb.append(selectedTables.get(i).getTableName());
        }
        return sb.toString();
    }

    private TextField styledField(String text, String prompt) {
        TextField f = new TextField(text);
        if (prompt != null) {
            f.setPromptText(prompt);
        }
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private CheckBox styledCheck(String text, boolean selected) {
        CheckBox cb = new CheckBox(text);
        cb.setSelected(selected);
        return cb;
    }

    private Button browseBtn() {
        Button btn = new Button("浏览...");
        btn.getStyleClass().addAll("button", "btn-sm");
        return btn;
    }

    private Label formLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("label", "label-form");
        l.setAlignment(Pos.CENTER_RIGHT);
        return l;
    }

    private void configureColumns(GridPane grid, double labelWidth) {
        ColumnConstraints lc = new ColumnConstraints(labelWidth);
        lc.setHalignment(HPos.RIGHT);
        ColumnConstraints fc = new ColumnConstraints();
        fc.setHgrow(Priority.ALWAYS);
        fc.setFillWidth(true);
        grid.getColumnConstraints().addAll(lc, fc);
    }

    /**
     * 在 GridPane 第 row 行第 1 列添加「文本框 + 浏览按钮」的横向布局
     */
    private void addBrowseRow(GridPane grid, int row, TextField field, Button btn) {
        HBox row_ = new HBox(6, field, btn);
        HBox.setHgrow(field, Priority.ALWAYS);
        row_.setAlignment(Pos.CENTER_LEFT);
        grid.add(row_, 1, row);
    }

    private VBox sectionCard(String title, Node content) {
        Label lbl = new Label(title);
        lbl.getStyleClass().add("section-header");
        VBox card = new VBox(8, lbl, content);
        card.getStyleClass().add("section-card");
        return card;
    }

    /**
     * 打开系统目录选择器，将选中路径回填到指定文本框
     */
    private void browse(TextField field, String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File dir = chooser.showDialog(this);
        if (dir != null) {
            field.setText(dir.getAbsolutePath());
        }
    }

    /**
     * 弹出警告对话框
     */
    @SuppressWarnings("deprecation")
    private void showWarn(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initStyle(StageStyle.EXTENDED);
        alert.setTitle("警告");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        loadCss(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    /**
     * 加载暗色主题 CSS，若 scene 为 null 则跳过（部分 Alert 初始化阶段 scene 尚未就绪）
     */
    private void loadCss(Scene scene) {
        if (scene == null) {
            return;
        }
        var url = getClass().getResource("/code-generator.css");
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }
}
