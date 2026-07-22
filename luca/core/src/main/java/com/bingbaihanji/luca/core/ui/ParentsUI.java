package com.bingbaihanji.luca.core.ui;

import com.bingbaihanji.luca.core.ui.component.*;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * IDEA 风格顶级 UI 框架组件，纯 JavaFX 代码实现。
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────┐
 * │  标题栏（项目名居中）                                   │
 * │  菜单栏（文件 编辑 视图 …）                              │
 * ├──┬────────────────────────────────────────────────┬───┤
 * │  │ ┌── 编辑器标签栏 ──────────────────────────────┐ │   │
 * │A │ │ tab1 × │ tab2 × │                          │ │ A │
 * │c │ ├────────────────────────────────────────────┤ │ c │
 * │t │ │                                            │ │ t │
 * │i │ │            编辑器内容区                      │ │ i │
 * │v │ │                                            │ │ v │
 * │i │ ├────────────────────────────────────────────┤ │ i │
 * │t │ │ 底部工具面板（Terminal / Run / …）            │ │ t │
 * │y │ └────────────────────────────────────────────┘ │ y │
 * ├──┴─────────────────────────────────────────────────┴───┤
 * │  状态栏（就绪 | 分支 | 编码 | 行列）                        │
 * └────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>多槽位系统</h3>
 * <ul>
 *   <li>左上/右上（TOP）：最多同时激活 2 个，FIFO 淘汰最早的</li>
 *   <li>左下/右下（BOTTOM）：每侧最多 1 个，互斥</li>
 * </ul>
 */
public class ParentsUI extends BorderPane {

    // ── 布局常量 ──────────────────────────────────────────
    private static final double SIDE_PANEL_WIDTH = 260;
    private static final double BOTTOM_PANEL_HEIGHT = 200;
    private static final double RESIZER_SIZE = 4;
    private static final int MAX_TOP_SLOTS = 2;
    /**
     * 左/右侧上方槽队列，FIFO，最多 MAX_TOP_SLOTS 项
     */
    private final Deque<String> leftTopQueue = new ArrayDeque<>();
    private final Deque<String> rightTopQueue = new ArrayDeque<>();
    /**
     * 条目内容/标题缓存
     */
    private final Map<String, Node> itemContents = new HashMap<>();
    private final Map<String, String> itemTitles = new HashMap<>();
    // ── 组件引用 ───────────────────────────────────────────
    private MenuBar menuBar;
    private IdeActivityBar leftActivityBar;
    private IdeActivityBar rightActivityBar;
    private IdeSplitPanel leftSplitPanel;
    private IdeSplitPanel rightSplitPanel;
    private IdeToolWindow leftBotWindow;
    private IdeToolWindow rightBotWindow;
    private SplitPane bottomHSplit;
    private LucaTabPane editorTabPane;

    // ── 多槽位状态 ─────────────────────────────────────────
    private IdeStatusBar statusBar;
    private SplitPane verticalSplit;
    private Region leftResizer;
    private Region rightResizer;
    /**
     * 当前左/右下方槽的条目 id，null = 无
     */
    private String leftBotId = null;
    private String rightBotId = null;

    // ── 活动栏注册 ID ──────────────────────────────────────
    // （供 highlight 同步；LucaApp 调 registerLeftPanelButton 时设置）
    // 实际 highlight 由 updateActivityBarHighlights 全量计算，此处不再单独记录。

    public ParentsUI() {
        buildUI();
    }

    // ══════════════════════════════════════════════════════
    //  构建阶段
    // ══════════════════════════════════════════════════════

    private static void expandPanel(Region panel, Region resizer, double prefWidth) {
        panel.setManaged(true);
        panel.setVisible(true);
        panel.setPrefWidth(prefWidth);
        if (resizer != null) {
            resizer.setManaged(true);
            resizer.setVisible(true);
        }
    }

    private static void collapsePanel(Region panel, Region resizer) {
        panel.setVisible(false);
        panel.setManaged(false);
        if (resizer != null) {
            resizer.setVisible(false);
            resizer.setManaged(false);
        }
    }

    private static Region createHResizer(Region target, boolean leftSide) {
        Region resizer = new Region();
        resizer.setMinWidth(RESIZER_SIZE);
        resizer.setPrefWidth(RESIZER_SIZE);
        resizer.setMaxWidth(RESIZER_SIZE);
        resizer.getStyleClass().add("ide-h-resizer");

        final double[] startX = new double[1];
        final double[] startWidth = new double[1];

        resizer.setOnMousePressed(e -> {
            startX[0] = e.getSceneX();
            startWidth[0] = target.getWidth();
        });
        resizer.setOnMouseDragged(e -> {
            double delta = e.getSceneX() - startX[0];
            double width = leftSide ? startWidth[0] + delta : startWidth[0] - delta;
            target.setPrefWidth(Math.max(120, width));
        });
        return resizer;
    }

    private void buildUI() {
        initComponents();
        setTop(buildTopArea());
        setCenter(buildBodyArea());
        setBottom(statusBar);
        getStyleClass().add("ide-layout-root");
    }

    private void initComponents() {
        menuBar = buildMenuBar();

        leftActivityBar = new IdeActivityBar();
        rightActivityBar = new IdeActivityBar();
        rightActivityBar.getStyleClass().add("ide-activity-bar-right");
        leftActivityBar.setRightBar(false);
        rightActivityBar.setRightBar(true);
        leftActivityBar.setPeer(rightActivityBar);
        rightActivityBar.setPeer(leftActivityBar);

        // 跨侧拖动回调：关闭本侧所有面板
        leftActivityBar.setOnItemMoved(() -> {
            closeAllTopSlots(false);
            closeBotSlot(false, null);
        });
        leftActivityBar.setPanelVisible(() -> !leftSplitPanel.isEmpty() || leftBotId != null);
        rightActivityBar.setOnItemMoved(() -> {
            closeAllTopSlots(true);
            closeBotSlot(true, null);
        });
        rightActivityBar.setPanelVisible(() -> !rightSplitPanel.isEmpty() || rightBotId != null);

        statusBar = new IdeStatusBar();
        editorTabPane = new LucaTabPane();

        leftSplitPanel = new IdeSplitPanel();
        rightSplitPanel = new IdeSplitPanel();
        rightSplitPanel.getStyleClass().add("ide-split-panel-right");

        // 底部槽窗口（初始隐藏）
        leftBotWindow = new IdeToolWindow("", null, () -> closeBotSlot(false, leftBotId));
        rightBotWindow = new IdeToolWindow("", null, () -> closeBotSlot(true, rightBotId));
        leftBotWindow.setVisible(false);
        leftBotWindow.setManaged(false);
        rightBotWindow.setVisible(false);
        rightBotWindow.setManaged(false);

        bottomHSplit = new SplitPane();
        bottomHSplit.setOrientation(Orientation.HORIZONTAL);
        bottomHSplit.getItems().addAll(leftBotWindow, rightBotWindow);

        leftResizer = createHResizer(leftSplitPanel, true);
        rightResizer = createHResizer(rightSplitPanel, false);

        // 初始：侧边栏不可见
        collapsePanel(leftSplitPanel, leftResizer);
        collapsePanel(rightSplitPanel, rightResizer);
    }

    // ══════════════════════════════════════════════════════
    //  公共槽位 API（供 LucaApp / 插件调用）
    // ══════════════════════════════════════════════════════

    @SuppressWarnings("all")
    private Node buildTopArea() {
        HeaderBar headerBar = new HeaderBar();
        headerBar.getStyleClass().add("ide-header-bar");

        ImageView logo = new ImageView(
                new Image(LucaApp.class.getResourceAsStream("/icon/logo.png"))
        );
        logo.setFitWidth(18);
        logo.setFitHeight(18);

        menuBar.getStyleClass().add("ide-header-menu-bar");

        HBox leadingBox = new HBox(10, logo, menuBar);
        leadingBox.setAlignment(Pos.CENTER_LEFT);
        leadingBox.setPadding(new Insets(4, 0, 0, 8));
        HeaderBar.setDragType(leadingBox, HeaderDragType.DRAGGABLE);
        headerBar.setLeading(leadingBox);

        Label titleLabel = new Label("luca");
        titleLabel.getStyleClass().add("ide-title-label");
        HeaderBar.setDragType(titleLabel, HeaderDragType.DRAGGABLE);
        headerBar.setCenter(titleLabel);

        return headerBar;
    }

    // ══════════════════════════════════════════════════════
    //  顶部槽（LEFT_TOP / RIGHT_TOP）
    // ══════════════════════════════════════════════════════

    private MenuBar buildMenuBar() {
        return new MenuBar(
                new Menu("文件(F)"),
                new Menu("编辑(E)"),
                new Menu("视图(V)"),
                new Menu("运行(R)"),
                new Menu("工具(T)"),
                new Menu("导航(N)"),
                new Menu("帮助(H)")
        );
    }

    private HBox buildBodyArea() {
        verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().add(editorTabPane);
        verticalSplit.setMinSize(0, 0);
        HBox.setHgrow(verticalSplit, Priority.ALWAYS);

        HBox body = new HBox(
                leftActivityBar,
                leftSplitPanel, leftResizer,
                verticalSplit,
                rightResizer, rightSplitPanel,
                rightActivityBar
        );
        body.setMinSize(0, 0);

        // Clip body to prevent content from overflowing into HeaderBar when window is too small
        Rectangle bodyClip = new Rectangle();
        bodyClip.widthProperty().bind(body.widthProperty());
        bodyClip.heightProperty().bind(body.heightProperty());
        body.setClip(bodyClip);

        return body;
    }

    // ══════════════════════════════════════════════════════
    //  底部槽（LEFT_BOTTOM / RIGHT_BOTTOM）
    // ══════════════════════════════════════════════════════

    /**
     * 切换槽位。
     *
     * @param isRight  true = 右侧活动栏条目
     * @param isBottom true = 落在活动栏底部分区（→ 底部槽），false = 顶部分区（→ 侧边槽）
     * @param id       条目唯一 ID
     * @param content  面板内容节点
     * @param title    面板标题
     */
    public void toggleSlot(boolean isRight, boolean isBottom,
                           String id, Node content, String title) {
        itemContents.put(id, content);
        itemTitles.put(id, title);
        if (isBottom) {
            toggleBotSlot(isRight, id, content, title);
        } else {
            toggleTopSlot(isRight, id, content, title);
        }
    }

    private void toggleTopSlot(boolean isRight, String id, Node content, String title) {
        Deque<String> queue = isRight ? rightTopQueue : leftTopQueue;
        IdeSplitPanel panel = isRight ? rightSplitPanel : leftSplitPanel;
        Region resizer = isRight ? rightResizer : leftResizer;

        if (queue.contains(id)) {
            // 再次点击 → 关闭
            int slot = panel.slotOf(id);
            queue.remove(id);
            panel.clearSlot(slot);
            panel.compact();
        } else {
            // 新增；若已满则 FIFO 淘汰队首
            if (queue.size() >= MAX_TOP_SLOTS) {
                String evicted = queue.pollFirst();
                panel.clearSlot(0);
                panel.compact();
            }
            queue.addLast(id);
            int newSlot = panel.count();   // 0 或 1（compact 后）
            panel.setSlot(newSlot, id, content, title,
                    () -> toggleTopSlot(isRight, id,
                            itemContents.getOrDefault(id, content),
                            itemTitles.getOrDefault(id, title)));
        }

        if (panel.isEmpty()) {
            collapsePanel(panel, resizer);
        } else {
            expandPanel(panel, resizer, SIDE_PANEL_WIDTH);
        }
        updateActivityBarHighlights(isRight);
    }

    private void closeAllTopSlots(boolean isRight) {
        Deque<String> queue = isRight ? rightTopQueue : leftTopQueue;
        IdeSplitPanel panel = isRight ? rightSplitPanel : leftSplitPanel;
        Region resizer = isRight ? rightResizer : leftResizer;
        for (int i = 0; i < MAX_TOP_SLOTS; i++) {
            panel.clearSlot(i);
        }
        queue.clear();
        collapsePanel(panel, resizer);
        updateActivityBarHighlights(isRight);
    }

    // ══════════════════════════════════════════════════════
    //  活动栏高亮同步
    // ══════════════════════════════════════════════════════

    private void toggleBotSlot(boolean isRight, String id, Node content, String title) {
        if (isRight) {
            if (id.equals(rightBotId)) {
                rightBotId = null;
            } else {
                rightBotId = id;
                rightBotWindow.setContent(content);
            }
        } else {
            if (id.equals(leftBotId)) {
                leftBotId = null;
            } else {
                leftBotId = id;
                leftBotWindow.setContent(content);
            }
        }
        updateBottomArea();
        updateActivityBarHighlights(isRight);
    }

    // ══════════════════════════════════════════════════════
    //  面板展开 / 折叠辅助
    // ══════════════════════════════════════════════════════

    /**
     * Force-close a bottom slot (called from IdeToolWindow's close button).
     */
    private void closeBotSlot(boolean isRight, String id) {
        if (isRight) {
            if (id == null || id.equals(rightBotId)) {
                rightBotId = null;
            }
        } else {
            if (id == null || id.equals(leftBotId)) {
                leftBotId = null;
            }
        }
        updateBottomArea();
        updateActivityBarHighlights(isRight);
    }

    private void updateBottomArea() {
        boolean hasLeft = leftBotId != null;
        boolean hasRight = rightBotId != null;

        // Sync window visibility
        leftBotWindow.setVisible(hasLeft);
        leftBotWindow.setManaged(hasLeft);
        rightBotWindow.setVisible(hasRight);
        rightBotWindow.setManaged(hasRight);

        // Remove any existing bottom entry in verticalSplit
        verticalSplit.getItems().removeIf(n -> n != editorTabPane);

        if (!hasLeft && !hasRight) {
            return;
        }

        if (hasLeft && hasRight) {
            // Both active: horizontal split
            bottomHSplit.getItems().setAll(leftBotWindow, rightBotWindow);
            bottomHSplit.setDividerPositions(0.5);
            verticalSplit.getItems().add(bottomHSplit);
        } else {
            // Only one active
            IdeToolWindow single = hasLeft ? leftBotWindow : rightBotWindow;
            verticalSplit.getItems().add(single);
        }

        bottomHSplit.setPrefHeight(BOTTOM_PANEL_HEIGHT);
        verticalSplit.setDividerPositions(0.72);
    }

    // ══════════════════════════════════════════════════════
    //  水平拖动分隔条
    // ══════════════════════════════════════════════════════

    private void updateActivityBarHighlights(boolean isRight) {
        IdeActivityBar bar = isRight ? rightActivityBar : leftActivityBar;
        Deque<String> topQueue = isRight ? rightTopQueue : leftTopQueue;
        String botId = isRight ? rightBotId : leftBotId;

        java.util.Set<String> active = new java.util.HashSet<>(topQueue);
        if (botId != null) {
            active.add(botId);
        }
        bar.setActiveIds(active);
    }

    // ══════════════════════════════════════════════════════
    //  兼容 API（供 LucaApp 调用）
    // ══════════════════════════════════════════════════════

    /**
     * 向底部 IdeBottomPanel（标签式，来自 BottomPanelApi）添加标签页。
     * 注：此路径独立于多槽位管理，保持与旧行为一致。
     */
    public void addBottomTab(String title, Node content) {
        // Legacy: use the original IdeBottomPanel approach
        // Since we removed IdeBottomPanel, we repurpose toggleBotSlot with a unique id
        String id = "bottomTab-" + title.hashCode();
        itemContents.put(id, content);
        itemTitles.put(id, title);
        // Add to left bot by default (BottomPanelApi doesn't specify a side)
        if (leftBotId == null) {
            leftBotId = id;
            leftBotWindow.setContent(content);
        } else if (rightBotId == null) {
            rightBotId = id;
            rightBotWindow.setContent(content);
        }
        updateBottomArea();
    }

    /**
     * @deprecated Use {@link #toggleSlot} instead.
     */
    @Deprecated
    public void setLeftContent(Node content, String title, String icon) {
        toggleTopSlot(false, "leftPanel-compat", content, title);
    }

    /**
     * @deprecated Use {@link #toggleSlot} instead.
     */
    @Deprecated
    public void setLeftContent(Node content) {
        setLeftContent(content, "项目", "☰");
    }

    /**
     * @deprecated Use {@link #toggleSlot} instead.
     */
    @Deprecated
    public void setRightContent(Node content, String title, String icon) {
        toggleTopSlot(true, "rightPanel-compat", content, title);
    }

    /**
     * @deprecated Use {@link #toggleSlot} instead.
     */
    @Deprecated
    public void setRightContent(Node content) {
        setRightContent(content, "右侧面板", "◈");
    }

    /**
     * No-op stubs kept for LucaApp compatibility — highlights now derived from queue state.
     */
    public void registerLeftPanelButton(String activityBarId) { /* no-op */ }

    public void registerRightPanelButton(String activityBarId) { /* no-op */ }

    // ── Getters ───────────────────────────────────────────

    public MenuBar getMenuBar() {
        return menuBar;
    }

    public LucaTabPane getEditorTabPane() {
        return editorTabPane;
    }

    public IdeStatusBar getStatusBar() {
        return statusBar;
    }

    public IdeActivityBar getLeftActivityBar() {
        return leftActivityBar;
    }

    public IdeActivityBar getRightActivityBar() {
        return rightActivityBar;
    }

    /** 检查给定 id 的槽是否已在左或右顶部 / 底部任意位置处于激活状态。 */
    public boolean isSlotVisible(String id) {
        return leftTopQueue.contains(id) || rightTopQueue.contains(id)
                || id.equals(leftBotId) || id.equals(rightBotId);
    }

    /** 检查左侧文件树是否已激活（供 LucaApp 判断是否需要自动展开）。 */
    public boolean isFileTreeVisible() {
        return isSlotVisible("builtin:file-tree");
    }
}
