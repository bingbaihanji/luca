package com.bingbaihanji.luca.core.kit;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * 文件目录树组件（核心实现）。
 * <p>
 * 特性：
 * <ul>
 *   <li>懒加载：子节点在首次展开时异步读取，避免卡 UI 线程</li>
 *   <li>目录排在文件前，各自按名称升序</li>
 *   <li>Unicode 图标区分目录(📁/📂)与文件(📄)</li>
 *   <li>单击任意条目触发外部回调 {@link #setOnFileSelected}</li>
 *   <li>顶部路径标签 + 刷新按钮</li>
 * </ul>
 * <p>
 * 用法：
 * <pre>{@code
 * FileTreeKit tree = new FileTreeKit();
 * tree.setOnFileSelected((path, isDir) -> { ... });
 * tree.openDirectory(Paths.get("/home/user/project"));
 * }</pre>
 */
public class FileTreeKit extends BorderPane {

    // ── 图标常量 ──────────────────────────────────────────────────
    private static final String ICON_FOLDER_CLOSED = "📁";
    private static final String ICON_FOLDER_OPEN   = "📂";
    private static final String ICON_FILE          = "📄";

    // ── 组件 ──────────────────────────────────────────────────────
    private final TreeView<PathItem> treeView = new TreeView<>();
    private final Label pathLabel = new Label("未打开目录");

    // ── 回调 ─────────────────────────────────────────────────────
    /** 文件/目录被单击时通知外部；第二个参数 true=目录 false=文件 */
    private BiConsumer<Path, Boolean> onFileSelected;

    // ── 后台加载线程池（daemon，随 JVM 退出） ─────────────────────
    private static final ExecutorService LOADER =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "file-tree-loader");
                t.setDaemon(true);
                return t;
            });

    // ── 当前根路径 ────────────────────────────────────────────────
    private Path rootPath;

    public FileTreeKit() {
        buildView();
    }

    // ══════════════════════════════════════════════════════════════
    //  公共 API
    // ══════════════════════════════════════════════════════════════

    /**
     * 打开指定目录，构建树根节点。
     */
    public void openDirectory(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        this.rootPath = dir;
        pathLabel.setText(dir.toString());

        TreeItem<PathItem> root = buildNode(dir);
        root.setExpanded(true);
        treeView.setRoot(root);
    }

    /** 回调：用户单击文件/目录时调用。 */
    public void setOnFileSelected(BiConsumer<Path, Boolean> callback) {
        this.onFileSelected = callback;
    }

    /** 刷新当前目录。 */
    public void refresh() {
        if (rootPath != null) {
            openDirectory(rootPath);
        }
    }

    public Path getRootPath() { return rootPath; }

    // ══════════════════════════════════════════════════════════════
    //  UI 构建
    // ══════════════════════════════════════════════════════════════

    private void buildView() {
        // ── 顶栏：路径 + 刷新按钮 ─────────────────────────────────
        Button refreshBtn = new Button("↺");
        refreshBtn.getStyleClass().add("ide-tool-window-action-btn");
        refreshBtn.setTooltip(new Tooltip("刷新"));
        refreshBtn.setOnAction(e -> refresh());

        pathLabel.getStyleClass().add("file-tree-path-label");
        pathLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pathLabel, Priority.ALWAYS);

        HBox topBar = new HBox(4, pathLabel, refreshBtn);
        topBar.getStyleClass().add("file-tree-top-bar");

        // ── TreeView 配置 ─────────────────────────────────────────
        treeView.setShowRoot(true);
        treeView.getStyleClass().add("file-tree-view");
        treeView.setCellFactory(tv -> new FileTreeCell());

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, sel) -> {
            if (sel == null || onFileSelected == null) {
                return;
            }
            PathItem item = sel.getValue();
            if (item != null && item.path() != null) {
                onFileSelected.accept(item.path(), item.isDirectory());
            }
        });

        setTop(topBar);
        setCenter(treeView);
        getStyleClass().add("file-tree-kit");
    }

    // ══════════════════════════════════════════════════════════════
    //  节点构建（懒加载）
    // ══════════════════════════════════════════════════════════════

    private TreeItem<PathItem> buildNode(Path path) {
        boolean isDir = Files.isDirectory(path);
        PathItem item = new PathItem(path, isDir);
        LazyTreeItem node = new LazyTreeItem(item);

        if (isDir) {
            // 占位符让展开箭头出现
            node.getChildren().add(makePlaceholder());
            node.expandedProperty().addListener((obs, wasExpanded, expanded) -> {
                if (expanded && !node.loaded) {
                    node.loaded = true;
                    loadChildren(node, path);
                }
                // 同步图标
                node.setValue(new PathItem(path, true, expanded));
            });
        }
        return node;
    }

    private void loadChildren(LazyTreeItem parent, Path dir) {
        LOADER.submit(() -> {
            List<TreeItem<PathItem>> dirs  = new ArrayList<>();
            List<TreeItem<PathItem>> files = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path child : stream) {
                    // 隐藏以 . 开头的条目
                    if (child.getFileName().toString().startsWith(".")) {
                        continue;
                    }
                    TreeItem<PathItem> node = buildNode(child);
                    if (Files.isDirectory(child)) {
                        dirs.add(node);
                    } else {
                        files.add(node);
                    }
                }
            } catch (IOException ignored) {}

            Comparator<TreeItem<PathItem>> byName =
                Comparator.comparing(n -> n.getValue().name().toLowerCase());
            dirs.sort(byName);
            files.sort(byName);

            List<TreeItem<PathItem>> all = new ArrayList<>(dirs);
            all.addAll(files);

            Platform.runLater(() -> {
                parent.getChildren().setAll(all);
                if (all.isEmpty()) {
                    parent.getChildren().add(makeEmpty());
                }
            });
        });
    }

    private static TreeItem<PathItem> makePlaceholder() {
        return new TreeItem<>(new PathItem(null, false, false, "加载中…"));
    }

    private static TreeItem<PathItem> makeEmpty() {
        return new TreeItem<>(new PathItem(null, false, false, "（空目录）"));
    }

    // ══════════════════════════════════════════════════════════════
    //  数据模型
    // ══════════════════════════════════════════════════════════════

    /**
     * TreeItem 携带的值。
     * {@code label} 仅在 placeholder / empty 条目时非空，其余情况用 path.getFileName()。
     */
    public record PathItem(Path path, boolean isDirectory, boolean expanded, String label) {
        PathItem(Path path, boolean isDirectory) {
            this(path, isDirectory, false, null);
        }
        PathItem(Path path, boolean isDirectory, boolean expanded) {
            this(path, isDirectory, expanded, null);
        }

        public String name() {
            if (label != null) {
                return label;
            }
            if (path == null) {
                return "";
            }
            Path fn = path.getFileName();
            return fn != null ? fn.toString() : path.toString();
        }

        public String icon() {
            if (path == null) {
                return "";
            }
            if (isDirectory) {
                return expanded ? ICON_FOLDER_OPEN : ICON_FOLDER_CLOSED;
            }
            return fileIcon(path);
        }
    }

    private static String fileIcon(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        if (name.endsWith(".java") || name.endsWith(".kt")) {
            return "☕";
        }
        if (name.endsWith(".xml") || name.endsWith(".html")) {
            return "🌐";
        }
        if (name.endsWith(".json") || name.endsWith(".yaml") || name.endsWith(".yml")) {
            return "📋";
        }
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
            name.endsWith(".gif") || name.endsWith(".svg")) {
            return "🖼";
        }
        if (name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") ||
            name.endsWith(".ogg") || name.endsWith(".m4a")) {
            return "🎵";
        }
        if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi")) {
            return "🎬";
        }
        if (name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".tar") ||
            name.endsWith(".gz")) {
            return "📦";
        }
        if (name.endsWith(".md") || name.endsWith(".txt")) {
            return "📝";
        }
        return ICON_FILE;
    }

    // ══════════════════════════════════════════════════════════════
    //  LazyTreeItem — 带 loaded 标志
    // ══════════════════════════════════════════════════════════════

    private static class LazyTreeItem extends TreeItem<PathItem> {
        boolean loaded = false;

        LazyTreeItem(PathItem value) {
            super(value);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  自定义 Cell
    // ══════════════════════════════════════════════════════════════

    private static class FileTreeCell extends TreeCell<PathItem> {
        @Override
        protected void updateItem(PathItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().removeAll("file-tree-dir", "file-tree-file", "file-tree-placeholder");
            } else if (item.path() == null) {
                setText(item.label());
                setGraphic(null);
                getStyleClass().removeAll("file-tree-dir", "file-tree-file");
                if (!getStyleClass().contains("file-tree-placeholder")) {
                    getStyleClass().add("file-tree-placeholder");
                }
            } else {
                setText(item.icon() + "  " + item.name());
                setGraphic(null);
                getStyleClass().removeAll("file-tree-placeholder");
                if (item.isDirectory()) {
                    if (!getStyleClass().contains("file-tree-dir")) {
                        getStyleClass().add("file-tree-dir");
                    }
                    getStyleClass().remove("file-tree-file");
                } else {
                    if (!getStyleClass().contains("file-tree-file")) {
                        getStyleClass().add("file-tree-file");
                    }
                    getStyleClass().remove("file-tree-dir");
                }
            }
        }
    }
}
