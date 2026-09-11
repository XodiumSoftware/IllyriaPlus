# IllyriaBridge — Claude Code Context

## Project at a Glance

- **Name:** IllyriaBridge
- **Type:** Single-module Minecraft Paper plugin project (server-side only)
- **MC Version:** 26.2 (Paper 26.2)
- **Language:** Kotlin (JVM 25)
- **Build Tool:** Gradle with Kotlin DSL

## APIs & Tools

| Category           | Technology                               | Purpose                       |
|--------------------|------------------------------------------|-------------------------------|
| **Core API**       | [Paper API](https://papermc.io/) 26.2    | Minecraft server plugin API   |
| **Language**       | Kotlin 2.4.0 / Java 25                   | JVM language                  |
| **Build Tool**     | Gradle (Kotlin DSL)                      | Build automation              |
| **Gradle Plugins** | paperweight userdev 2.0.0-beta.21        | Paper development environment |
|                    | shadow 9.5.1                             | Fat JAR creation              |
|                    | run-paper 3.0.2                          | Local test server             |
|                    | resource-factory 1.3.1                   | `paper-plugin.yml` generation |
|                    | dokka 2.2.0                              | Documentation generation      |
|                    | ktlint 14.2.0                            | Kotlin linting                |
|                    | foojay-resolver 1.0.0                    | Auto-download JVM toolchains  |
| **Code Style**     | .editorconfig                            | IDE-agnostic formatting rules |

### Paper API Resources

- **Documentation**: https://docs.papermc.io/paper/dev/
- **JavaDoc**: https://jd.papermc.io/paper/26.2/ (matches project version)

## Quick Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run local test server (auto-downloads Paper 26.2)
./gradlew runServer

# Build only (no shadow)
./gradlew build

# Run Kotlin linting
./gradlew ktlintCheck

# Generate Dokka documentation
./gradlew dokkaGenerateHtml
```

## Project Structure

```
IllyriaBridge/
├── build.gradle.kts                    # Build configuration
├── settings.gradle.kts                 # Project settings
├── gradle.properties                   # Gradle properties
├── src/                                # Source directory
│   ├── IllyriaBridge.kt                # Main plugin class
│   ├── bridges/
│   │   ├── BridgeInterface.kt          # Bridge contract
│   │   ├── FabricRecipeBridge.kt       # Fabric recipe sync handler
│   │   └── XaeroMapBridge.kt           # Xaero map sync handler
│   └── payloads/                       # Payload classes
│       └── FabricRecipeSyncPayload.kt  # Fabric recipe sync payload
├── docs/                               # Generated Dokka documentation
└── .github/workflows/                  # CI/CD workflows
```

## Architecture

### Entry Point

**IllyriaBridge** (`JavaPlugin`) — Main class in package `org.xodium.illyriabridge`. On enable:

- Validates the server version against the plugin's supported version
- Instantiates all bridges implementing `BridgeInterface`
- Registers each bridge and logs total registration time
- On disable: unregisters outgoing plugin channels

### Bridge System

**BridgeInterface** — Common contract for all bridges:

- Extends Bukkit `Listener`
- Provides a `register()` method that registers event listeners and returns timing in milliseconds

**FabricRecipeBridge** (`bridges` package):

- Handles `PlayerJoinEvent`
- Detects Fabric clients via `Player.getClientBrandName()`
- Registers outgoing plugin channel `fabric:recipe_sync`
- For Fabric clients: groups recipes by `RecipeSerializer`, encodes via `FabricRecipeSyncPayload.CODEC`, and sends a `ClientboundCustomPayloadPacket`

**XaeroMapBridge** (`bridges` package):

- Registers outgoing plugin channels `xaeroworldmap:main` and `xaerominimap:main`
- Handles `PlayerRegisterChannelEvent` and `PlayerChangedWorldEvent`
- Sends a persistent server world ID to Xaero map clients from `xaeromap.id`

### Package Structure

| Package                          | Contents                                                         |
|----------------------------------|------------------------------------------------------------------|
| `org.xodium.illyriabridge`       | `IllyriaBridge` — Main plugin class                              |
| `org.xodium.illyriabridge.bridges` | `BridgeInterface`, `FabricRecipeBridge`, `XaeroMapBridge`       |
| `org.xodium.illyriabridge.payloads` | `FabricRecipeSyncPayload` — Fabric recipe sync payload         |

### Key Conventions

- All internal classes use appropriate visibility (`public`/`internal`/`private`)
- Plugin uses Paper's plugin channel API and NMS packets for cross-platform mod compatibility
- Recipe data is encoded using Minecraft's `RegistryFriendlyByteBuf`
- Bridges are singleton objects implementing `BridgeInterface`

## CI/CD

GitHub Actions workflows in `.github/workflows/`:

- **kotlin.yml** — Runs `ktlintCheck`, builds shadow JAR, uploads artifacts, creates nightly release, and publishes Dokka docs to GitHub Pages on `main`
- **enforce_pr_title.yml** — Validates PR titles follow conventional commits

## Claude Code Workflow

### Task Management

**When creating tasks:**

- Number tasks in the name (e.g., "1. Add Verdance enchantment", "2. Update mana system")
- This makes it easy to reference specific tasks in conversation

**After completing each task:**

- Ask the user if they want to git commit the changes or adjust before committing

**When all tasks in a worktree are complete:**

- Ask the user if they want to git publish (push) the changes or adjust before publishing

## Memory System

This project uses Claude Code's persistent memory in `.claude/memory/`. These files persist across sessions and different PCs. Review `MEMORY.md` for existing context about the user and project.
