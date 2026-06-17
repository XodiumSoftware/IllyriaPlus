# ARCHITECTURE.md

This file provides guidance when working with code in this repository.

## Project Overview

IllyriaPlus is a single-module Paper Minecraft plugin project (1.21.11) that enhances base gameplay with custom enchantments, recipes, and mechanics.

Built with Kotlin + Gradle, targeting Java 25. Uses the Paper API's modern lifecycle/registry APIs extensively.

## Project Structure

```
IllyriaPlus/
├── settings.gradle.kts      # Project settings
├── build.gradle.kts         # Build configuration
├── src/                     # Source directory
│   ├── IllyriaPlus.kt       # Main plugin class
│   ├── IllyriaPlusBootstrap.kt  # Bootstrap class
│   ├── mechanics/           # Feature mechanics (organized in subfolders: entity, player, server, world)
│   ├── enchantments/        # Enchantment implementations
│   ├── interfaces/          # ModuleInterface, EnchantmentInterface, RecipeInterface
│   ├── managers/            # (empty)
│   ├── pdcs/                # PlayerPDC
│   ├── recipes/             # Recipe implementations
│   ├── data/                # Data classes
│   └── utils/               # Utility functions
└── docs/                    # Generated documentation
```

## Build & Run Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run test server
./gradlew runServer

# Build only (no shadow)
./gradlew build

# Generate documentation
./gradlew dokkaGenerateHtml
```

Output JAR:

- `build/libs/IllyriaPlus-*.jar`

There are no automated tests in this project.

## Architecture

### Entry Points

- **`IllyriaPlusBootstrap`** — `PluginBootstrap` implementation. Runs before plugin enable. Creates item tags (`vanillaplus:tools`, `vanillaplus:weapons`, `vanillaplus:tether_items`), registers five custom enchantments into Paper's registry via `RegistryEvents.ENCHANTMENT`, then tags all five as tradeable, non-treasure, and in-enchanting-table via `LifecycleEvents.TAGS.postFlatten`.
- **`IllyriaPlus`** — `JavaPlugin` main class. On enable: validates server version, registers all recipes, registers all mechanics. All mechanics are active by default (`enabled` defaults to `true` on `ModuleInterface`); override `enabled` to `false` in a specific mechanic to disable it at compile time.

### Module System

Every feature is an `object` implementing **`ModuleInterface`** (which extends `Listener`). Modules self-register as Bukkit event listeners and register their commands/permissions via `register()`.

Each module exposes a nested `object Config` with hardcoded default values. There is no file-based configuration — all settings are compile-time constants.

All mechanics are instantiated as `object` singletons and listed explicitly in `IllyriaPlus.onEnable()`.

### Enchantments

Custom enchantments implement **`EnchantmentInterface`** and are registered in `IllyriaPlusBootstrap` via `RegistryEvents.ENCHANTMENT`. The interface provides:

- **`key`** — a `TypedKey<Enchantment>` derived automatically from the class name (e.g. `VinemineEnchantment` → `vanillaplus:vinemine`).
- **`invoke(builder)`** — override to configure the enchantment's registry entry (description, anvil cost, level range, weight, slot group, etc.). The default implementation is a no-op pass-through.
- **`get()`** — looks up and returns the live `Enchantment` instance from the registry after bootstrap.

Event handling is done via ordinary `@EventHandler` methods in each enchantment object — there is no generic event type on the interface. Five enchantments are actively registered and tagged as tradeable, non-treasure, and in-enchanting-table:

| Enchantment | Slot Group | Supported Items                               |
|-------------|------------|-----------------------------------------------|
| Vinemine    | `MAINHAND` | Pickaxes                                      |
| Tether      | `MAINHAND` | Tools + Weapons (`vanillaplus:tether_items`)  |
| Nimbus      | `SADDLE`   | Harnesses (Happy Ghast saddle slot)           |
| Embertread  | `FEET`     | Foot armor                                    |
| Silk Touch  | `MAINHAND` | Pickaxes                                      |
| Feather Falling | `FEET` | Foot armor                                  |

SilkTouch and FeatherFalling exist as implementations but are not currently registered in the bootstrap.

### PDCs (Persistent Data Containers)

PDC helpers in `pdcs/` expose Kotlin property delegates on entity types. `PlayerPDC` provides `nickname` and `scoreboardVisibility`.

### Recipes

Recipe objects implement **`RecipeInterface`** and are listed in `IllyriaPlus.onEnable()`. They expose a `recipes` list and a `register()` function that returns elapsed time in ms.

### Package Structure (`org.xodium.illyriaplus` in IllyriaCore)

| Package         | Contents                                                                 |
|-----------------|--------------------------------------------------------------------------|
| `mechanics/`    | 27 feature mechanic singletons (organized by category: entity, player, server, world) |
| `data/`         | `AdjacentBlockData`, `BookData`, `BuildSetupData`, `CommandData`                          |
| `enchantments/` | Vinemine, Tether, Nimbus, Embertread, SilkTouch, FeatherFalling |
| `interfaces/`   | `ModuleInterface`, `EnchantmentInterface`, `RecipeInterface`, `ItemInterface` |
| `managers/`     | (empty)                                                                  |
| `pdcs/`         | `PlayerPDC`                                                              |
| `recipes/`      | Chainmail, DiamondRecycle, Painting, RottenFlesh, WoodLog                |
| `utils/`        | `Utils`, `CommandUtils`, `BlockUtils`, `PlayerUtils`, `ScheduleUtils`    |

### Key Conventions

- All internal classes are `internal` visibility.
- All mechanics are `object` singletons.
- Each module defines a nested `object Config` (with nested `object` blocks for logical groupings) containing hardcoded default values. There is no file-based config system.
- MiniMessage (`Utils.MM`) is used throughout for all text formatting.
- The `@Suppress("UnstableApiUsage")` annotation is needed whenever using Paper's experimental APIs (registry events, dialogs, enchantment builders, menu types, etc.).
- ktlint is enforced (configured in `.idea/ktlint-plugin.xml`); wildcard imports are disabled globally via `.editorconfig`.
- Recipe `NamespacedKey` naming: `{descriptive_name}_{recipe_type}` (e.g. `diamond_recycle_blasting_recipe`, `chainmail_helmet_shaped_recipe`).
