package com.bingbaihanji.luca.plugin.code.generator;

import com.bingbaihanji.sunsen.api.annotation.Plugin;
import com.bingbaihanji.sunsen.api.support.AbstractPlugin;

/**
 * 代码生成器插件入口类，通过 Sunsen 插件框架注册到 luca UI 平台。
 * 该插件提供数据库表结构浏览与 Java 代码自动生成能力（Entity / Mapper / Service / Controller）。
 */
@Plugin(
        id = "com.bingbaihanji.luca.plugin.codeGenerator",   // 插件唯一标识，用于插件管理器识别与依赖解析
        name = "Code Generator",                               // 插件展示名称
        version = "1.0.0",                                     // 插件版本号
        packagePrefixes = {"com.bingbaihanji.luca.plugin.code.generator"}     // 插件扫描包前缀，Sunsen 会从此包开始扫描 @Extension
)
public class CodeGeneratorPlugin extends AbstractPlugin {

    @Override
    protected void onInitialized() {
        // onInitialized 等同于 onInit 后的回调，此阶段禁止访问其他插件的扩展
        // 如有本地资源初始化（如读取配置文件），可在此处进行
    }

    @Override
    public void onStart() {
        // 所有插件的扩展均已注册后才会调用此方法
        // 如需访问其他插件的扩展，在此处进行
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onDestroy() {
    }

}
