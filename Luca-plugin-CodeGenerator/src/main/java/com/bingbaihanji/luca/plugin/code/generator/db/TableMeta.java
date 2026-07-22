package com.bingbaihanji.luca.plugin.code.generator.db;

import lombok.Data;

/**
 * 数据库表元数据模型，用于承载单张表的基础信息。
 * 通过 DatabaseMetaData#getTables 读取后填充。
 */
@Data
public class TableMeta {
    private String tableName;   // 数据库原始表名，如 t_user / sys_role
    private String remarks;     // 表注释（数据库 COMMENT），代码生成时可作为类注释内容
}
