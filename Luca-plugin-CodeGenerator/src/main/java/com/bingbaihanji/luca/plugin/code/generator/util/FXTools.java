package com.bingbaihanji.luca.plugin.code.generator.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;

/**
 *
 * @author bingbaihanji
 * @date 2026-05-18 10:32:04
 * @description //TODO
 */

@SuppressWarnings("deprecation")
public final class FXTools {

    private FXTools() {
        throw new IllegalArgumentException("Utility class");
    }

    // 设置弹窗标题栏
    public static HeaderBar createpopUpBoxHeaderBar(Node node) {
        HeaderBar headerBar = new HeaderBar();
        headerBar.setLeading(node);
        // 设置Label组件可拖动
        HeaderBar.setDragType(node, HeaderDragType.DRAGGABLE);
        HeaderBar.setMargin(node, new Insets(0, 0, 0, 12));
        HeaderBar.setAlignment(node, Pos.CENTER_LEFT);

        return headerBar;
    }


}
