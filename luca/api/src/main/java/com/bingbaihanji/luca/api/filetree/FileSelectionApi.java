package com.bingbaihanji.luca.api.filetree;

import com.bingbaihanji.sunsen.api.annotation.ExtensionPoint;

import java.nio.file.Path;

/**
 * 文件选中事件监听扩展点。
 * <p>
 * 插件实现此接口后，每当用户在"目录"面板中单击一个文件或目录时，
 * 宿主会调用 {@link #onFileSelected(Path, boolean)} 通知所有订阅者。
 * <p>
 * 典型用途：
 * <ul>
 *   <li>文本编辑器插件 — 收到 .txt / .java 等文件路径后在中心区打开</li>
 *   <li>音乐播放器插件 — 收到 .mp3 / .flac 等路径后开始播放</li>
 *   <li>图片查看器插件 — 收到图片路径后在标签页显示</li>
 * </ul>
 */
@ExtensionPoint(id = "file:selection", description = "文件选中事件监听")
public interface FileSelectionApi {

    /**
     * 用户在目录树中选中条目时被调用。
     *
     * @param path        被选中的文件或目录绝对路径
     * @param isDirectory true = 目录，false = 文件
     */
    void onFileSelected(Path path, boolean isDirectory);
}
