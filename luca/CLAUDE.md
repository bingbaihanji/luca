# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
mvn clean install        # Full build (all modules)
mvn clean compile        # Compile only
mvn package              # Package — core module produces a fat JAR via Maven Shade
mvn test                 # Run tests (none implemented yet)
```

**Requirements:** Java 25+ with preview features. The build enables `--enable-preview` automatically via the Maven
Compiler Plugin configuration.

## Running the Application

The fat JAR is built to `core/target/` by the Maven Shade Plugin. Main class: `com.bingbaihanji.MainKt` (delegates to
`Start.java` which enables JavaFX preview then launches `LucaApp`).

## Architecture

### Module Structure

```
luca (parent pom)
├── api/      — Extension point interfaces (the plugin contract)
└── core/     — Main application: UI framework, startup, components
```

Plugins are external JARs loaded at runtime from `D:\bingbaihanji\overarching\luca\plugins`.

### Startup Flow

`Start.java` → `LucaApp.java` → `ParentsUI.java`

`LucaApp` does three things in order:

1. Initializes `DefaultPluginManager` (Sunsen framework) and loads/starts plugins from the `plugins/` directory.
2. Builds `ParentsUI` — the master `BorderPane` layout with all UI regions.
3. Queries each extension point and injects the returned nodes into the appropriate UI region.

### Plugin Extension Points (defined in `api/`)

| Interface                  | Injects into                         |
|----------------------------|--------------------------------------|
| `MenuApi`                  | HeaderBar menu bar                   |
| `LeftPanelApi`             | Left `IdeToolWindow`                 |
| `RightPanelApi`            | Right `IdeToolWindow`                |
| `BottomPanelApi`           | `IdeBottomPanel` tab                 |
| `ActivityBarItemApi`       | `IdeActivityBar` (left/right/bottom) |
| `StatusBarContributionApi` | `IdeStatusBar` (left/right areas)    |

Extension points use the **Sunsen framework** (`com.bingbaihanji:sunsen`) annotation `@ExtensionPoint`. Plugins
implement these interfaces and are discovered via the plugin JAR's metadata.

### UI Component Map

`ParentsUI` composes these JavaFX components in a `BorderPane`:

- **HeaderBar** (Java 25 preview) — native window title bar with logo and menu bar
- **IdeActivityBar** — vertical icon sidebars (left/right)
- **IdeToolWindow** — collapsible side panels; can be dragged out to a floating `Stage`
- **IdeTabPane** — center editor tab area with middle-click and right-click menus
- **IdeBottomPanel** — tabbed bottom area (terminal, output, etc.)
- **IdeStatusBar** — 22px footer with status text and plugin widget slots

CSS theme files live in `core/src/main/resources/css/`: `idea-dark.css` (main Darcula-inspired theme) and
`dark-theme.css` (dialogs/alerts).

### Key Dependencies

- **JavaFX 25.0.2** — UI framework (Windows native bindings included)
- **Sunsen 1.0-SNAPSHOT** — personal plugin/extension-point framework
- **Kotlin 2.3.0** — language support (reserved, minimal usage currently)
- **Lombok 1.18.42** — boilerplate reduction via annotations

## Plugin Development

To create a new plugin:

1. Depend on the `api` module (`com.bingbaihanji:luca-api`).
2. Implement one or more extension point interfaces.
3. Register the implementation in Sunsen's plugin metadata format.
4. Drop the built JAR into `D:\bingbaihanji\overarching\luca\plugins`.

The plugin template directory is at `plugins/com.bingbaihanji.luca.plugin.codeGenerator/`.
