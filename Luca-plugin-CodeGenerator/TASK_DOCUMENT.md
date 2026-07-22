# luca-plugin-CodeGenerator 开发任务文档

## 项目概述

本文档描述 `Luca-plugin-CodeGenerator` 插件的完整开发流程。该插件运行在 `luca` JavaFX IDE 框架中，通过 `Sunsen`
插件框架接入宿主，实现以下功能：

- 通过 JDBC 连接数据库（MySQL / PostgreSQL）
- 读取表结构元数据
- 生成 SpringBoot + MyBatis Plus 风格的代码（Entity、Mapper、Mapper XML、Service 接口 + 实现类、Controller with CRUD）

**代码生成使用 Freemarker 模板引擎，直接从 JDBC 元数据渲染，不依赖 MybatisX-dev 的 IntelliJ IDEA 相关 API。**  
`MybatisX-dev/` 目录仅供参考，了解代码风格，请勿直接复制其中依赖 IDEA SDK 的类。

---

## 绝对禁止

1. **禁止修改 `Sunsen/` 目录下的任何代码**（sunsen-core、sunsen-api、sunsen-server、sunsen-demo-plain 均不可改）
2. **禁止修改 `MybatisX-dev/` 目录下的任何代码**
3. 插件 JAR 打包时**不得包含** `sunsen-core`、`sunsen-api`、`luca-api`、`javafx.*` 的类（宿主已提供）

---

## 现有代码说明（开始前必读）

### luca 模块

- `../luca/core/src/main/java/com/bingbaihanji/luca/core/ui/lucaApp.java` — JavaFX Application 入口，**目前未集成 Sunsen
  PluginManager**，需要修改
- `../luca/core/src/main/java/com/bingbaihanji/luca/core/ui/ParentsUI.java` — IDEA 风格主 UI，已有
  `setLeftContent/setRightContent/setCenterContent/setBottomContent` 方法；`menuBar` 目前是局部变量，**需要提升为字段并暴露
  getter**
- `../luca/api/src/main/java/com/bingbaihanji/luca/api/menu/MenuApi.java` — 唯一的扩展点，已存在，**不要修改**

### Sunsen 插件框架关键 API（只读）

| 类/接口                   | 模块              | 说明                                                              |
|------------------------|-----------------|-----------------------------------------------------------------|
| `@Plugin`              | `sunsen-api`    | `com.bingbaihanji.sunsen.api.annotation.Plugin`，标注主插件类          |
| `AbstractPlugin`       | `sunsen-api`    | `com.bingbaihanji.sunsen.api.support.AbstractPlugin`，插件基类       |
| `@Extension`           | `sunsen-core`   | `com.bingbaihanji.sunsen.api.annotation.Extension`，标注扩展实现类      |
| `@ExtensionPoint`      | `sunsen-core`   | `com.bingbaihanji.sunsen.api.annotation.ExtensionPoint`，标注扩展点接口 |
| `DefaultPluginManager` | `sunsen-server` | `com.bingbaihanji.sunsen.core.DefaultPluginManager`，插件管理器实现     |

### 当前 luca-plugin-CodeGenerator 状态

- `/src/main/java/com/bingbaihanji/luca/plugin/MenuBarPlugin.java` — 存在，是空壳，**需要重命名和实现
  **
- `/pom.xml` — artifactId `lpc`，已依赖 `luca-api` 和 `sunsen-api`，**需要增加依赖和打包配置**

---

## 模块依赖图

```
lucaApp (luca-core)
  └── depends on sunsen-server (DefaultPluginManager)
  └── depends on luca-api (MenuApi, LeftPanelApi)

luca-plugin-CodeGenerator (插件 JAR)
  └── provided: luca-api, sunsen-api, javafx
  └── bundled: freemarker, JDBC 驱动, lombok (provided)
```

---

## Task 0: 准备工作

在开始任何修改前，先确保所有模块已安装到本地 Maven 仓库：

```bash
# 先安装 Sunsen（禁止修改，只安装）
cd Sunsen && mvn install -DskipTests && cd ..

# 安装 luca（后续会修改 luca-core，每次修改后重新 install）
cd luca && mvn install -DskipTests && cd ..
```

---

## Task 1: 修改 luca/api — 新增 LeftPanelApi 扩展点

**新建文件**: `../luca/api/src/main/java/com/bingbaihanji/luca/api/panel/LeftPanelApi.java`

```java
package com.bingbaihanji.luca.api.panel;

import com.bingbaihanji.sunsen.api.annotation.ExtensionPoint;
import javafx.scene.Node;

@ExtensionPoint(id = "leftPanel:content", description = "左侧面板内容扩展")
public interface LeftPanelApi {
    /**
     * 返回插件提供的左侧面板内容节点。
     * 宿主在所有插件 start 完成后调用此方法注入内容。
     */
    Node getContent();
}
```

完成后重新安装 luca-api：`cd luca && mvn install -DskipTests`

---

## Task 2: 修改 ParentsUI — 暴露 MenuBar

**文件**: `../luca/core/src/main/java/com/bingbaihanji/luca/core/ui/ParentsUI.java`

当前 `menuBar` 是 `createHeader()` 方法内的局部变量，需要：

1. 在类顶部添加实例字段：
   ```java
   private MenuBar menuBar;
   ```
2. 在 `createHeader()` 方法内，将原来的 `MenuBar menuBar = new MenuBar(...)` 改为给实例字段赋值：
   ```java
   this.menuBar = new MenuBar(...);  // 参数不变
   ```
3. 在类中添加 getter 方法：
   ```java
   public MenuBar getMenuBar() {
       return menuBar;
   }
   ```

---

## Task 3: 修改 lucaApp — 集成 Sunsen PluginManager

**文件**: `../luca/core/src/main/java/com/bingbaihanji/luca/core/ui/lucaApp.java`

**luca-core 的 pom.xml 增加依赖**：

```xml
<dependency>
    <groupId>com.bingbaihanji</groupId>
    <artifactId>sunsen-server</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**修改 `start(Stage primaryStage)` 方法**，新逻辑如下（保留原来的 Scene 创建与 show）：

```java
@Override
public void start(Stage primaryStage) throws Exception {
    // 1. 确定插件目录（与运行 JAR 同级的 plugins/ 文件夹）
    Path pluginsDir = Path.of(System.getProperty("user.dir"), "plugins");
    if (!Files.exists(pluginsDir)) {
        Files.createDirectories(pluginsDir);
    }

    // 2. 初始化 PluginManager
    DefaultPluginManager pluginManager = new DefaultPluginManager(pluginsDir);
    pluginManager.loadPlugins();
    pluginManager.startPlugins();

    // 3. 构建主 UI
    ParentsUI root = new ParentsUI();

    // 4. 调用所有 MenuApi 扩展，向菜单栏注入菜单项
    List<?> menuExtensions = pluginManager.getExtensions(MenuApi.class);
    for (Object ext : menuExtensions) {
        ((MenuApi) ext).extend(root.getMenuBar());
    }

    // 5. 调用 LeftPanelApi 扩展（取第一个），注入左侧面板内容
    List<?> leftPanelExtensions = pluginManager.getExtensions(LeftPanelApi.class);
    if (!leftPanelExtensions.isEmpty()) {
        Node leftContent = ((LeftPanelApi) leftPanelExtensions.get(0)).getContent();
        root.setLeftContent(leftContent);
    }

    // 6. 展示主窗口
    Scene scene = new Scene(root, 1280, 800);
    primaryStage.setTitle("luca IDE");
    primaryStage.setScene(scene);
    primaryStage.show();

    // 7. 应用退出时停止插件
    primaryStage.setOnCloseRequest(e -> {
        pluginManager.stopPlugins();
        pluginManager.unloadPlugins();
    });
}
```

需要的 import：

- `com.bingbaihanji.luca.api.menu.MenuApi`
- `com.bingbaihanji.luca.api.panel.LeftPanelApi`
- `com.bingbaihanji.sunsen.core.DefaultPluginManager`
- `java.nio.file.Files`, `java.nio.file.Path`
- `javafx.scene.Node`
- `java.util.List`

完成后重新安装：`cd luca && mvn install -DskipTests`

---

## Task 4: 完善 luca-plugin-CodeGenerator 的 pom.xml

**文件**: `/pom.xml`

在现有依赖基础上增加以下内容：

```xml
<dependencies>
    <!-- 已有 -->
    <dependency>
        <groupId>com.bingbaihanji</groupId>
        <artifactId>luca-api</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.bingbaihanji</groupId>
        <artifactId>sunsen-api</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>

    <!-- JavaFX（宿主已提供，仅编译期） -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.10</version>
        <scope>provided</scope>
    </dependency>

    <!-- Freemarker 模板引擎（打包进插件 JAR） -->
    <dependency>
        <groupId>org.freemarker</groupId>
        <artifactId>freemarker</artifactId>
        <version>2.3.32</version>
    </dependency>

    <!-- MySQL JDBC 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.3.0</version>
    </dependency>

    <!-- PostgreSQL JDBC 驱动 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.3</version>
    </dependency>

    <!-- Lombok（编译期） -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.32</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- 将依赖打包进 JAR，但排除宿主已提供的库 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.2</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals><goal>shade</goal></goals>
                    <configuration>
                        <createDependencyReducedPom>false</createDependencyReducedPom>
                        <filters>
                            <filter>
                                <artifact>*:*</artifact>
                                <excludes>
                                    <exclude>META-INF/*.SF</exclude>
                                    <exclude>META-INF/*.DSA</exclude>
                                    <exclude>META-INF/*.RSA</exclude>
                                </excludes>
                            </filter>
                        </filters>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## Task 5: 创建 plugin.json

**新建文件**: `/src/main/resources/META-INF/plugin.json`

```json
{
  "id": "com.bingbaihanji.luca.plugin.codeGenerator",
  "name": "Code Generator",
  "description": "连接数据库，生成 SpringBoot + MyBatis Plus 风格代码",
  "version": "1.0.0",
  "apiVersion": "1.0",
  "mainClass": "com.bingbaihanji.luca.plugin.code.generator.CodeGeneratorPlugin",
  "packagePrefixes": [
    "com.bingbaihanji.luca.plugin"
  ]
}
```

**注意**：`packagePrefixes` 必须包含插件所有自身类的包前缀，不能包含 `com.bingbaihanji.sunsen.*` 或
`com.bingbaihanji.luca.api.*`。

---

## Task 6: 主插件类 CodeGeneratorPlugin

**删除旧文件** `/src/main/java/com/bingbaihanji/luca/plugin/MenuBarPlugin.java`

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/CodeGeneratorPlugin.java`

```java
package com.bingbaihanji.luca.plugin;

import com.bingbaihanji.sunsen.api.annotation.Plugin;
import com.bingbaihanji.sunsen.api.support.AbstractPlugin;

@Plugin
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
```

---

## Task 7: 工具类 NamingUtils

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/util/NamingUtils.java`

实现以下静态方法：

```java
package com.bingbaihanji.luca.plugin.code.generator.util;

public class NamingUtils {

    /**
     * 下划线转小驼峰: user_name -> userName
     */
    public static String toCamelCase(String name) { ...}

    /**
     * 下划线转大驼峰(PascalCase): user_name -> UserName
     */
    public static String toPascalCase(String name) { ...}

    /**
     * 去除表名前缀: stripTablePrefix("t_user", "t_") -> "user"
     * prefix 为空或 null 时直接返回原名
     */
    public static String stripTablePrefix(String tableName, String prefix) { ...}

    /**
     * 类名转 URL 前缀: UserInfo -> user-info (REST 路径风格，可选用 camelCase)
     * 本项目简单实现为 lowerFirstChar: UserInfo -> userInfo
     */
    public static String toLowerFirst(String name) { ...}
}
```

---

## Task 8: JDBC 类型映射 JdbcTypeMapper

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/db/JdbcTypeMapper.java`

根据 `java.sql.Types` 常量返回 Java 类型短名称和全限定名：

| JDBC Types 常量                              | Java 短名称      | 全限定名                    |
|--------------------------------------------|---------------|-------------------------|
| VARCHAR, CHAR, LONGVARCHAR, CLOB, NVARCHAR | String        | java.lang.String        |
| INTEGER, SMALLINT, TINYINT                 | Integer       | java.lang.Integer       |
| BIGINT                                     | Long          | java.lang.Long          |
| FLOAT, REAL                                | Float         | java.lang.Float         |
| DOUBLE                                     | Double        | java.lang.Double        |
| DECIMAL, NUMERIC                           | BigDecimal    | java.math.BigDecimal    |
| DATE                                       | LocalDate     | java.time.LocalDate     |
| TIME                                       | LocalTime     | java.time.LocalTime     |
| TIMESTAMP                                  | LocalDateTime | java.time.LocalDateTime |
| BOOLEAN, BIT                               | Boolean       | java.lang.Boolean       |
| BLOB, BINARY, VARBINARY, LONGVARBINARY     | byte[]        | byte[]                  |
| 其他                                         | String        | java.lang.String        |

提供方法：

```java
public static String toShortType(int jdbcType)
public static String toFullType(int jdbcType)
// 是否需要 import（java.lang.* 不需要）
public static boolean needsImport(int jdbcType)
```

---

## Task 9: 元数据模型类

### 9a: TableMeta.java

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/db/TableMeta.java`

```java
package com.bingbaihanji.luca.plugin.code.generator.db;

import lombok.Data;

@Data
public class TableMeta {
    private String tableName;   // 数据库表名，如 t_user
    private String remarks;     // 表注释
}
```

### 9b: ColumnMeta.java

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/db/ColumnMeta.java`

```java
package com.bingbaihanji.luca.plugin.code.generator.db;

import lombok.Data;

@Data
public class ColumnMeta {
    private String columnName;      // 数据库列名，如 user_name
    private String javaField;       // Java 字段名，如 userName
    private int jdbcType;           // java.sql.Types 常量
    private String jdbcTypeName;    // JDBC 类型名，如 VARCHAR
    private String javaShortType;   // Java 类型短名，如 String
    private String javaFullType;    // Java 全类名，如 java.time.LocalDateTime
    private int size;               // 列长度
    private String remarks;         // 列注释
    private boolean nullable;
    private boolean autoIncrement;
    private boolean primaryKey;
}
```

---

## Task 10: 数据库元数据读取服务

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/db/DbMetadataService.java`

```java
package com.bingbaihanji.luca.plugin.code.generator.db;

import java.sql.*;
import java.util.*;

public class DbMetadataService {

    /**
     * 建立数据库连接
     */
    public Connection connect(DbConfig config) throws Exception {
        Class.forName(config.getDriverClass());
        return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
    }

    /**
     * 列出指定 schema 下的所有表
     * @param schema 数据库名/schema 名，MySQL 传数据库名，PostgreSQL 传 schema（通常 "public"）
     */
    public List<TableMeta> listTables(Connection conn, String schema) throws Exception {
        List<TableMeta> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        // MySQL: catalog=schema, schemaPattern=null; PostgreSQL: catalog=null, schemaPattern=schema
        try (ResultSet rs = meta.getTables(schema, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                TableMeta tm = new TableMeta();
                tm.setTableName(rs.getString("TABLE_NAME"));
                tm.setRemarks(rs.getString("REMARKS"));
                tables.add(tm);
            }
        }
        return tables;
    }

    /**
     * 读取指定表的列元数据
     */
    public List<ColumnMeta> listColumns(Connection conn, String schema, String tableName) throws Exception {
        List<ColumnMeta> columns = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();

        // 收集主键列名
        Set<String> pkColumns = new HashSet<>();
        try (ResultSet pkRs = meta.getPrimaryKeys(schema, null, tableName)) {
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        try (ResultSet rs = meta.getColumns(schema, null, tableName, "%")) {
            while (rs.next()) {
                ColumnMeta cm = new ColumnMeta();
                cm.setColumnName(rs.getString("COLUMN_NAME"));
                cm.setJavaField(NamingUtils.toCamelCase(cm.getColumnName()));
                cm.setJdbcType(rs.getInt("DATA_TYPE"));
                cm.setJdbcTypeName(rs.getString("TYPE_NAME"));
                cm.setJavaShortType(JdbcTypeMapper.toShortType(cm.getJdbcType()));
                cm.setJavaFullType(JdbcTypeMapper.toFullType(cm.getJdbcType()));
                cm.setSize(rs.getInt("COLUMN_SIZE"));
                cm.setRemarks(rs.getString("REMARKS"));
                cm.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                cm.setAutoIncrement("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));
                cm.setPrimaryKey(pkColumns.contains(cm.getColumnName()));
                columns.add(cm);
            }
        }
        return columns;
    }
}
```

---

## Task 11: 数据库连接配置模型

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/db/DbConfig.java`

```java
package com.bingbaihanji.luca.plugin.code.generator.db;

import lombok.Data;

@Data
public class DbConfig {
    /** JDBC 驱动类名，如 com.mysql.cj.jdbc.Driver */
    private String driverClass;
    /** JDBC URL，如 jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai */
    private String url;
    private String username;
    private String password;
    /** schema/database 名称，用于 DatabaseMetaData 查询 */
    private String schemaName;

    /** 内置预设 */
    public static DbConfig mysqlPreset(String host, int port, String db, String user, String pass) {
        DbConfig c = new DbConfig();
        c.driverClass = "com.mysql.cj.jdbc.Driver";
        c.url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8";
        c.username = user;
        c.password = pass;
        c.schemaName = db;
        return c;
    }

    public static DbConfig postgresPreset(String host, int port, String db, String user, String pass) {
        DbConfig c = new DbConfig();
        c.driverClass = "org.postgresql.Driver";
        c.url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        c.username = user;
        c.password = pass;
        c.schemaName = "public";
        return c;
    }
}
```

---

## Task 12: 代码生成配置模型

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/generator/GeneratorConfig.java`

```java
package com.bingbaihanji.luca.plugin.generator;

import lombok.Data;

@Data
public class GeneratorConfig {
    /** Java 源文件输出根目录，如 /home/dev/myproject/src/main/java */
    private String outputPath;
    /** resources 目录，Mapper XML 写入 resources/mapper/，如 /home/dev/myproject/src/main/resources */
    private String resourcesPath;
    /** 基础包名，如 com.example.demo */
    private String basePackage;
    /** 要去掉的表名前缀，如 t_（为空则不去除） */
    private String tablePrefix = "";
    /** 作者名 */
    private String author = System.getProperty("user.name", "author");

    // 子包名（相对于 basePackage）
    private String entitySubPackage = "entity";
    private String mapperSubPackage = "mapper";
    private String serviceSubPackage = "service";
    private String serviceImplSubPackage = "service.impl";
    private String controllerSubPackage = "controller";

    // 生成选项
    private boolean generateEntity = true;
    private boolean generateMapper = true;
    private boolean generateMapperXml = true;
    private boolean generateService = true;
    private boolean generateServiceImpl = true;
    private boolean generateController = true;
    private boolean useLombok = true;
}
```

---

## Task 13: Freemarker 模板文件

**创建目录**: `/src/main/resources/templates/`

在该目录下创建以下 6 个 `.ftl` 模板文件：

### 13a: `templates/entity.ftl`

```freemarker
package ${packageName};

<#list imports as imp>
import ${imp};
</#list>
<#if useLombok>
import lombok.Data;
</#if>
import com.baomidou.mybatisplus.annotation.TableName;
<#if hasPk>
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
</#if>
import com.baomidou.mybatisplus.annotation.TableField;

/**
 * ${tableRemarks!tableName}
 *
 * @author ${author}
 * @date ${date}
 */
<#if useLombok>
@Data
</#if>
@TableName("${tableName}")
public class ${className} {
<#list columns as col>

    <#if col.remarks?has_content>
    /** ${col.remarks} */
    </#if>
    <#if col.primaryKey>
    @TableId(value = "${col.columnName}", type = IdType.AUTO)
    <#else>
    @TableField("${col.columnName}")
    </#if>
    private ${col.javaShortType} ${col.javaField};
</#list>
<#if !useLombok>

    // 无 Lombok 时需手动生成 getter/setter（此处省略，请使用 IDE 自动生成）
</#if>
}
```

### 13b: `templates/mapper.ftl`

```freemarker
package ${packageName};

import ${entityFullClass};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ${tableRemarks!tableName} 的数据库操作 Mapper
 *
 * @author ${author}
 * @date ${date}
 * @see ${entityClass}
 */
@Mapper
public interface ${className} extends BaseMapper<${entityClass}> {
}
```

### 13c: `templates/mapperXml.ftl`

```freemarker
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${mapperFullClass}">

    <!-- ${tableRemarks!tableName} 自定义 SQL 在此扩展 -->

</mapper>
```

### 13d: `templates/serviceInterface.ftl`

```freemarker
package ${packageName};

import ${entityFullClass};
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * ${tableRemarks!tableName} 的业务逻辑接口
 *
 * @author ${author}
 * @date ${date}
 */
public interface ${className} extends IService<${entityClass}> {
}
```

### 13e: `templates/serviceImpl.ftl`

```freemarker
package ${packageName};

import ${entityFullClass};
import ${mapperFullClass};
import ${serviceInterfaceFullClass};
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * ${tableRemarks!tableName} 的业务逻辑实现
 *
 * @author ${author}
 * @date ${date}
 */
@Service
public class ${className} extends ServiceImpl<${mapperClass}, ${entityClass}>
        implements ${serviceInterfaceClass} {
}
```

### 13f: `templates/controller.ftl`

```freemarker
package ${packageName};

import ${entityFullClass};
import ${serviceInterfaceFullClass};
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ${tableRemarks!tableName} 控制器
 *
 * @author ${author}
 * @date ${date}
 */
@RestController
@RequestMapping("/${urlPrefix}")
public class ${className} {

    @Autowired
    private ${serviceInterfaceClass} ${serviceField};

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public Page<${entityClass}> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ${serviceField}.page(new Page<>(pageNum, pageSize));
    }

    /**
     * 根据 ID 查询详情
     */
    @GetMapping("/{id}")
    public ${entityClass} getById(@PathVariable ${pkType} id) {
        return ${serviceField}.getById(id);
    }

    /**
     * 新增
     */
    @PostMapping
    public boolean save(@RequestBody ${entityClass} entity) {
        return ${serviceField}.save(entity);
    }

    /**
     * 修改
     */
    @PutMapping
    public boolean update(@RequestBody ${entityClass} entity) {
        return ${serviceField}.updateById(entity);
    }

    /**
     * 根据 ID 删除
     */
    @DeleteMapping("/{id}")
    public boolean remove(@PathVariable ${pkType} id) {
        return ${serviceField}.removeById(id);
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    public boolean removeBatch(@RequestBody List<${pkType}> ids) {
        return ${serviceField}.removeByIds(ids);
    }
}
```

---

## Task 14: Freemarker 渲染引擎

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/generator/CodeGeneratorEngine.java`

### 职责

1. 初始化 Freemarker `Configuration`，从 classpath `templates/` 加载模板
2. 提供 `generateAll(TableMeta table, List<ColumnMeta> columns, GeneratorConfig config)` 方法
3. 对每个启用的生成类型，构建模板数据 Map，渲染模板，写入目标文件

### 核心数据构建逻辑

```java
// 基础类名推导（去前缀 -> Pascal Case）
String baseName = NamingUtils.toPascalCase(
    NamingUtils.stripTablePrefix(table.getTableName(), config.getTablePrefix())
);

// Entity
String entityClass     = baseName;                      // e.g. UserInfo
String entityPkg       = config.getBasePackage() + "." + config.getEntitySubPackage();
String entityFullClass = entityPkg + "." + entityClass; // e.g. com.example.entity.UserInfo

// Mapper
String mapperClass     = baseName + "Mapper";
String mapperPkg       = config.getBasePackage() + "." + config.getMapperSubPackage();
String mapperFullClass = mapperPkg + "." + mapperClass;

// Service Interface
String serviceClass     = baseName + "Service";
String servicePkg       = config.getBasePackage() + "." + config.getServiceSubPackage();
String serviceFullClass = servicePkg + "." + serviceClass;

// ServiceImpl
String serviceImplClass = baseName + "ServiceImpl";
String serviceImplPkg   = config.getBasePackage() + "." + config.getServiceImplSubPackage();

// Controller
String controllerClass = baseName + "Controller";
String controllerPkg   = config.getBasePackage() + "." + config.getControllerSubPackage();

// 主键信息（取第一个 primaryKey 列，若无则取第一列）
ColumnMeta pk = columns.stream().filter(ColumnMeta::isPrimaryKey).findFirst()
                        .orElse(columns.isEmpty() ? null : columns.get(0));
String pkType = (pk != null) ? pk.getJavaShortType() : "Long";

// imports 集合（仅收集非 java.lang.* 类型）
Set<String> imports = new LinkedHashSet<>();
for (ColumnMeta col : columns) {
    if (JdbcTypeMapper.needsImport(col.getJdbcType())) {
        imports.add(col.getJavaFullType());
    }
}
```

### 文件写出路径规则

```
outputPath/       (= config.outputPath, 如 /project/src/main/java)
├── {entityPkg 转路径}/UserInfo.java
├── {mapperPkg 转路径}/UserInfoMapper.java
├── {servicePkg 转路径}/UserInfoService.java
├── {serviceImplPkg 转路径}/UserInfoServiceImpl.java
└── {controllerPkg 转路径}/UserInfoController.java

resourcesPath/    (= config.resourcesPath, 如 /project/src/main/resources)
└── mapper/UserInfoMapper.xml
```

包名转路径：`entityPkg.replace('.', File.separatorChar)` → `com/example/entity`

写文件前使用 `Files.createDirectories(dir)` 确保目录存在。

---

## Task 15: MenuApi 扩展实现

**新建文件**:
`/src/main/java/com/bingbaihanji/luca/plugin/menu/CodeGeneratorMenuExtension.java`

```java
package com.bingbaihanji.luca.plugin.code.generator.menu;

import com.bingbaihanji.luca.api.menu.MenuApi;
import com.bingbaihanji.luca.plugin.code.generator.ui.DbConnectionDialog;
import com.bingbaihanji.sunsen.api.annotation.Extension;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

@Extension
public class CodeGeneratorMenuExtension implements MenuApi {

    @Override
    public void extend(MenuBar parentMenu) {
        // 注意：此方法在 JavaFX Application Thread 上调用（由 lucaApp 在 start() 中调用）
        for (Menu menu : parentMenu.getMenus()) {
            if (menu.getText().startsWith("工具")) {
                MenuItem item = new MenuItem("代码生成器(G)");
                item.setOnAction(e -> new DbConnectionDialog().show());
                menu.getItems().add(item);
                return;
            }
        }
    }
}
```

---

## Task 16: JavaFX UI — 数据库连接对话框

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/ui/DbConnectionDialog.java`

### 布局

```
┌─────────────────────────────────────────────┐
│  代码生成器 — 数据库连接                      │
├─────────────────────────────────────────────┤
│  数据库类型: [MySQL ▼]                       │
│  Host:       [localhost____]  Port: [3306_] │
│  数据库名:   [______________]                │
│  用户名:     [______________]                │
│  密码:       [••••••••••••••]                │
│  JDBC URL:   [_________________________]    │
│  Driver:     [com.mysql.cj.jdbc.Driver___]  │
├─────────────────────────────────────────────┤
│          [测试连接]    [连接并浏览表]          │
└─────────────────────────────────────────────┘
```

### 要点

- 使用 JavaFX `Stage`（`Modality.APPLICATION_MODAL`）
- 数据库类型下拉框选 `MySQL` 时自动填充 Driver 和 URL 模板；选 `PostgreSQL` 时填充对应值；选 `自定义` 时 URL 和 Driver
  手动填写
- "测试连接"：在后台线程（`Task<Connection>`）执行 `DbMetadataService.connect(config)`，成功后 `Platform.runLater`
  显示成功提示；失败显示错误信息（`Alert.AlertType.ERROR`）
- "连接并浏览表"：连接成功后打开 `TableBrowserDialog`，将 `Connection` 和 `DbConfig` 传入
- 深色主题样式参考 `ParentsUI`：背景 `#2b2b2b`，控件背景 `#3c3f41`，文字 `#bbbbbb`，边框 `#4b4b4b`

---

## Task 17: JavaFX UI — 表浏览器对话框

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/ui/TableBrowserDialog.java`

### 布局

```
┌─────────────────────────────────────────────────────────────┐
│  代码生成器 — 选择表                                          │
├───────────────┬─────────────────────────────────────────────┤
│ 搜索: [____]  │  列信息                                      │
│               │  ┌──────────┬────────┬────────┬────────┐   │
│ ☑ t_user      │  │ 列名     │ Java字段│ Java类型│ 备注  │   │
│ ☐ t_order     │  ├──────────┼────────┼────────┼────────┤   │
│ ☑ t_product   │  │ id       │ id     │ Long   │ 主键ID│   │
│               │  │ user_name│userName│ String │ 用户名 │   │
│               │  └──────────┴────────┴────────┴────────┘   │
├───────────────┴─────────────────────────────────────────────┤
│                              [配置并生成代码]                  │
└─────────────────────────────────────────────────────────────┘
```

### 要点

- 左侧：`ListView<TableMeta>` 配合 `CheckBoxListCell` 或自定义 Cell，支持多选
- 表列表数据在后台线程加载（`Task<List<TableMeta>>`），加载时显示 `ProgressIndicator`
- 右侧：选中左侧某表时，在后台线程加载列信息并更新 `TableView<ColumnMeta>`
    - 列：列名、Java字段名、Java类型、JDBC类型、长度、是否主键、是否可空、备注
- 底部"配置并生成代码"按钮：收集勾选的表列表，打开 `GeneratorConfigDialog`，传入 `Connection`、`DbConfig`、选中的
  `List<TableMeta>`
- 关闭对话框时**关闭 Connection**

---

## Task 18: JavaFX UI — 代码生成配置对话框

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/ui/GeneratorConfigDialog.java`

### 布局

```
┌────────────────────────────────────────────────────┐
│  代码生成器 — 生成配置                                │
├────────────────────────────────────────────────────┤
│  输出路径 (src/main/java): [___________] [浏览]     │
│  Resources 路径:           [___________] [浏览]     │
│  基础包名:                 [com.example.demo______] │
│  表前缀 (去除):            [t__]                    │
│  作者:                     [user_________________]  │
├────────────────────────────────────────────────────┤
│  生成选项:                                           │
│   ☑ Entity    ☑ Mapper    ☑ MapperXML              │
│   ☑ Service   ☑ ServiceImpl  ☑ Controller          │
│   ☑ Lombok                                          │
├────────────────────────────────────────────────────┤
│  将为以下表生成代码: t_user, t_order (共 2 张)       │
├────────────────────────────────────────────────────┤
│               [取消]          [生成代码]             │
└────────────────────────────────────────────────────┘
```

### 要点

- "浏览"按钮使用 `DirectoryChooser` 选择目录
- "生成代码"按钮点击后：
    1. 验证必填字段（outputPath、basePackage 不能为空）
    2. 在后台线程中，对每张选中的表：
       a. 调用 `DbMetadataService.listColumns(conn, schema, tableName)` 获取列信息
       b. 调用 `CodeGeneratorEngine.generateAll(tableMeta, columns, config)` 生成文件
    3. `Platform.runLater` 更新进度；全部完成后弹出成功对话框，列出所有生成的文件路径
    4. 失败时弹出错误对话框显示异常信息

---

## Task 19: LeftPanelApi 扩展（可选，左侧面板数据库浏览器）

如需在 luca 主界面左侧常驻数据库浏览器，可实现此扩展。

**新建文件**: `/src/main/java/com/bingbaihanji/luca/plugin/panel/DbBrowserPanelExtension.java`

```java
package com.bingbaihanji.luca.plugin.code.generator.panel;

import com.bingbaihanji.luca.api.panel.LeftPanelApi;
import com.bingbaihanji.sunsen.api.annotation.Extension;
import javafx.scene.Node;

@Extension
public class DbBrowserPanelExtension implements LeftPanelApi {
    @Override
    public Node getContent() {
        return new DbBrowserPanel();  // 自定义 JavaFX 控件
    }
}
```

`DbBrowserPanel` 是一个 `VBox`，包含：

- 顶部：连接按钮（点击弹出 `DbConnectionDialog`）
- 主体：`TreeView` 展示已连接的数据库及其表，支持双击表弹出生成配置对话框

---

## 最终包结构

```
luca-plugin-CodeGenerator/
├── pom.xml
└── src/main/
    ├── java/com/bingbaihanji/luca/plugin/
    │   ├── CodeGeneratorPlugin.java              # 主插件类
    │   ├── db/
    │   │   ├── DbConfig.java
    │   │   ├── DbMetadataService.java
    │   │   ├── TableMeta.java
    │   │   ├── ColumnMeta.java
    │   │   └── JdbcTypeMapper.java
    │   ├── generator/
    │   │   ├── GeneratorConfig.java
    │   │   └── CodeGeneratorEngine.java
    │   ├── menu/
    │   │   └── CodeGeneratorMenuExtension.java   # @Extension implements MenuApi
    │   ├── panel/
    │   │   └── DbBrowserPanelExtension.java      # @Extension implements LeftPanelApi（可选）
    │   ├── ui/
    │   │   ├── DbConnectionDialog.java
    │   │   ├── TableBrowserDialog.java
    │   │   └── GeneratorConfigDialog.java
    │   └── util/
    │       └── NamingUtils.java
    └── resources/
        ├── META-INF/
        │   └── plugin.json
        └── templates/
            ├── entity.ftl
            ├── mapper.ftl
            ├── mapperXml.ftl
            ├── serviceInterface.ftl
            ├── serviceImpl.ftl
            └── controller.ftl
```

**同时需要修改的 luca 模块文件**：

- `../luca/api/src/main/java/com/bingbaihanji/luca/api/panel/LeftPanelApi.java`（新建）
- `../luca/core/src/main/java/com/bingbaihanji/luca/core/ui/ParentsUI.java`（暴露 menuBar）
- `../luca/core/src/main/java/com/bingbaihanji/luca/core/ui/lucaApp.java`（集成 PluginManager）
- `../luca/core/pom.xml`（增加 sunsen-server 依赖）

---

## 关键约束总结（Codex 必须遵守）

### Sunsen 框架约束

1. **生命周期顺序**：`onInitialized()`（`onInit` 回调）阶段**禁止**调用 `getExtensions()`；只能在 `onStart()` 中访问其他插件的扩展
2. **@Extension 自动注册**：框架在 LOADED 阶段自动扫描并实例化 `@Extension` 类，**无需**在插件代码中手动注册
3. **plugin.json 必须正确**：`packagePrefixes` 必须覆盖插件所有自身类，`apiVersion` 必须为 `"1.0"`
4. **ClassLoader 隔离**：插件 JAR 不得包含 `com.bingbaihanji.sunsen.*`、`com.bingbaihanji.luca.api.*`、`javafx.*` 的类

### JavaFX 线程约束

5. 所有 UI 组件创建和修改**必须在 JavaFX Application Thread** 上执行
6. 所有 JDBC 操作（连接、查询元数据）**必须在后台线程**执行，使用 `javafx.concurrent.Task<T>` 包装
7. 从后台线程更新 UI 使用 `Platform.runLater(Runnable)`

### 代码生成约束

8. Freemarker 模板从 classpath `templates/` 目录加载：
   `cfg.setClassForTemplateLoading(CodeGeneratorEngine.class, "/templates")`
9. 写文件前必须用 `Files.createDirectories()` 确保目标目录存在
10. 生成的文件如已存在，**默认覆盖**（可在配置中加选项）

---

## 验证步骤

构建完成后按以下步骤验证：

```bash
# 1. 构建插件 JAR（含依赖）
cd luca-plugin-CodeGenerator && mvn package

# 2. 将插件 JAR 放到 plugins 目录
mkdir -p luca/core/plugins
cp target/lpc-1.0-SNAPSHOT.jar luca/core/plugins/

# 3. 重新构建 luca-core
cd luca && mvn package

# 4. 运行 lucaApp
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls \
     -cp luca/core/target/luca-core-*.jar com.bingbaihanji.luca.core.ui.lucaApp
```

**预期结果**：

1. 主界面启动，菜单栏"工具(T)"下出现"代码生成器(G)"菜单项
2. 点击菜单项，弹出数据库连接对话框（深色主题）
3. 填写 MySQL 连接信息，"测试连接"成功
4. "连接并浏览表"打开表浏览器，左侧显示表列表，右侧显示列信息
5. 勾选表，点击"配置并生成代码"，填写输出路径和包名
6. 点击"生成代码"，文件写入目标目录，弹出成功提示列出文件路径
7. 检查目标目录下生成的 `.java` 和 `.xml` 文件内容符合预期
