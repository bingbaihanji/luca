package com.bingbaihanji.luca.plugin.code.generator.db;

import java.sql.Types;

/**
 * JDBC 类型与 Java 类型的映射工具类。
 * 依据 java.sql.Types 常量，将数据库类型转换为对应的 Java 类型字符串。
 */
public class JdbcTypeMapper {

    /**
     * 将 JDBC 类型转换为 Java 短类型名（不含包名），用于类字段声明与泛型参数
     */
    public static String toShortType(int jdbcType) {
        return switch (jdbcType) {
            case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.CLOB, Types.NVARCHAR -> "String";
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> "Integer";
            case Types.BIGINT -> "Long";
            case Types.FLOAT, Types.REAL -> "Float";
            case Types.DOUBLE -> "Double";
            case Types.DECIMAL, Types.NUMERIC -> "BigDecimal";          // 金额/高精度数值类型
            case Types.DATE -> "LocalDate";                             // JSR-310 日期类型
            case Types.TIME -> "LocalTime";                             // JSR-310 时间类型
            case Types.TIMESTAMP -> "LocalDateTime";                    // JSR-310 日期时间类型
            case Types.BOOLEAN, Types.BIT -> "Boolean";
            case Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> "byte[]";
            default -> "String";                                        // 未知类型默认兜底为 String
        };
    }

    /**
     * 将 JDBC 类型转换为 Java 全限定类名，用于模板中 import 语句与完全限定引用
     */
    public static String toFullType(int jdbcType) {
        return switch (jdbcType) {
            case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.CLOB, Types.NVARCHAR -> "java.lang.String";
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> "java.lang.Integer";
            case Types.BIGINT -> "java.lang.Long";
            case Types.FLOAT, Types.REAL -> "java.lang.Float";
            case Types.DOUBLE -> "java.lang.Double";
            case Types.DECIMAL, Types.NUMERIC -> "java.math.BigDecimal";
            case Types.DATE -> "java.time.LocalDate";
            case Types.TIME -> "java.time.LocalTime";
            case Types.TIMESTAMP -> "java.time.LocalDateTime";
            case Types.BOOLEAN, Types.BIT -> "java.lang.Boolean";
            case Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> "byte[]";
            default -> "java.lang.String";
        };
    }

    /**
     * 判断该 JDBC 类型是否需要显式 import（即不属于 java.lang 包或需要额外导包）
     * 返回 true 的类型：BigDecimal、LocalDate、LocalTime、LocalDateTime
     */
    public static boolean needsImport(int jdbcType) {
        return switch (jdbcType) {
            case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.CLOB, Types.NVARCHAR,
                 Types.INTEGER, Types.SMALLINT, Types.TINYINT,
                 Types.BIGINT,
                 Types.FLOAT, Types.REAL,
                 Types.DOUBLE,
                 Types.BOOLEAN, Types.BIT,
                 Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> false;
            case Types.DECIMAL, Types.NUMERIC,       // 需要 import java.math.BigDecimal
                 Types.DATE,                          // 需要 import java.time.LocalDate
                 Types.TIME,                          // 需要 import java.time.LocalTime
                 Types.TIMESTAMP -> true;              // 需要 import java.time.LocalDateTime
            default -> false;
        };
    }
}
