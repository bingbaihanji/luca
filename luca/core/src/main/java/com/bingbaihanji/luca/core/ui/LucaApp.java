package com.bingbaihanji.luca.core.ui;

import com.bingbaihanji.luca.api.activity.ActivityBarItemApi;
import com.bingbaihanji.luca.api.center.CenterTabApi;
import com.bingbaihanji.luca.api.filetree.FileSelectionApi;
import com.bingbaihanji.luca.api.menu.MenuApi;
import com.bingbaihanji.luca.api.menu.TabPaneAccessor;
import com.bingbaihanji.luca.api.panel.BottomPanelApi;
import com.bingbaihanji.luca.api.panel.LeftPanelApi;
import com.bingbaihanji.luca.api.panel.RightPanelApi;
import com.bingbaihanji.luca.api.statusbar.StatusBarContributionApi;
import com.bingbaihanji.luca.core.kit.FileTreeKit;
import com.bingbaihanji.sunsen.core.DefaultPluginManager;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

/**
 * luca
 * <p>
 * 启动流程：
 * <ol>
 *   <li>初始化 PluginManager，扫描并启动所有插件</li>
 *   <li>构建主 UI（{@link ParentsUI}）</li>
 *   <li>将各扩展点的实现注入对应 UI 区域</li>
 *   <li>加载深色主题 CSS，展示主窗口</li>
 * </ol>
 */
public class LucaApp extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }


    DefaultPluginManager pluginManager = new DefaultPluginManager();

    @Override
    public void init() throws Exception {
        super.init();

        Path pluginsDir = getPluginsDir();
        // 1. 插件目录
        if (!Files.exists(pluginsDir)) {
            Files.createDirectories(pluginsDir);
        }

        // 2. 启动插件框架
        pluginManager.setPluginsDir(pluginsDir);
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
    }

    @Override
    public void start(Stage stage) throws Exception {

        stage.initStyle(StageStyle.EXTENDED);
        // 3. 构建主 UI
        ParentsUI root = new ParentsUI();

        // 4. MenuApi：向菜单栏追加子菜单项
        List<?> menuExts = pluginManager.getExtensions(MenuApi.class);
        for (Object ext : menuExts) {
            MenuApi api = (MenuApi) ext;
            api.bindTabPane(new TabPaneAccessor() {
                @Override
                public void addOrSelect(String id, String title, Node content) {
                    root.getEditorTabPane().addTab(id, title, content);
                }

                @Override
                public String getActiveId() {
                    return root.getEditorTabPane().getActiveId();
                }

                @Override
                public boolean containsTab(String id) {
                    return root.getEditorTabPane().containsTab(id);
                }
            });
            api.extend(root.getMenuBar());
        }

        // 5. LeftPanelApi：注入左上槽
        List<?> leftExts = pluginManager.getExtensions(LeftPanelApi.class);
        if (!leftExts.isEmpty()) {
            LeftPanelApi api = (LeftPanelApi) leftExts.getFirst();
            final String id = "leftPanel-api";
            root.getLeftActivityBar().addItem(
                    id, api.getActivityIcon(), api.getPanelTitle(),
                    (isRight, isBottom) ->
                            root.toggleSlot(isRight, isBottom, id, api.getContent(), api.getPanelTitle())
            );
            // Open immediately in left top slot
            root.toggleSlot(false, false, id, api.getContent(), api.getPanelTitle());
        }

        // 6. RightPanelApi：注入右上槽
        List<?> rightExts = pluginManager.getExtensions(RightPanelApi.class);
        if (!rightExts.isEmpty()) {
            RightPanelApi api = (RightPanelApi) rightExts.getFirst();
            final String id = "rightPanel-api";
            root.getRightActivityBar().addItem(
                    id, api.getActivityIcon(), api.getPanelTitle(),
                    (isRight, isBottom) ->
                            root.toggleSlot(isRight, isBottom, id, api.getContent(), api.getPanelTitle())
            );
            // Open immediately in right top slot
            root.toggleSlot(true, false, id, api.getContent(), api.getPanelTitle());
        }

        // 7. BottomPanelApi：向底部面板注入标签页（支持多个）
        List<?> bottomExts = pluginManager.getExtensions(BottomPanelApi.class);
        for (Object ext : bottomExts) {
            BottomPanelApi api = (BottomPanelApi) ext;
            String tabTitle = api.getTabIcon().isBlank()
                    ? api.getTabTitle()
                    : api.getTabIcon() + " " + api.getTabTitle();
            root.addBottomTab(tabTitle, api.getContent());
        }

        // 8. ActivityBarItemApi：自定义活动栏图标（按 order 升序）
        List<?> activityExts = pluginManager.getExtensions(ActivityBarItemApi.class);
        activityExts.stream()
                .map(e -> (ActivityBarItemApi) e)
                .sorted(Comparator.comparingInt(ActivityBarItemApi::getOrder))
                .forEach(api -> {
                    java.util.function.BiConsumer<Boolean, Boolean> action =
                            (isRight, isBottom) ->
                                    root.toggleSlot(isRight, isBottom, api.getId(), api.getContent(), api.getPanelTitle());

                    switch (api.getSide()) {
                        case LEFT -> root.getLeftActivityBar().addItem(
                                api.getId(), api.getIcon(), api.getTooltip(), action);
                        case RIGHT -> root.getRightActivityBar().addItem(
                                api.getId(), api.getIcon(), api.getTooltip(), action);
                        case BOTTOM -> root.addBottomTab(api.getPanelTitle(), api.getContent());
                    }
                });

        // 9. StatusBarContributionApi：状态栏组件
        List<?> statusExts = pluginManager.getExtensions(StatusBarContributionApi.class);
        for (Object ext : statusExts) {
            StatusBarContributionApi api = (StatusBarContributionApi) ext;
            if (api.isRightAligned()) {
                root.getStatusBar().addRightWidget(api.getWidget());
            } else {
                root.getStatusBar().addLeftWidget(api.getWidget());
            }
        }

        // 10. EditorTabApi：插件预置中心标签页
        List<?> editorTabExts = pluginManager.getExtensions(CenterTabApi.class);
        for (Object ext : editorTabExts) {
            CenterTabApi api = (CenterTabApi) ext;
            for (CenterTabApi.TabDescriptor tab : api.getTabs()) {
                root.getEditorTabPane().addTab(tab.id(), tab.title(), tab.content());
            }
        }

        // 11. FileSelectionApi 订阅者列表（供文件树广播）
        List<?> fileSelExts = pluginManager.getExtensions(FileSelectionApi.class);

        // 12. 内建"目录"面板 ── FileTreeKit
        FileTreeKit fileTree = new FileTreeKit();
        fileTree.setOnFileSelected((path, isDir) -> {
            // 广播给所有订阅插件
            for (Object ext : fileSelExts) {
                ((FileSelectionApi) ext).onFileSelected(path, isDir);
            }
        });

        final String fileTreeId = "builtin:file-tree";
        root.getLeftActivityBar().addItem(
                fileTreeId, "📁", "目录",
                (isRight, isBottom) ->
                        root.toggleSlot(isRight, isBottom, fileTreeId, fileTree, "目录")
        );

//        // 13. 菜单"文件" → "打开目录"
//        root.getMenuBar().getMenus().stream()
//                .filter(m -> m.getText().startsWith("文件"))
//                .findFirst()
//                .ifPresent(fileMenu -> {
//                    MenuItem openDir = new MenuItem("打开目录(O)");
//                    openDir.setOnAction(e -> {
//                        // 使用 Swing JFileChooser 替代 JavaFX DirectoryChooser，
//                        // 浏览目录时右侧可展示该目录下的文件/子目录内容
//                        SwingUtilities.invokeLater(() -> {
//                            JFileChooser chooser = new JFileChooser();
//                            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//                            chooser.setDialogTitle("选择目录");
//                            int result = chooser.showOpenDialog(null);
//                            if (result == JFileChooser.APPROVE_OPTION) {
//                                File selected = chooser.getSelectedFile();
//                                Platform.runLater(() -> {
//                                    fileTree.openDirectory(selected.toPath());
//                                    // 若目录面板未打开，自动展开
//                                    if (!root.isFileTreeVisible()) {
//                                        root.toggleSlot(false, false, fileTreeId, fileTree, "目录");
//                                    }
//                                });
//                            }
//                        });
//                    });
//                    fileMenu.getItems().add(openDir);
//                });
//
//
        // 13. 菜单"文件" → "打开目录"
        root.getMenuBar().getMenus().stream()
                .filter(m -> m.getText().startsWith("文件"))
                .findFirst()
                .ifPresent(fileMenu -> {
                    MenuItem openDir = new MenuItem("打开目录(O)");
                    openDir.setOnAction(e -> {
                        DirectoryChooser chooser = new DirectoryChooser();
                        chooser.setTitle("选择目录");
                        File selected = chooser.showDialog(stage);
                        if (selected != null) {
                            fileTree.openDirectory(selected.toPath());
                            // 若目录面板未打开，自动展开
                            if (!root.isFileTreeVisible()) {
                                root.toggleSlot(false, false, fileTreeId, fileTree, "目录");
                            }
                        }
                    });
                    fileMenu.getItems().add(openDir);
                });

        // 14. 展示主窗口
        Scene scene = new Scene(root, 1280, 800);

        String css = LucaApp.class.getResource("/css/idea-dark.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("luca");
        stage.getIcons().add(new Image(LucaApp.class.getResourceAsStream("/icon/logo.png")));
        stage.setScene(scene);
        stage.show();

        // 15. 退出时停止插件
        stage.setOnCloseRequest(e -> {
            pluginManager.stopPlugins();
            pluginManager.unloadPlugins();
        });
    }


    @Override
    public void stop() throws Exception {
        super.stop();
        pluginManager.stopPlugins();
        pluginManager.unloadPlugins();
    }


    /**
     * 获取 plugins 目录（始终位于程序自身目录）
     */
    public static Path getPluginsDir() throws IOException {
        Path baseDir = determineBaseDirectory();
        Path pluginsDir = baseDir.resolve("plugins");

        Files.createDirectories(pluginsDir);
        return pluginsDir;
    }

    /**
     * 获取程序所在目录：
     * - JAR 运行 → JAR 所在目录
     * - IDE 运行 → classes 输出目录
     */
    private static Path determineBaseDirectory() {
        try {
            var uri = LucaApp.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();

            Path path = Paths.get(uri);

            // JAR 运行
            if (Files.isRegularFile(path)) {
                return path.getParent();
            }

            // IDE 运行（classes 目录）
            return path;

        } catch (Exception e) {
            throw new IllegalStateException("Cannot determine application directory", e);
        }
    }
}
