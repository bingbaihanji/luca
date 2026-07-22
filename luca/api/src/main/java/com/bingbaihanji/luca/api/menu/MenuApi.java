package com.bingbaihanji.luca.api.menu;

import com.bingbaihanji.sunsen.api.annotation.ExtensionPoint;
import javafx.scene.control.MenuBar;

@ExtensionPoint(id = "menuBar:menu", description = "菜单拓展")
public interface MenuApi {

    /**
     * 向目标菜单添加子菜单项。<br>
     * 实现类通过调用{@code parentMenu.getItems().add(...)} 添加零个、一个或多个子菜单项，
     * 从而决定该菜单下有多少子菜单以及具体的菜单行为。
     *
     * @param parentMenu 目标菜单实例，不会被为null
     */
    void extend(MenuBar parentMenu);

    /**
     * 可选回调：宿主在调用 {@link #extend} 之前注入标签页访问器，
     * 供需要动态增删标签的菜单扩展使用。默认实现为空操作，不影响已有插件。
     */
    default void bindTabPane(TabPaneAccessor accessor) {}
}
