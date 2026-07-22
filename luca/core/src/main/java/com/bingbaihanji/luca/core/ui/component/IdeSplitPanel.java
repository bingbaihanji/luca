package com.bingbaihanji.luca.core.ui.component;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 管理 0~2 个竖向排列的 IdeToolWindow 槽位。
 * <ul>
 *   <li>count==0: 不可见</li>
 *   <li>count==1: 单个 IdeToolWindow，无 SplitPane 开销</li>
 *   <li>count==2: 竖向 SplitPane，各占 50%</li>
 * </ul>
 */
public class IdeSplitPanel extends Region {

    private static final int MAX_SLOTS = 2;

    private final String[] ids = new String[MAX_SLOTS];
    private final IdeToolWindow[] windows = new IdeToolWindow[MAX_SLOTS];
    private final SplitPane splitPane = new SplitPane();
    private int count = 0;
    private int activeCount = 0;

    public IdeSplitPanel() {
        splitPane.setOrientation(Orientation.VERTICAL);
        setMinWidth(0);
        setMinHeight(0);
        syncView();
    }

    // ── 公共 API ────────────────────────────────────────────────

    public void setSlot(int idx, String id, Node content, String title, Runnable onClose) {
        if (idx < 0 || idx >= MAX_SLOTS) {
            throw new IllegalArgumentException("idx must be 0 or 1");
        }

        // Replace existing slot
        if (ids[idx] != null) {
            clearSlot(idx);
        }

        ids[idx] = id;
        windows[idx] = new IdeToolWindow(title, content, onClose);
        count = countFilled();
        syncView();
    }

    public void clearSlot(int idx) {
        if (idx < 0 || idx >= MAX_SLOTS) {
            return;
        }
        ids[idx] = null;
        windows[idx] = null;
        count = countFilled();
        syncView();
    }

    /**
     * Compact: if slot 0 is empty but slot 1 has content, shift slot 1 → slot 0.
     */
    public void compact() {
        if (ids[0] == null && ids[1] != null) {
            ids[0] = ids[1];
            ids[1] = null;
            windows[0] = windows[1];
            windows[1] = null;
        }
        count = countFilled();
        syncView();
    }

    public boolean containsId(String id) {
        if (id == null) {
            return false;
        }
        return id.equals(ids[0]) || id.equals(ids[1]);
    }

    /**
     * Returns 0 or 1, or -1 if not found.
     */
    public int slotOf(String id) {
        if (id == null) {
            return -1;
        }
        if (id.equals(ids[0])) {
            return 0;
        }
        if (id.equals(ids[1])) {
            return 1;
        }
        return -1;
    }

    public int count() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Returns the id at slot idx, or null.
     */
    public String idAt(int idx) {
        return (idx >= 0 && idx < MAX_SLOTS) ? ids[idx] : null;
    }

    // ── 内部 ────────────────────────────────────────────────────

    private int countFilled() {
        int n = 0;
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (ids[i] != null) {
                n++;
            }
        }
        return n;
    }

    private void syncView() {
        getChildren().clear();
        if (count == 0) {
            setVisible(false);
            setManaged(false);
            return;
        }
        setVisible(true);
        setManaged(true);

        if (count == 1) {
            // Find the single non-null window
            IdeToolWindow win = (windows[0] != null) ? windows[0] : windows[1];
            win.setPrefWidth(Double.MAX_VALUE);
            win.setPrefHeight(Double.MAX_VALUE);
            VBox.setVgrow(win, Priority.ALWAYS);
            getChildren().add(win);
        } else {
            // count == 2
            splitPane.getItems().setAll(windows[0], windows[1]);
            splitPane.setDividerPositions(0.5);
            splitPane.setPrefWidth(Double.MAX_VALUE);
            splitPane.setPrefHeight(Double.MAX_VALUE);
            getChildren().add(splitPane);
        }
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        for (Node child : getChildren()) {
            child.resize(w, h);
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        if (count == 0) {
            return 0;
        }
        double max = 0;
        for (Node child : getChildren()) {
            max = Math.max(max, child.prefWidth(height));
        }
        return max;
    }

    @Override
    protected double computePrefHeight(double width) {
        if (count == 0) {
            return 0;
        }
        double max = 0;
        for (Node child : getChildren()) {
            max = Math.max(max, child.prefHeight(width));
        }
        return max;
    }
}
