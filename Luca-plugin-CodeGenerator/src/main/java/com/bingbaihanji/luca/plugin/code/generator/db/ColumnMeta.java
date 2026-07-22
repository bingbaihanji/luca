package com.bingbaihanji.luca.plugin.code.generator.db;

import lombok.Data;

/**
 * 数据库列元数据模型，用于承载单张表某一列的结构信息。
 * 通过 DatabaseMetaData 读取后填充，供代码生成器作为模板渲染的数据源。
 */
@Data
public class ColumnMeta {
    private String columnName;      // 数据库列名，如 user_name
    private String javaField;       // Java 字段名（下划线转小驼峰），如 userName
    private int jdbcType;           // java.sql.Types 常量值，用于类型映射
    private String jdbcTypeName;    // JDBC 类型名，如 VARCHAR / BIGINT
    private String javaShortType;   // Java 短类型名（不含包名），如 String / LocalDateTime
    private String javaFullType;    // Java 全限定类名，如 java.time.LocalDateTime
    private int size;               // 列长度/精度，如 VARCHAR(255) 中的 255
    private String remarks;         // 列注释（数据库 COMMENT）
    private boolean nullable;       // 是否允许为 NULL
    private boolean autoIncrement;  // 是否为自增列
    private boolean primaryKey;     // 是否为主键列
}
