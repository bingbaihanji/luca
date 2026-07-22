package com.bingbaihanji.luca.plugin.code.generator.db;

import com.bingbaihanji.luca.plugin.code.generator.util.NamingUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据库元数据服务：负责 JDBC 连接的建立、表列表查询、列结构查询。
 * 所有数据库操作均通过标准 java.sql.DatabaseMetaData 完成，不依赖特定数据库方言。
 */
public class DbMetadataService {

    /**
     * 建立数据库连接
     * 步骤：1) 反射加载 JDBC 驱动类  2) 通过 DriverManager 获取连接
     */
    public Connection connect(DbConfig config) throws Exception {
        Class.forName(config.getDriverClass());   // 显式加载驱动，确保 ClassLoader 注册该驱动
        return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
    }

    /**
     * 列出指定 schema 下的所有表
     * 步骤：1) 获取 DatabaseMetaData  2) 调用 getTables 查询类型为 TABLE 的表  3) 逐行封装为 TableMeta
     *
     * @param schema 数据库名/schema 名，MySQL 传数据库名，PostgreSQL 传 schema（通常 "public"）
     */
    public List<TableMeta> listTables(Connection conn, String schema) throws Exception {
        List<TableMeta> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        // MySQL: catalog=schema, schemaPattern=null; PostgreSQL: catalog=null, schemaPattern=schema
        try (ResultSet rs = meta.getTables(schema, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                TableMeta tm = new TableMeta();
                tm.setTableName(rs.getString("TABLE_NAME"));   // 提取表名
                tm.setRemarks(rs.getString("REMARKS"));        // 提取表注释（若数据库支持）
                tables.add(tm);
            }
        }
        return tables;
    }

    /**
     * 读取指定表的列元数据
     * 步骤：
     * 1) 通过 getPrimaryKeys 收集该表所有主键列名，存入 Set 便于快速判断
     * 2) 通过 getColumns 遍历所有列，逐列构建 ColumnMeta
     * 3) 列名转 Java 字段名（下划线转驼峰）、JDBC 类型转 Java 类型、标记主键/自增/可空性
     */
    public List<ColumnMeta> listColumns(Connection conn, String schema, String tableName) throws Exception {
        List<ColumnMeta> columns = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();

        // 收集主键列名：用于后续设置 ColumnMeta.primaryKey
        Set<String> pkColumns = new HashSet<>();
        try (ResultSet pkRs = meta.getPrimaryKeys(schema, null, tableName)) {
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        try (ResultSet rs = meta.getColumns(schema, null, tableName, "%")) {
            while (rs.next()) {
                ColumnMeta cm = new ColumnMeta();
                cm.setColumnName(rs.getString("COLUMN_NAME"));                                          // 原始列名
                cm.setJavaField(NamingUtils.toCamelCase(cm.getColumnName()));                           // 转小驼峰字段名
                cm.setJdbcType(rs.getInt("DATA_TYPE"));                                                 // JDBC 类型常量
                cm.setJdbcTypeName(rs.getString("TYPE_NAME"));                                          // 数据库类型名
                cm.setJavaShortType(JdbcTypeMapper.toShortType(cm.getJdbcType()));                      // 映射为 Java 短类型
                cm.setJavaFullType(JdbcTypeMapper.toFullType(cm.getJdbcType()));                        // 映射为 Java 全限定类型
                cm.setSize(rs.getInt("COLUMN_SIZE"));                                                   // 列长度
                cm.setRemarks(rs.getString("REMARKS"));                                                 // 列注释
                cm.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);              // 判断是否可空
                cm.setAutoIncrement("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));          // 判断是否自增
                cm.setPrimaryKey(pkColumns.contains(cm.getColumnName()));                               // 判断是否为主键
                columns.add(cm);
            }
        }
        return columns;
    }
}
