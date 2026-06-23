# ARCHITECTURE.md

This file provides guidance when working with code in this repository.

## Project Overview

IllyriaPlus is a single-module Paper Minecraft plugin project (26.1.2) that enhances base gameplay with custom enchantments, recipes, and mechanics.

Built with Kotlin + Gradle, targeting Java 25. Uses the Paper API's modern lifecycle/registry APIs extensively.

## Project Structure

```
IllyriaPlus/
├── settings.gradle.kts       # Project settings
├── build.gradle.kts          # Build configuration
├── src/                      # Source directory
│   ├── IllyriaPlus.kt        # Main plugin class
│   ├── IllyriaPlusBootstrap.kt # Bootstrap class
│   ├── Utils.kt              # Utility functions
│   ├── mechanics/            # Feature mechanics (organized by category)
│   │   ├── entity/           # Entity mechanics
│   │   │   └── monster/    # Monster-specific mechanics
│   │   ├── player/           # Player mechanics
│   │   ├── server/           # Server mechanics
│   │   └── world/            # World mechanics
│   ├── enchantments/         # Enchantment implementations
│   │   ├── utility/          # Custom utility enchantments
│   │   └── vanilla/          # Vanilla enchantment overrides
│   ├── items/                # Reusable item builders
│   │   └── alcoholics/       # Alcoholic drink items
│   ├── paintings/            # Custom painting variant implementations
│   ├── recipes/              # Recipe implementations
│   │   └── vanilla/          # Vanilla-style custom recipes
│   ├── damagetypes/          # Custom damage type implementations
│   ├── data/                 # Data classes
│   └── pdcs/                 # Persistent Data Container helpers
└── docs/                     # Generated documentation
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

# Run linting
./gradlew ktlintCheck

# Fix linting issues
./gradlew ktlintFormat
```

Output JAR:

- `build/libs/IllyriaPlus-*.jar`

There are no automated tests in this project.

## Architecture

### Entry Points

- **`IllyriaPlusBootstrap`** — `PluginBootstrap` implementation. Runs before plugin enable. Creates item tags (`illyriaplus:tools`, `illyriaplus:weapons`, `illyriaplus:tether_items`), registers custom enchantments, damage types, paintings, and banner patterns into Paper's registry via the respective `RegistryEvents`, then tags enchantments as tradeable, non-treasure, and in-enchanting-table via `LifecycleEvents.TAGS.postFlatten`.
- **`IllyriaPlus`** — `JavaPlugin` main class. On enable: validates server version, registers all recipes, registers all mechanics, and registers all enchantment event listeners. All modules are listed explicitly in `IllyriaPlus.onEnable()`.

### Module System

Every feature is an `object` implementing the appropriate interface.

| Type | Interface | Registration |
|---|---|---|
| Mechanics | `MechanicInterface` (extends Bukkit `Listener`) | `PluginManager.registerEvents()` + command/permission registration via `register()` |
| Enchantments | `EnchantmentInterface` (extends Bukkit `Listener`) | `PluginManager.registerEvents()` via `register()` |
| Recipes | `RecipeInterface` | `Server.addRecipe()` / `PotionBrewer.addPotionMix()` via `register()` |
| Paintings | `PaintingInterface` | Registered in `IllyriaPlusBootstrap` via `RegistryEvents.PAINTING_VARIANT` and added to `minecraft:painting_variant/placeable` |
| Damage Types | `DamageTypeInterface` | Registered in `IllyriaPlusBootstrap` via `RegistryEvents.DAMAGE_TYPE` |

All modules are singletons. There is no file-based configuration — values are hardcoded as `private const val` / `private val` properties directly in each module object. To change behavior, edit the source and rebuild.

### Items

Reusable item builders live in `src/items/` and implement **`ItemInterface`**, which exposes an `operator fun invoke(): ItemStack` so each object can be called like a factory function.

Current groups:

- `alcoholics/` — Absinthe, Ale, ChorusWine, Cider, Glowshine, Mead, Moonshine, NetherAle, RedWine, Rum, Stout, Vodka, Whiskey

These items are consumed by `AlcoholRecipe` for brewing results, but can be used anywhere an `ItemStack` is needed.

### Mechanics

Mechanics are grouped by category under `src/mechanics/`:

- `entity/` — Bat, Griefing, Silence, SpawnEgg, Tameable
- `entity/monster/` — AbstractSkeleton, Creeper, Husk, Monster, Zombie (`MonsterInterface` base)
- `player/` — Alcohol, Enderchest, Head, Locator, Messages, Nickname, Sit, Xp
- `server/` — Chat, Motd, Rules, ScoreBoard, ServerInfo, TabList
- `world/` — BlockPlacement, ChiseledBookshelf, Dimension, Inventory, Openable, Tree

Currently **26** mechanics are registered in `IllyriaPlus.onEnable()`.

### Enchantments

Custom enchantments implement **`EnchantmentInterface`** and are registered in `IllyriaPlusBootstrap` via `RegistryEvents.ENCHANTMENT`. The interface provides:

- **`key`** — a `TypedKey<Enchantment>` derived automatically from the class name (e.g. `VinemineEnchantment` → `illyriaplus:vinemine`).
- **`invoke(builder)`** — configures the enchantment's registry entry (description, anvil cost, level range, weight, slot group, etc.). The default implementation is a no-op pass-through.
- **`get()`** — looks up and returns the live `Enchantment` instance from the registry after bootstrap.

Event handling is done via ordinary `@EventHandler` methods in each enchantment object.

**Bootstrap-registered enchantments** (registered in Paper's registry and tagged as tradeable / non-treasure / in-enchanting-table):

| Enchantment | Slot Group | Supported Items |
|-------------|------------|-----------------|
| Vinemine    | `MAINHAND` | Pickaxes (`ItemTypeTagKeys.PICKAXES`) |
| Tether      | `MAINHAND` | Tools + Weapons (`illyriaplus:tether_items`) |
| Nimbus      | `SADDLE`   | Harnesses (`ItemTypeTagKeys.HARNESSES`) |
| Embertread  | `FEET`     | Foot armor (`ItemTypeTagKeys.FOOT_ARMOR`) |

**Enchantment event handlers** (registered as listeners in `IllyriaPlus.onEnable()`):

- Embertread
- FeatherFalling
- Fortune
- Nimbus
- SilkTouch
- Tether
- Vinemine

FeatherFalling, Fortune, and SilkTouch are not registered in the bootstrap registry; they implement behavior by listening to events and checking for vanilla enchantments on items.

### PDCs (Persistent Data Containers)

PDC helpers in `src/pdcs/` expose Kotlin property delegates on entity types. `PlayerPDC` provides:

- `Player.nickname` — stored under `illyriaplus:nickname`; returns the player's real name when unset.
- `Player.scoreboardVisibility` — stored under `illyriaplus:scoreboard_visibility`; defaults to `false`.
- `Player.intoxication` — stored under `illyriaplus:intoxication`; current alcoholic intoxication level.
- `Player.lastDrinkTime` — stored under `illyriaplus:last_drink_time`; epoch millis of the last alcoholic drink.

### Paintings

Custom painting variants implement **`PaintingInterface`** and are registered in `IllyriaPlusBootstrap` via `RegistryEvents.PAINTING_VARIANT`. The interface provides:

- **`key`** — a `TypedKey<Art>` derived automatically from the class name (e.g. `AlphaPainting` → `illyriaplus:alpha`).
- **`assetKey`** — the snake_case fragment used for the sprite asset id, derived from the class name (e.g. `AlphaPainting` → `alpha`).
- **`title`** — the display title in proper case, also derived from the class name.
- **`invoke(builder)`** — configures the variant's asset id, width, height, title, and author.
- **`get()`** — looks up and returns the live `Art` instance from the registry after bootstrap.

Painting variants have no event listeners, so unlike mechanics and enchantments they are not registered in `IllyriaPlus.onEnable()`.

The **`YAPETTO`** shared namespace constant lives in `PaintingInterface.Companion`. Each painting object in `src/paintings/yapetto/` only hardcodes its width and height; the registry key, asset id, and title are all derived from the class name by stripping the `Painting` suffix. The author is always `YAPETTO`.

All **117** Yapetto variants are collected into the `paintings` list in `IllyriaPlusBootstrap` and added to the `minecraft:painting_variant/placeable` tag during bootstrap so they appear when placing paintings in-game.

### Damage Types

Custom damage types implement **`DamageTypeInterface`** and are registered in `IllyriaPlusBootstrap` via `RegistryEvents.DAMAGE_TYPE`. The interface provides:

- **`key`** — a `TypedKey<DamageType>` derived automatically from the class name (e.g. `AlcoholDamageType` → `illyriaplus:alcohol`).
- **`invoke(builder)`** — configures the damage type's registry entry (`damageEffect`, `damageScaling`, `deathMessageType`, `exhaustion`, `messageId`).
- **`get()`** — looks up and returns the live `DamageType` instance from the registry after bootstrap.

Damage types have no event listeners, so unlike mechanics and enchantments they are not registered in `IllyriaPlus.onEnable()`.

**Bootstrap-registered damage types:**

| Damage Type | Message ID | Purpose |
|-------------|------------|---------|
| Alcohol     | `alcohol_poisoning` | Alcohol poisoning damage from the `Alcohol` mechanic |

The `messageId` must have matching translations in the resource pack (`resourcepack/assets/illyriaplus/lang/en_us.json`) under `death.attack.<message_id>`, plus `.item` and `.player` variants when applicable.

### Recipes

Recipe objects implement **`RecipeInterface`** and are listed in `IllyriaPlus.onEnable()`. They expose `recipes` and `potions` collections plus a `register()` function that returns elapsed time in ms.

Currently **9** recipe modules are registered:

- AlcoholRecipe
- ChainmailRecipe
- DiamondRecycleRecipe
- IceBreakdownRecipe
- NetherWartBlockRecipe
- PaintingRecipe
- RottenFleshRecipe
- WoodLogRecipe
- WoolToStringRecipe

`AlcoholRecipe` provides 13 custom `PotionMix` brewing recipes.

### Data Classes

Data classes live in `src/data/`:

- `AdjacentBlockData`
- `BuildSetupData`
- `CommandData`
- `PaintingData`
- `TreeStructureData`

### Utilities

General utilities are in `src/Utils.kt` as the `internal object Utils`, with nested objects for each concern:

- `Utils.MM` — MiniMessage parser with custom gradient aliases
- `Utils.Enchantment` — enchantment display helpers
- `Utils.Schedule` — scheduled task helpers
- `Utils.Command` — Brigadier command execution helpers (`executesCatching`, `playerExecuted`)
- `Utils.Block` — block helpers
- `Utils.Player` — player helpers
- `Utils.Monster` — monster mount helpers

### Package Structure (`org.xodium.illyriaplus`)

| Package | Contents |
|---------|----------|
| `mechanics/` | 26 mechanic singletons (organized by category) |
| `mechanics/entity/monster/` | 6 monster-specific mechanics plus `MonsterInterface` |
| `data/` | `AdjacentBlockData`, `BuildSetupData`, `CommandData`, `TreeStructureData` |
| `enchantments/` | `EnchantmentInterface`; Embertread, Nimbus, Tether, Vinemine, FeatherFalling, Fortune, SilkTouch |
| `enchantments/utility/` | Custom utility enchantments registered in the bootstrap |
| `enchantments/vanilla/` | Vanilla enchantment behavior overrides |
| `paintings/` | `PaintingInterface`; 117 Yapetto painting variants from the Portfolio datapack |
| `damagetypes/` | `DamageTypeInterface`; custom damage types such as `AlcoholDamageType` |
| `recipes/` | `RecipeInterface`; 8 vanilla-style recipes |
| `pdcs/` | `PlayerPDC` |

### Key Conventions

- All internal classes are `internal` visibility.
- All mechanics/enchantments/recipes are `object` singletons.
- MiniMessage (`Utils.MM`) is used throughout for all text formatting.
- The `@Suppress("UnstableApiUsage")` annotation is needed whenever using Paper's experimental APIs (registry events, dialogs, enchantment builders, menu types, etc.).
- ktlint is enforced (configured in `.idea/ktlint-plugin.xml`); wildcard imports are disabled globally via `.editorconfig`.
- Recipe `NamespacedKey` naming: `{descriptive_name}_{recipe_type}` (e.g. `diamond_recycle_blasting_recipe`, `chainmail_helmet_shaped_recipe`).
- **Import types instead of using fully qualified paths** — e.g., `import org.bukkit.inventory.meta.PotionMeta` instead of `org.bukkit.inventory.meta.PotionMeta`.
- **Use `it` for single-parameter lambdas** — e.g., `list.forEach { it.doSomething() }` instead of `list.forEach { item -> item.doSomething() }`.
- **Use `ItemStack.of()` instead of `ItemStack()` constructor** — Paper's modern API for creating item stacks.
- **Don't create intermediate `const val` for override properties** — assign directly to the override, e.g., `override val key: String = "illyriaplus:my_potion"` instead of creating a `const val KEY` and then `override val key = KEY`.
- **Don't add KDoc to implemented overrides** — the base interface/class already has documentation; let it inherit naturally.
- **Use data class builders** — e.g., `potion(PotionData(color = X, displayName = Y))` instead of lambda receivers for simpler configuration.
- **Use explicit named factory functions** — prefer `potion()` and `splash()` over `invoke()` operator for clarity.
- **Alphabetical order for static collections** — static maps/lists should be sorted alphabetically by key.

### Code Structure (in interfaces, classes, objects)

Order members from top to bottom:

1. **`const val`** — compile-time constants
2. **`val`** — read-only properties (overrides first)
3. **`var`** — mutable properties (overrides first)
4. **`fun`** — functions (overrides first)

Within each group:

- **`override`** members go above regular members
- **`@EventHandler`** functions go above regular `public` functions
- **`@EventHandler`** functions should always be named `on(event: <EventType>)` — Kotlin allows multiple `@EventHandler fun on(...)` as long as parameter types differ
- **`@EventHandler`** functions should not have KDoc comments (the event type is self-documenting)
- **`public`** members go above **`private`** members
