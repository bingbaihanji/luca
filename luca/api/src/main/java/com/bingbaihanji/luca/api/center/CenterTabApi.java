package com.bingbaihanji.luca.api.center;

import com.bingbaihanji.sunsen.api.annotation.ExtensionPoint;
import javafx.scene.Node;

import java.util.List;

/**
 * 中心编辑器标签页扩展点。
 * <p>
 * 插件实现此接口后，宿主将在启动时调用 {@link #getTabs()} 并把每个条目
 * 通过 {@code LucaTabPane.addTab(id, title, content)} 注入中心区。
 * <p>
 * 示例：音乐播放器插件可提供一个"播放器"标签页，文本编辑器插件可提供
 * 多个"文件"标签页。
 */
@ExtensionPoint(id = "editor:tab", description = "中心编辑器标签页扩展")
public interface CenterTabApi {

    /**
     * 返回此插件希望在启动时预置的标签页列表。
     * 每次调用应返回同一批对象（宿主只在启动时调用一次）。
     */
    List<TabDescriptor> getTabs();

    /**
     * 描述一个标签页。
     *
     * @param id      全局唯一标识，建议用 "插件名:功能" 格式
     * @param title   标签栏显示的标题
     * @param content 标签页内容节点
     */
    record TabDescriptor(String id, String title, Node content) {
    }
}