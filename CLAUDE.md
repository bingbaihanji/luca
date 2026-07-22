# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Monorepo Overview

This is a monorepo (`D:\bingbaihanji\overarching`) containing a Java plugin framework and the applications built on top of it. There is no top-level build system — each sub-project has its own `pom.xml` and independent Maven build.

| Directory | Description | JDK | Build |
|-----------|-------------|-----|-------|
| `Sunsen/` | Lightweight Java plugin framework with ClassLoader isolation | 21+ | `mvn install` |
| `luca/` | JavaFX IDE-like desktop app using Sunsen for plugins | 25 | `mvn install` |
| `Luca-plugin-AudioSpectrum/` | Audio spectrum visualization plugin for luca | 25 | `mvn package` |
| `Luca-plugin-CodeGenerator/` | Database → SpringBoot/MyBatis Plus code generator plugin for luca | 25 | `mvn package` |

**Each sub-project has its own `CLAUDE.md`** with project-specific architecture, build commands, and constraints. Read those before working in a specific sub-project.

## Cross-Project Dependency Graph

```
Luca-plugin-AudioSpectrum ──┐
Luca-plugin-CodeGenerator ──┤
                            ▼
                         luca (luca-api + luca-core)
                            │
                            ▼
                        Sunsen (sunsen-core + sunsen-api + sunsen-server)
```

**Build order matters**: Sunsen must be `mvn install`ed first (other projects reference it by Maven GAV coordinates), then luca must be `mvn install`ed before plugins can be built.

```bash
# Correct build order for a full build:
cd Sunsen && mvn install -DskipTests && cd ..
cd luca && mvn install -DskipTests && cd ..
cd Luca-plugin-AudioSpectrum && mvn package && cd ..
cd Luca-plugin-CodeGenerator && mvn package && cd ..
```

## Module Boundary Rules

These constraints span multiple projects and are easy to violate without understanding the full picture:

1. **`sunsen-core` must stay zero-dependency.** All types that cross ClassLoader boundaries (interfaces, annotations, records, events, exceptions) must be defined here. Never add third-party dependencies to it.

2. **Extension point interfaces live in `{app}-api` modules**, not in host business code. Sunsen defines its contract in `sunsen-core`; luca defines its plugin contract in `luca/api/` (e.g., `MenuApi`, `LeftPanelApi`). Plugins depend on these API modules with `provided` scope — they must NOT bundle these classes in their JARs.

3. **Plugin JARs must not contain** `com.bingbaihanji.sunsen.*`, `com.bingbaihanji.luca.api.*`, or `javafx.*` classes — these are provided by the host's parent ClassLoader.

4. **`packagePrefixes` in `plugin.json`** must cover all plugin-owned classes but must NOT include any framework or API packages. Prefix conflicts are detected at load time and will cause the plugin to be rejected.

## Sunsen Framework Invariants

Key constraints that apply when working across the framework and its consumers:

- **Lifecycle ordering**: `@Extension` scanning happens at `LOADED` stage (after `onInit()`, before `onStart()`). Calling `getExtensions()` in `onInit()` is forbidden — other plugins may not be loaded yet.

- **Hot reload atomicity**: `reloadPlugin()` holds the `ExtensionRegistry` write lock for the entire "unregister old → register new" sequence. Never split these into separate lock windows.

- **ClassLoader delegation order** (in `PluginClassLoader`): JDK classes and `com.bingbaihanji.sunsen.api.*` always go to the parent loader first. Plugin own classes (matching `packagePrefixes`) break parent delegation. This is what keeps types consistent across plugins.

- **`apiVersion` must match**: Plugin `plugin.json` `apiVersion` must equal `SunsenVersion.API_VERSION` (currently `"1.0"`). Breaking API changes require bumping the major version.

- **`PluginContext` is the gateway**: All framework capabilities exposed to plugins go through `PluginContext`. Never bypass it by giving plugins direct access to `PluginManager`.

## Technology Stack

- **Java**: 25 with `--enable-preview` for luca and plugins; 21+ for Sunsen
- **JavaFX 25.0.2**: UI framework for luca (Windows native bindings)
- **Maven**: Build system (no Gradle). Plugins use `maven-shade-plugin` for fat JARs
- **Lombok**: Annotation-based boilerplate reduction (used in luca and plugins)
- **Kotlin 2.3.0**: Reserved in luca (minimal usage currently)
- **Jackson 2.17.0**: Only external dependency in Sunsen (`sunsen-server`, for `plugin.json` parsing)
- **Freemarker**: Template engine used by Luca-plugin-CodeGenerator for code generation
- **JUnit 5 + Surefire 3.2.5**: Testing in Sunsen

## Commit Style

Commits use short, imperative Chinese summaries (e.g., `优化文档`, `修复 unloadPlugins() 对 STARTING 状态的处理`).
