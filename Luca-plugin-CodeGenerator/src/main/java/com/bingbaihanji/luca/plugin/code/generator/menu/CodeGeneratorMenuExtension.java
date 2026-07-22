package com.bingbaihanji.luca.plugin.code.generator.menu;

import com.bingbaihanji.luca.api.menu.MenuApi;
import com.bingbaihanji.luca.plugin.code.generator.ui.DbConnectionDialog;
import com.bingbaihanji.sunsen.api.annotation.Extension;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

/**
 * 菜单扩展：将「代码生成器」入口注册到 luca 主框架的「工具」菜单下。
 * 通过实现 MenuApi 接口并标注 @Extension，由 Sunsen 插件框架自动发现并注入。
 */
@Extension
public class CodeGeneratorMenuExtension implements MenuApi {

    @Override
    public void extend(MenuBar parentMenu) {
        // 注意：此方法在 JavaFX Application Thread 上调用（由 lucaApp 在 start() 中调用）
        for (Menu menu : parentMenu.getMenus()) {
            if (menu.getText().startsWith("工具")) {               // 定位「工具」菜单
                MenuItem item = new MenuItem("代码生成器(G)");     // 新建菜单项
                item.setOnAction(e -> new DbConnectionDialog().show()); // 点击打开数据库连接对话框
                menu.getItems().add(item);                         // 将菜单项追加到「工具」菜单
                return;
            }
        }
    }
}
