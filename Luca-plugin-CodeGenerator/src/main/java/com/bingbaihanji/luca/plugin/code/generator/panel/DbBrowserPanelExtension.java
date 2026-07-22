package com.bingbaihanji.luca.plugin.code.generator.panel;

import com.bingbaihanji.luca.api.panel.LeftPanelApi;
import com.bingbaihanji.sunsen.api.annotation.Extension;
import javafx.scene.Node;

/**
 * 左侧边栏扩展：将数据库浏览器面板注册到 luca 主框架的左侧边栏。
 * 通过实现 LeftPanelApi 接口并标注 @Extension，由 Sunsen 插件框架自动发现并注入。
 */
@Extension
public class DbBrowserPanelExtension implements LeftPanelApi {
    @Override
    public Node getContent() {
        return new DbBrowserPanel();   // 返回数据库浏览器面板实例
    }
}
