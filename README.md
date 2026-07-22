# Luca

**Luca** 是一个基于 JavaFX 的插件化桌面应用框架，灵感来自 IntelliJ IDEA 的 UI 布局与插件体系。它使用 [Sunsen](https://github.com/bingbaihanji/sunsen) 作为底层插件运行时，通过 ClassLoader 隔离和扩展点机制实现功能的动态插拔。

> 名字取自意大利语"光"——一个轻量的容器，承载各种插件发出的光。

---

## 项目结构

```
luca/                          ← 核心应用（JavaFX IDE 风格框架）
├── api/                       ← 扩展点接口（插件契约）
└── core/                      ← 主程序：UI 组件、启动流程、CSS 主题

Luca-plugin-AudioSpectrum/     ← 音频频谱可视化插件
Luca-plugin-CodeGenerator/     ← 数据库 → SpringBoot/MyBatis Plus 代码生成插件
```

### 依赖关系

```
Luca-plugin-AudioSpectrum ──┐
Luca-plugin-CodeGenerator ──┤
                            ▼
                         luca (luca-api + luca-core)
                            │
                            ▼
                        Sunsen (sunsen-core + sunsen-api + sunsen-server)
```

插件通过 `luca-api` 定义的扩展点接口与宿主交互，运行时由 Sunsen 框架管理生命周期和类加载。

---

## 核心特性

### luca — 可插拔的 IDE 风格框架

- **IntelliJ 风格 UI**：HeaderBar、ActivityBar、ToolWindow、TabPane、BottomPanel、StatusBar，全部纯 JavaFX 实现
- **6 个扩展点**：菜单栏、左右面板、底部面板、活动栏、状态栏，插件可自由注入 UI 内容
- **深色主题**：内置 Darcula 风格 CSS（`idea-dark.css`）
- **窗口组件可拖拽**：ToolWindow 支持拖出为浮动窗口
- **Java 25 Preview**：使用 `--enable-preview` 特性（HeaderBar 原生标题栏）

### Luca-plugin-AudioSpectrum — 音频可视化

- 5 种可视化效果：波形、频谱条形图、圆形频谱、立体声电平表、螺旋粒子
- FFT 频谱分析（Cooley-Tukey 算法）
- 多格式音频解码（WAV / MP3 / FLAC / APE，基于 FFmpeg）
- 实时均衡器
- 高性能：FPS 控制、预计算表、环形缓冲区

### Luca-plugin-CodeGenerator — 代码生成器

- 通过 JDBC 连接 MySQL / PostgreSQL，读取表结构元数据
- 生成 SpringBoot + MyBatis Plus 风格代码：Entity、Mapper、Mapper XML、Service、Controller
- Freemarker 模板引擎渲染，支持自定义模板
- 图形化界面：连接配置 → 表浏览 → 生成选项 → 一键生成

---

## 快速开始

### 环境要求

- **JDK 25+**（需要 `--enable-preview`）
- **Maven 3.8+**

### 构建

```bash
# 1. 先安装 Sunsen 插件框架（其他模块依赖它）
git clone https://github.com/bingbaihanji/sunsen.git
cd sunsen && mvn install -DskipTests && cd ..

# 2. 构建 Luca 核心应用
cd luca && mvn install -DskipTests && cd ..

# 3. 构建插件
cd Luca-plugin-AudioSpectrum && mvn package && cd ..
cd Luca-plugin-CodeGenerator && mvn package && cd ..
```

### 运行

```bash
# 将插件 JAR 放入 plugins 目录
mkdir -p luca/core/plugins
cp Luca-plugin-AudioSpectrum/target/lpas-1.0-SNAPSHOT.jar luca/core/plugins/
cp Luca-plugin-CodeGenerator/target/lpc-1.0-SNAPSHOT.jar luca/core/plugins/

# 运行（需要 JavaFX SDK）
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web,javafx.swing \
     --enable-preview \
     -jar luca/core/target/luca-core-1.0-SNAPSHOT.jar
```

---

## 开发自己的插件

### 1. 依赖 luca-api

```xml
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
```

### 2. 创建 plugin.json

在 `src/main/resources/META-INF/plugin.json` 中声明插件元数据：

```json
{
  "id": "com.example.my-plugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "apiVersion": "1.0",
  "mainClass": "com.example.MyPlugin",
  "packagePrefixes": ["com.example"]
}
```

或者使用 `@Plugin` 注解，编译期自动生成。

### 3. 实现扩展点

```java
@Extension
public class MyMenuExtension implements MenuApi {
    @Override
    public void extend(MenuBar parentMenu) {
        Menu menu = new Menu("我的插件");
        menu.getItems().add(new MenuItem("Hello"));
        parentMenu.getMenus().add(menu);
    }
}
```

### 4. 可用扩展点

| 接口 | 说明 |
|------|------|
| `MenuApi` | 向菜单栏注入菜单项 |
| `LeftPanelApi` | 左侧面板内容 |
| `RightPanelApi` | 右侧面板内容 |
| `BottomPanelApi` | 底部面板标签页 |
| `ActivityBarItemApi` | 活动栏图标项 |
| `StatusBarContributionApi` | 状态栏组件 |

### 5. 打包与部署

使用 `maven-shade-plugin` 打包插件 JAR（排除宿主已提供的依赖），放入 `luca/core/plugins/` 目录即可。

---

## 技术栈

| 组件 | 技术 |
|------|------|
| 插件框架 | [Sunsen](https://github.com/bingbaihanji/sunsen) — 轻量级 Java 插件系统 |
| UI 框架 | JavaFX 25.0.2 |
| JDK | 25（`--enable-preview`） |
| 构建工具 | Maven |
| 模板引擎 | Freemarker（代码生成插件） |
| 音频解码 | JAVE / FFmpeg（音频插件） |
| 代码简化 | Lombok |

---

## 模块说明

### luca/api — 扩展点契约

定义宿主与插件之间的接口契约。插件只依赖此模块，不依赖宿主业务代码。

### luca/core — 宿主应用

IDE 风格的 JavaFX 应用，包含：
- `ParentsUI` — 主布局（BorderPane 组合各区域）
- `IdeToolWindow` — 可折叠、可拖拽的侧边面板
- `IdeTabPane` — 中心编辑区标签页
- `IdeBottomPanel` — 底部标签面板
- `IdeActivityBar` — 垂直图标栏
- `IdeStatusBar` — 状态栏
- `HeaderBar` — 原生标题栏 + 菜单栏

### Luca-plugin-AudioSpectrum

音频频谱可视化插件，提供波形、频谱条形图、圆形频谱、立体声电平表、螺旋粒子等可视化效果，支持 WAV/MP3/FLAC/APE 格式解码。

### Luca-plugin-CodeGenerator

数据库代码生成插件，连接 MySQL/PostgreSQL 后读取表结构，一键生成 SpringBoot + MyBatis Plus 风格的 Entity、Mapper、Service、Controller 代码。

---

## 相关项目

- **[Sunsen](https://github.com/bingbaihanji/sunsen)** — Luca 底层使用的轻量级 Java 插件框架

---

## 许可证

本项目采用 [MIT 许可证](LICENSE) 开源。

---

## 致谢

- [IntelliJ IDEA](https://www.jetbrains.com/idea/) — UI 布局与插件体系的设计灵感
- [Spring Boot](https://spring.io/projects/spring-boot) — 自动装配与生命周期事件的设计参考
- [OSGi](https://www.osgi.org/) / [PF4J](https://github.com/pf4j/pf4j) — ClassLoader 隔离与依赖管理的实现参考
