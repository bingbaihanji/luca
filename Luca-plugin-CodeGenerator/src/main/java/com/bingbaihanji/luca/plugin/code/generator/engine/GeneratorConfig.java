package com.bingbaihanji.luca.plugin.code.generator.engine;

import lombok.Data;

/**
 * 代码生成器配置模型：控制输出路径、包结构、生成开关与代码风格。
 * 由 GeneratorConfigDialog 界面收集用户输入后填充，传递给 CodeGeneratorEngine。
 */
@Data
public class GeneratorConfig {
    /**
     * Java 源文件输出根目录，如 /home/dev/myproject/src/main/java
     */
    private String outputPath;
    /**
     * resources 目录，Mapper XML 写入 resources/mapper/，如 /home/dev/myproject/src/main/resources
     */
    private String resourcesPath;
    /**
     * 基础包名，如 com.example.demo，后续子包在此基础上拼接
     */
    private String basePackage;
    /**
     * 要去掉的表名前缀，如 t_（为空则不去除）
     */
    private String tablePrefix = "";
    /**
     * 作者名，用于生成类注释中的 @author
     */
    private String author = System.getProperty("user.name", "bingbaihanji");

    // 各层子包名（相对于 basePackage）
    private String entitySubPackage = "entity";        // Entity 层子包
    private String mapperSubPackage = "mapper";        // Mapper 接口层子包
    private String serviceSubPackage = "service";      // Service 接口层子包
    private String serviceImplSubPackage = "service.impl"; // ServiceImpl 层子包
    private String controllerSubPackage = "controller";    // Controller 层子包

    // 生成选项开关：控制是否生成对应文件
    private boolean generateEntity = true;       // 是否生成 Entity
    private boolean generateMapper = true;       // 是否生成 Mapper 接口
    private boolean generateMapperXml = true;    // 是否生成 Mapper XML
    private boolean generateService = true;      // 是否生成 Service 接口
    private boolean generateServiceImpl = true;  // 是否生成 ServiceImpl
    private boolean generateController = true;   // 是否生成 Controller
    private boolean useLombok = true;            // 是否在 Entity 中使用 Lombok（@Data 等）
}
