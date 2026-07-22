package com.bingbaihanji.luca.core.ui.component;

import javafx.animation.PauseTransition;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * IDEA 风格活动栏（窄条图标侧边栏）。
 * <p>
 * 布局：顶部分区（从上往下）+ 弹性填充 + 底部分区（从下往上）。
 * <p>
 * 拖动交互：
 * <ul>
 *   <li>左键长按 400ms 后进入拖动模式</li>
 *   <li>鼠标移动期间若偏移 &gt;6px 则取消长按（视为普通操作）</li>
 *   <li>拖动时显示蓝色边框幽灵图标 + 蓝色吸附指示线</li>
 *   <li>可在同侧上下调序，也可拖到对侧（需通过 {@link #setPeer} 关联）</li>
 *   <li>使用 Scene 级事件过滤器捕获鼠标移出按钮后的事件，解决拖动卡顿问题</li>
 * </ul>
 * <p>
 * 点击动作通过 {@code Consumer<Boolean>} 传入：参数 {@code true} 表示当前条目位于右侧活动栏，
 * 动作应将面板内容展示到右侧；{@code false} 时展示到左侧。
 * 条目被拖到对侧后，点击自动操作正确一侧的面板。
 */
public class IdeActivityBar extends VBox {

    private static final int LONG_PRESS_MS = 400;
    /**
     * 吸附指示线：蓝色 2px 横条。JavaFX 节点只能有一个父容器，天然保证同时只出现一次。
     */
    private static final Region SNAP_LINE;
    private static IdeActivityBar dragSourceBar;
    private static BarItem dragItem;
    private static boolean dragging = false;
    private static Popup dragGhost;
    /**
     * Scene 级拖动过滤器：注册在 Scene 捕获阶段，确保鼠标移出按钮范围后仍持续收到事件。
     * 使用静态 final 引用，以便 removeEventFilter 时能匹配同一实例。
     */
    private static final EventHandler<MouseEvent> SCENE_DRAG_FILTER = e -> {
        if (!dragging || dragSourceBar == null) {
            return;
        }
        dragSourceBar.moveGhost(e.getScreenX(), e.getScreenY());
        dragSourceBar.refreshSnap(e.getScreenX(), e.getScreenY());
        e.consume();
    };
    private static Scene activeScene;

    static {
        SNAP_LINE = new Region();
        SNAP_LINE.setMinHeight(2);
        SNAP_LINE.setPrefHeight(2);
        SNAP_LINE.setMaxHeight(2);
        SNAP_LINE.setMinWidth(28);
        SNAP_LINE.setMaxWidth(Double.MAX_VALUE);
        SNAP_LINE.setStyle("-fx-background-color:#4b9eff;-fx-background-radius:1;");
        SNAP_LINE.setMouseTransparent(true);
        VBox.setMargin(SNAP_LINE, new Insets(1, 4, 1, 4));
    }

    // ── 布局：顶部 VBox + 弹性填充 + 底部 VBox ──────────────
    private final VBox topBox = new VBox(2);

    // ═══════════════════════════════════════════════════════
    //  静态拖动状态（所有实例共享，同一时刻最多一个拖动会话）
    // ═══════════════════════════════════════════════════════
    private final VBox bottomBox = new VBox(2);
    private final List<BarItem> topItems = new ArrayList<>();
    private final List<BarItem> bottomItems = new ArrayList<>();
    private final Map<String, Label> labelMap = new LinkedHashMap<>();
    /**
     * true = 右侧活动栏。点击条目时将此值传给 action，决定操作哪侧面板。
     */
    private boolean rightBar = false;
    /**
     * 对侧活动栏，用于跨侧拖拽和 hitTest。
     */
    private IdeActivityBar peer;
    /**
     * 跨侧拖动完成时调用，用于关闭本侧面板（由外部 ParentsUI 注入）。
     */
    private Runnable onItemMoved;
    /**
     * 返回本侧面板当前是否可见（由外部 ParentsUI 注入）。
     */
    private java.util.function.BooleanSupplier panelVisible = () -> false;
    // ── 每实例按压状态 ────────────────────────────────────────
    private PauseTransition longPressTimer;    /**
     * Scene 级释放过滤器：鼠标在任意位置松开时提交落点，结束拖动会话。
     */
    private static final EventHandler<MouseEvent> SCENE_RELEASE_FILTER = e -> {
        if (!dragging || e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (dragSourceBar != null) {
            dragSourceBar.commitDrop();
        }
        e.consume();
        unregisterSceneHandlers();
    };
    private double pressScreenX, pressScreenY;

    public IdeActivityBar() {
        setMinWidth(40);
        setPrefWidth(40);
        setMaxWidth(40);
        setPadding(new Insets(6, 2, 6, 2));
        getStyleClass().add("ide-activity-bar");

        topBox.setAlignment(Pos.TOP_CENTER);
        topBox.setSpacing(2);
        bottomBox.setAlignment(Pos.BOTTOM_CENTER);
        bottomBox.setSpacing(2);

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);
        getChildren().addAll(topBox, filler, bottomBox);
    }

    private static void registerSceneHandlers(Scene scene) {
        activeScene = scene;
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, SCENE_DRAG_FILTER);
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, SCENE_RELEASE_FILTER);
    }

    // ═══════════════════════════════════════════════════════
    //  构造
    // ═══════════════════════════════════════════════════════

    private static void unregisterSceneHandlers() {
        if (activeScene != null) {
            activeScene.removeEventFilter(MouseEvent.MOUSE_DRAGGED, SCENE_DRAG_FILTER);
            activeScene.removeEventFilter(MouseEvent.MOUSE_RELEASED, SCENE_RELEASE_FILTER);
            activeScene = null;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  公共 API
    // ═══════════════════════════════════════════════════════

    /**
     * 声明此栏所在侧。true = 右侧，false = 左侧（默认）。
     * 条目被点击时将此值传给 action，使 action 能操作正确一侧的面板。
     * 条目被拖到对侧后，点击时自动使用目标栏的 rightBar 值。
     */
    public void setRightBar(boolean rightBar) {
        this.rightBar = rightBar;
    }

    /**
     * 关联对侧活动栏，启用跨侧拖拽。
     * 左栏与右栏应互相 setPeer，确保双向拖拽均可工作。
     */
    public void setPeer(IdeActivityBar peer) {
        this.peer = peer;
    }

    /**
     * 注入"关闭本侧面板"回调，跨侧拖动落点时自动调用。
     */
    public void setOnItemMoved(Runnable cb) {
        this.onItemMoved = cb;
    }

    /**
     * 注入"本侧面板是否可见"查询，跨侧拖动时决定是否在目标侧重新打开面板。
     */
    public void setPanelVisible(java.util.function.BooleanSupplier supplier) {
        this.panelVisible = supplier;
    }

    /**
     * 添加条目到顶部分区。
     *
     * @param action 点击回调；{@code isRight=true} 时表示此条目当前在右侧栏，动作应操作右侧面板。
     */
    public void addItem(String id, String icon, String tooltipText, BiConsumer<Boolean, Boolean> action) {
        addItem(id, icon, tooltipText, action, false);
    }

    /**
     * @param toBottom true = 加入底部分区
     */
    public void addItem(String id, String icon, String tooltipText, BiConsumer<Boolean, Boolean> action, boolean toBottom) {
        BarItem item = new BarItem(id, icon, tooltipText, action);
        Label btn = buildLabel(item);
        labelMap.put(id, btn);
        if (toBottom) {
            bottomItems.add(item);
            bottomBox.getChildren().add(btn);
        } else {
            topItems.add(item);
            topBox.getChildren().add(btn);
        }
    }

    /**
     * 设置激活项高亮；传空集合或 null 清除全部。
     */
    public void setActiveIds(Collection<String> ids) {
        labelMap.forEach((k, lbl) -> {
            lbl.getStyleClass().remove("ide-activity-bar-item-active");
            if (ids != null && ids.contains(k)) {
                lbl.getStyleClass().add("ide-activity-bar-item-active");
            }
        });
    }

    public boolean hasItems() {
        return !labelMap.isEmpty();
    }

    // ═══════════════════════════════════════════════════════
    //  Label 构建
    // ═══════════════════════════════════════════════════════

    private Label buildLabel(BarItem item) {
        Label btn = new Label(item.icon());
        btn.setMinSize(36, 36);
        btn.setPrefSize(36, 36);
        btn.setMaxSize(36, 36);
        btn.setAlignment(Pos.CENTER);
        btn.getStyleClass().add("ide-activity-bar-item");

        Tooltip tip = new Tooltip(item.tooltipText());
        tip.setShowDelay(Duration.millis(500));
        btn.setTooltip(tip);

        // PRESS：启动长按计时器
        btn.setOnMousePressed(e -> onPress(e, item));

        // DRAG（按钮级）：仅在计时器触发前检测是否移动过大以取消长按。
        // 真正的拖动逻辑由 Scene 级 SCENE_DRAG_FILTER 处理，此处不重复。
        btn.setOnMouseDragged(e -> {
            if (longPressTimer != null) {
                if (Math.abs(e.getScreenX() - pressScreenX) > 6 ||
                        Math.abs(e.getScreenY() - pressScreenY) > 6) {
                    stopTimer();
                }
            }
            if (dragging) {
                e.consume(); // 防止拖动结束时触发 click 事件
            }
        });

        // RELEASE（按钮级）：仅处理短点击。
        // 拖动释放由 Scene 级 SCENE_RELEASE_FILTER 处理（先于此处触发并 consume）。
        btn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY || dragging) {
                return;
            }
            stopTimer();
            // 使用当前所在栏的 rightBar 值，跨侧拖动后自动路由到正确面板
            item.action().accept(rightBar, bottomItems.contains(item));
            e.consume();
        });

        return btn;
    }

    // ═══════════════════════════════════════════════════════
    //  长按检测
    // ═══════════════════════════════════════════════════════

    private void onPress(MouseEvent e, BarItem item) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        pressScreenX = e.getScreenX();
        pressScreenY = e.getScreenY();
        longPressTimer = new PauseTransition(Duration.millis(LONG_PRESS_MS));
        longPressTimer.setOnFinished(ev -> beginDrag(item));
        longPressTimer.play();
    }

    private void stopTimer() {
        if (longPressTimer != null) {
            longPressTimer.stop();
            longPressTimer = null;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  拖动开始
    // ═══════════════════════════════════════════════════════

    private void beginDrag(BarItem item) {
        dragging = true;
        dragItem = item;
        dragSourceBar = this;
        longPressTimer = null;

        // 半透明化源图标，给用户视觉反馈
        Label srcBtn = labelMap.get(item.id());
        if (srcBtn != null) {
            srcBtn.setOpacity(0.3);
        }

        // 创建跟随鼠标的幽灵 Popup
        Label ghost = new Label(item.icon());
        ghost.setMinSize(36, 36);
        ghost.setPrefSize(36, 36);
        ghost.setMaxSize(36, 36);
        ghost.setAlignment(Pos.CENTER);
        ghost.setStyle(
                "-fx-background-color:rgba(75,158,255,0.25);" +
                        "-fx-border-color:#4b9eff;-fx-border-width:1;" +
                        "-fx-border-radius:4;-fx-background-radius:4;" +
                        "-fx-text-fill:#bbbbbb;-fx-font-size:17px;"
        );
        dragGhost = new Popup();
        dragGhost.getContent().add(ghost);
        dragGhost.setAutoFix(false);
        dragGhost.setAutoHide(false);
        var win = (getScene() != null) ? getScene().getWindow() : null;
        if (win != null) {
            dragGhost.show(win, pressScreenX - 18, pressScreenY - 18);
        }

        // 注册 Scene 级过滤器，使拖动事件不受按钮边界限制
        Scene scene = getScene();
        if (scene != null) {
            registerSceneHandlers(scene);
        }
    }

    private void moveGhost(double sx, double sy) {
        if (dragGhost != null && dragGhost.isShowing()) {
            dragGhost.setX(sx - 18);
            dragGhost.setY(sy - 18);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  吸附指示线
    // ═══════════════════════════════════════════════════════

    private void refreshSnap(double sx, double sy) {
        IdeActivityBar target = hitTest(sx, sy);
        if (target == null) {
            clearSnap();
            return;
        }

        boolean inBottom = isInBottomSection(target, sy);
        VBox box = inBottom ? target.bottomBox : target.topBox;
        int newIdx = computeInsertIdx(box, sy);

        // 位置未变则跳过，避免不必要的布局触发
        if (box.getChildren().indexOf(SNAP_LINE) == newIdx) {
            return;
        }

        clearSnap();
        int clamped = Math.max(0, Math.min(newIdx, box.getChildren().size()));
        box.getChildren().add(clamped, SNAP_LINE);
    }

    private IdeActivityBar hitTest(double sx, double sy) {
        if (containsScreen(sx, sy)) {
            return this;
        }
        if (peer != null && peer.containsScreen(sx, sy)) {
            return peer;
        }
        return null;
    }

    private boolean containsScreen(double sx, double sy) {
        if (getScene() == null) {
            return false;
        }
        var local = screenToLocal(sx, sy);
        return local != null && getBoundsInLocal().contains(local);
    }

    private boolean isInBottomSection(IdeActivityBar bar, double sy) {
        // 两侧均有条目时，以两 Box 之间的中点作为分界
        if (!bar.topItems.isEmpty() && !bar.bottomItems.isEmpty()) {
            double topBottom = bar.topBox.localToScreen(0, bar.topBox.getHeight()).getY();
            double bottomStart = bar.bottomBox.localToScreen(0, 0).getY();
            return sy > (topBottom + bottomStart) / 2.0;
        }
        // 否则（任意一侧为空，包括两侧均空）按屏幕高度比例：
        // 上方 60% → 顶部分区，下方 40% → 底部分区
        var bounds = bar.localToScreen(bar.getBoundsInLocal());
        return bounds != null && sy > bounds.getMinY() + bounds.getHeight() * 0.6;
    }

    private int computeInsertIdx(VBox box, double sy) {
        int idx = 0;
        for (int i = 0; i < box.getChildren().size(); i++) {
            var child = box.getChildren().get(i);
            if (child == SNAP_LINE) {
                continue;
            }
            var b = child.localToScreen(child.getBoundsInLocal());
            if (b != null && sy > b.getMinY() + b.getHeight() / 2.0) {
                idx = i + 1;
            }
        }
        return idx;
    }

    private void clearSnap() {
        if (SNAP_LINE.getParent() instanceof VBox v) {
            v.getChildren().remove(SNAP_LINE);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  提交落点
    // ═══════════════════════════════════════════════════════

    private void commitDrop() {
        dragging = false;
        stopTimer();

        if (dragGhost != null) {
            dragGhost.hide();
            dragGhost = null;
        }

        Label srcLabel = dragSourceBar.labelMap.get(dragItem.id());
        if (srcLabel != null) {
            srcLabel.setOpacity(1.0);
        }

        VBox snapBox = findSnapBox();
        if (snapBox == null) {
            clearSnap();
            resetDragState();
            return;
        }

        IdeActivityBar targetBar = resolveTargetBar(snapBox);
        boolean inBottom = (snapBox == targetBar.bottomBox);

        // SNAP_LINE 前的真实子节点数 = 目标列表插入位置
        int snapIdx = snapBox.getChildren().indexOf(SNAP_LINE);
        int insertPos = countRealBefore(snapBox, snapIdx);

        // 拖动前所在分区（移除前捕获）
        boolean wasInBottom = dragSourceBar.bottomItems.contains(dragItem);

        // 同 bar 同 box：若源条目在插入点之前，移除后插入位置前移一位
        List<BarItem> sameList = (targetBar == dragSourceBar)
                ? (inBottom ? dragSourceBar.bottomItems : dragSourceBar.topItems)
                : null;
        int srcIdx = (sameList != null) ? sameList.indexOf(dragItem) : -1;

        clearSnap();
        dragSourceBar.removeItem(dragItem);

        if (srcIdx >= 0 && srcIdx < insertPos) {
            insertPos--;
        }

        List<BarItem> targetList = inBottom ? targetBar.bottomItems : targetBar.topItems;
        insertPos = Math.max(0, Math.min(insertPos, targetList.size()));

        targetList.add(insertPos, dragItem);

        Label finalLabel;
        if (targetBar != dragSourceBar) {
            // 跨侧：记录源侧面板可见状态，关闭源侧面板，重建 Label（闭包捕获目标栏）
            boolean wasVisible = dragSourceBar.panelVisible.getAsBoolean();
            if (dragSourceBar.onItemMoved != null) {
                dragSourceBar.onItemMoved.run();
            }
            finalLabel = targetBar.buildLabel(dragItem);
            targetBar.labelMap.put(dragItem.id(), finalLabel);
            rebuildBox(inBottom ? targetBar.bottomBox : targetBar.topBox, targetList, targetBar.labelMap);
            // 若源侧面板原本可见，在目标侧重新打开
            if (wasVisible) {
                dragItem.action().accept(targetBar.rightBar, targetBar.bottomItems.contains(dragItem));
            }
        } else {
            finalLabel = srcLabel;
            targetBar.labelMap.put(dragItem.id(), finalLabel);
            rebuildBox(inBottom ? targetBar.bottomBox : targetBar.topBox, targetList, targetBar.labelMap);
            // 同侧跨分区（top↔bottom）：关闭旧分区面板，在新分区重新打开
            if (wasInBottom != inBottom) {
                // 先关旧槽（用旧 isBottom 调用 action，toggleSlot 会 toggle → 关闭）
                dragItem.action().accept(targetBar.rightBar, wasInBottom);
                // 再开新槽
                dragItem.action().accept(targetBar.rightBar, inBottom);
            }
        }

        resetDragState();
    }

    private VBox findSnapBox() {
        if (topBox.getChildren().contains(SNAP_LINE)) {
            return topBox;
        }
        if (bottomBox.getChildren().contains(SNAP_LINE)) {
            return bottomBox;
        }
        if (peer != null) {
            if (peer.topBox.getChildren().contains(SNAP_LINE)) {
                return peer.topBox;
            }
            if (peer.bottomBox.getChildren().contains(SNAP_LINE)) {
                return peer.bottomBox;
            }
        }
        return null;
    }

    private IdeActivityBar resolveTargetBar(VBox box) {
        if (box == topBox || box == bottomBox) {
            return this;
        }
        return (peer != null) ? peer : this;
    }

    private int countRealBefore(VBox box, int before) {
        int n = 0;
        for (int i = 0; i < before; i++) {
            if (box.getChildren().get(i) != SNAP_LINE) {
                n++;
            }
        }
        return n;
    }

    private void removeItem(BarItem item) {
        Label btn = labelMap.remove(item.id());
        topItems.remove(item);
        bottomItems.remove(item);
        if (btn != null) {
            topBox.getChildren().remove(btn);
            bottomBox.getChildren().remove(btn);
        }
    }

    private void rebuildBox(VBox box, List<BarItem> list, Map<String, Label> map) {
        box.getChildren().clear();
        for (BarItem bi : list) {
            Label lbl = map.get(bi.id());
            if (lbl != null) {
                box.getChildren().add(lbl);
            }
        }
    }

    private void resetDragState() {
        dragItem = null;
        dragSourceBar = null;
    }

    // ═══════════════════════════════════════════════════════
    //  Scene 级过滤器注册 / 注销
    // ═══════════════════════════════════════════════════════

    // ── 条目数据模型 ───────────────────────────────────────
    private record BarItem(String id, String icon, String tooltipText, BiConsumer<Boolean, Boolean> action) {
    }


}
