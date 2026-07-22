package com.bingbaihanji.luca.plugin.code.generator.engine;

import com.bingbaihanji.luca.plugin.code.generator.db.ColumnMeta;
import com.bingbaihanji.luca.plugin.code.generator.db.JdbcTypeMapper;
import com.bingbaihanji.luca.plugin.code.generator.db.TableMeta;
import com.bingbaihanji.luca.plugin.code.generator.util.NamingUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;

/**
 * 代码生成引擎：基于 FreeMarker 模板，根据表元数据批量生成 Java 源码与 Mapper XML。
 * 生成范围包括：Entity、Mapper（接口+XML）、Service（接口+实现）、Controller。
 */
public class CodeGeneratorEngine {

    // FreeMarker 配置对象，全局复用，避免每次生成时重复初始化
    private final Configuration cfg;

    public CodeGeneratorEngine() {
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        // 从类路径 /templates 目录加载模板文件（entity.ftl / mapper.ftl / ...）
        cfg.setClassForTemplateLoading(CodeGeneratorEngine.class, "/templates");
        cfg.setDefaultEncoding("UTF-8");
        // 模板异常时直接抛出，便于定位渲染错误
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
    }

    /**
     * 根据单张表的元数据与生成配置，批量输出全部文件。
     * 步骤：
     * 1) 推导基础类名（去表前缀 -> 转大驼峰）
     * 2) 计算各层包名与全限定类名
     * 3) 确定主键类型与需要 import 的类型集合
     * 4) 构建 FreeMarker 数据模型 root
     * 5) 根据 GeneratorConfig 开关，逐个模板渲染并写入磁盘
     */
    public void generateAll(TableMeta table, List<ColumnMeta> columns, GeneratorConfig config) throws Exception {
        // 基础类名推导：去除表前缀后转为 PascalCase，如 t_user_info -> UserInfo
        String baseName = NamingUtils.toPascalCase(
                NamingUtils.stripTablePrefix(table.getTableName(), config.getTablePrefix())
        );

        // Entity 包名与全限定类名
        String entityPkg = config.getBasePackage() + "." + config.getEntitySubPackage();
        String entityFullClass = entityPkg + "." + baseName;

        // Mapper 接口包名与全限定类名
        String mapperClass = baseName + "Mapper";
        String mapperPkg = config.getBasePackage() + "." + config.getMapperSubPackage();
        String mapperFullClass = mapperPkg + "." + mapperClass;

        // Service 接口包名与全限定类名
        String serviceClass = baseName + "Service";
        String servicePkg = config.getBasePackage() + "." + config.getServiceSubPackage();
        String serviceFullClass = servicePkg + "." + serviceClass;

        // ServiceImpl 包名与全限定类名
        String serviceImplClass = baseName + "ServiceImpl";
        String serviceImplPkg = config.getBasePackage() + "." + config.getServiceImplSubPackage();
        String serviceImplFullClass = serviceImplPkg + "." + serviceImplClass;

        // Controller 包名与全限定类名
        String controllerClass = baseName + "Controller";
        String controllerPkg = config.getBasePackage() + "." + config.getControllerSubPackage();
        String controllerFullClass = controllerPkg + "." + controllerClass;

        // 主键信息：优先取标记为主键的列；若无主键则取第一列；若列空则默认 Long
        ColumnMeta pk = columns.stream().filter(ColumnMeta::isPrimaryKey).findFirst()
                .orElse(columns.isEmpty() ? null : columns.get(0));
        String pkType = (pk != null) ? pk.getJavaShortType() : "Long";

        // 收集需要 import 的 Java 类型（排除 java.lang 下的类型）
        Set<String> imports = new LinkedHashSet<>();
        for (ColumnMeta col : columns) {
            if (JdbcTypeMapper.needsImport(col.getJdbcType())) {
                imports.add(col.getJavaFullType());
            }
        }

        boolean hasPk = columns.stream().anyMatch(ColumnMeta::isPrimaryKey);   // 是否存在主键
        String date = LocalDate.now().toString();                              // 生成日期（类注释用）
        String urlPrefix = NamingUtils.toLowerFirst(baseName);                 // URL 路由前缀，如 UserInfo -> userInfo
        String serviceField = NamingUtils.toLowerFirst(serviceClass);          // 依赖注入字段名，如 userInfoService

        // 构建 FreeMarker 数据模型，供所有模板共享
        Map<String, Object> root = new HashMap<>();
        root.put("tableName", table.getTableName());
        root.put("tableRemarks", table.getRemarks());
        root.put("author", config.getAuthor());
        root.put("date", date);
        root.put("baseName", baseName);
        root.put("entityClass", baseName);
        root.put("entityFullClass", entityFullClass);
        root.put("mapperClass", mapperClass);
        root.put("mapperFullClass", mapperFullClass);
        root.put("serviceClass", serviceClass);
        root.put("serviceFullClass", serviceFullClass);
        root.put("serviceInterfaceClass", serviceClass);
        root.put("serviceInterfaceFullClass", serviceFullClass);
        root.put("serviceImplClass", serviceImplClass);
        root.put("serviceImplFullClass", serviceImplFullClass);
        root.put("controllerClass", controllerClass);
        root.put("controllerFullClass", controllerFullClass);
        root.put("pkType", pkType);
        root.put("urlPrefix", urlPrefix);
        root.put("serviceField", serviceField);
        root.put("columns", columns);
        root.put("imports", imports);
        root.put("hasPk", hasPk);
        root.put("useLombok", config.isUseLombok());

        // 生成 Entity
        if (config.isGenerateEntity()) {
            Map<String, Object> data = new HashMap<>(root);
            data.put("packageName", entityPkg);
            data.put("className", baseName);
            writeFile(cfg.getTemplate("entity.ftl"), data,
                    config.getOutputPath(), entityPkg, baseName + ".java");
        }

        // 生成 Mapper 接口
        if (config.isGenerateMapper()) {
            Map<String, Object> data = new HashMap<>(root);
            data.put("packageName", mapperPkg);
            data.put("className", mapperClass);
            writeFile(cfg.getTemplate("mapper.ftl"), data,
                    config.getOutputPath(), mapperPkg, mapperClass + ".java");
        }

        // 生成 Mapper XML（写入 resources/mapper/ 目录）
        if (config.isGenerateMapperXml()) {
            Map<String, Object> data = new HashMap<>(root);
            data.put("packageName", mapperPkg);
            data.put("className", mapperClass);
            String xmlName = baseName + "Mapper.xml";
            File dir = new File(config.getResourcesPath(), "mapper");
            Files.createDirectories(dir.toPath());
            File file = new File(dir, xmlName);
            try (Writer out = new FileWriter(file)) {
                cfg.getTemplate("mapperXml.ftl").process(data, out);
            }
        }

        // 生成 Service 接口
        if (config.isGenerateService()) {
            Map<String, Object> data = new HashMap<>(root);
            data.put("packageName", servicePkg);
            data.put("className", serviceClass);
            writeFile(cfg.getTemplate("serviceInterface.ftl"), data,
                    config.getOutputPath(), servicePkg, serviceClass + ".java");
        }

        // 生成 ServiceImpl
        if (config.isGenerateServiceImpl()) {
            Map<String, Object> data = new HashMap<>(root);
            data.put("packageName", serviceImplPkg);
            data.put("className", serviceImplClass);
            writeFile(cfg.getTemplate("serviceImpl.ftl"), data,
                    config.getOutputPath(), serviceImplPkg, serviceImplClass + ".java");
        }

        // 生成 Controller
        if (config.isGenerateController()) {
            Map<String, Object> data = new HashMap<>(root);
            data.put("packageName", controllerPkg);
            data.put("className", controllerClass);
            writeFile(cfg.getTemplate("controller.ftl"), data,
                    config.getOutputPath(), controllerPkg, controllerClass + ".java");
        }
    }

    /**
     * 通用文件写入方法：将 FreeMarker 模板渲染结果写入指定路径。
     * 步骤：1) 将包名转为目录路径  2) 递归创建目录  3) 渲染模板并写入文件
     */
    private void writeFile(Template template, Map<String, Object> data, String outputPath, String packageName, String fileName) throws Exception {
        String packagePath = packageName.replace('.', File.separatorChar);   // 包路径转文件路径
        File dir = new File(outputPath, packagePath);
        Files.createDirectories(dir.toPath());                              // 若目录不存在则自动创建
        File file = new File(dir, fileName);
        try (Writer out = new FileWriter(file)) {
            template.process(data, out);                                    // FreeMarker 渲染输出
        }
    }
}
