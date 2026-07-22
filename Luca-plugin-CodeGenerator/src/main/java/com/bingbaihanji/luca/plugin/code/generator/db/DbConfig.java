package com.bingbaihanji.luca.plugin.code.generator.db;

import lombok.Data;

/**
 * 数据库连接配置模型，封装 JDBC 连接所需的全部参数。
 * 支持通过内置预设方法快速生成 MySQL / PostgreSQL 配置。
 */
@Data
public class DbConfig {
    /**
     * JDBC 驱动类名，如 com.mysql.cj.jdbc.Driver
     */
    private String driverClass;
    /**
     * JDBC URL，如 jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai
     */
    private String url;
    /**
     * 数据库登录用户名
     */
    private String username;
    /**
     * 数据库登录密码
     */
    private String password;
    /**
     * schema/database 名称，用于 DatabaseMetaData 查询
     * MySQL 传数据库名，PostgreSQL 传 schema（通常 "public"）
     */
    private String schemaName;

    /**
     * 内置 MySQL 预设：根据主机、端口、数据库名、用户名、密码快速构建 DbConfig
     */
    public static DbConfig mysqlPreset(String host, int port, String db, String user, String pass) {
        DbConfig c = new DbConfig();
        c.driverClass = "com.mysql.cj.jdbc.Driver";          // 固定使用 MySQL 8 驱动
        // 组装 JDBC URL，关闭 SSL、指定时区与编码，避免连接警告
        c.url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8";
        c.username = user;
        c.password = pass;
        c.schemaName = db;                                    // MySQL 的 schemaName 即数据库名
        return c;
    }

    /**
     * 内置 PostgreSQL 预设：根据主机、端口、数据库名、用户名、密码快速构建 DbConfig
     */
    public static DbConfig postgresPreset(String host, int port, String db, String user, String pass) {
        DbConfig c = new DbConfig();
        c.driverClass = "org.postgresql.Driver";             // 固定使用 PostgreSQL 驱动
        c.url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        c.username = user;
        c.password = pass;
        c.schemaName = "public";                              // PostgreSQL 默认 schema 为 public
        return c;
    }
}
