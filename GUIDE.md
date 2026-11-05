# Installation

## Table of Contents

- [Prerequisites](#prerequisites)
- [Download Nightly Build](#download-nightly-build)
- [Build from Source](#build-from-source)
- [Installation](#installation-1)
- [Configuration](#configuration)
- [Features](#features)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

- [Paper](https://papermc.io/) Minecraft server 26.3
- Java 25

## Download Nightly Build

Download pre-built JARs from GitHub releases.

### Setup

1. Download the latest release:

    ```bash
    curl -L -o IllyriaPlus.jar https://github.com/XodiumSoftware/IllyriaPlus/releases/download/nightly/IllyriaPlus.jar
    ```

2. Place the JAR in your server's `plugins/` directory

## Build from Source

Build the plugins using Gradle.

### Prerequisites

- [JDK 25](https://adoptium.net/)
- [Git](https://git-scm.com/)

### Setup

1. Clone the repository:

    ```bash
    git clone https://github.com/XodiumSoftware/IllyriaPlus.git
    cd IllyriaPlus
    ```

2. Build the plugins:

    ```bash
    ./gradlew shadowJar
    ```

    To build a single plugin:

    ```bash
    ./gradlew :IllyriaCore:shadowJar
    ./gradlew :IllyriaBridge:shadowJar
    ```

3. The output JARs are at:
    ```
    IllyriaCore/build/libs/IllyriaPlus-*.jar
    IllyriaBridge/build/libs/IllyriaBridge-*.jar
    ```

### Run a Test Server

To quickly test a plugin:

```bash
./gradlew :IllyriaCore:runServer
./gradlew :IllyriaBridge:runServer
```

This automatically downloads Paper 26.3 and starts a local test server with the plugin.

## Installation

1. Place the JAR in your server's `plugins/` directory
2. Start or restart the server
3. The plugin will enable automatically

## Configuration

IllyriaPlus uses **compile-time configuration** — there are no config files to edit. All settings are hardcoded as constants directly in each module object.

To customize behavior, you must:

1. Fork the repository
2. Edit the constants in the relevant item, mechanic, enchantment, or recipe object
3. Rebuild the plugin

### Item Configuration Location

Each custom item stores its settings as `private const val` or `private val` properties directly in its `object`, for example:

```kotlin
// In IllyriaCore/src/items/ArcaneStaffItem.kt
internal object ArcaneStaffItem : ItemsInterface {
    private const val COOLDOWN_TICKS: Long = 10
    // ...
}
```

### Mechanic Configuration Location

Each mechanic stores its settings as `private const val` or `private val` properties directly in its `object`, for example:

```kotlin
// In IllyriaCore/src/mechanics/player/MessagesMechanic.kt
internal object MessagesMechanic : MechanicInterface {
    private const val JOIN_MSG = "<green>➕ <player> joined!</green>"
    // ...
}
```

## Features

Enhances base gameplay with custom enchantments, mechanics, and weapons.

For detailed player guides and documentation, see the **[Illyria Wiki](https://github.com/XodiumSoftware/illyria-wiki)**.

### Custom Weapons

IllyriaPlus adds craftable custom weapons, each forged at a smithing table with the vanilla netherite upgrade template:

| Weapon       | Damage | Speed | Notes                                        |
| ------------ | ------ | ----- | -------------------------------------------- |
| Halberd      | 9      | 1.8   | +2 knockback; polearm reach feel             |
| Greatsword   | 12     | 1.3   | +1 knockback, -5% movement speed             |
| Arcane Staff | —      | —     | Blaze-rod base; right-click shoots fireballs |
| Nullspire    | 20     | 3.0   | +2 reach, +20% speed; blinds on hit          |

The template is consumed during smithing, as are the two catalyst items.

### Enchantments

Custom enchantments are divided into two groups:

**Custom utility enchantments** (registered in Paper's registry):

| Enchantment | Slot      | Description                          |
| ----------- | --------- | ------------------------------------ |
| Vinemine    | Main Hand | Breaks connected ore veins           |
| Tether      | Main Hand | Auto-collects drops and XP           |
| Nimbus      | Saddle    | Faster Happy Ghast flight            |
| Embertread  | Feet      | Immunity to lava/fire contact damage |

**Vanilla behavior overrides** (event listeners, not registered as custom enchantments):

| Enchantment     | Slot      | Description                                     |
| --------------- | --------- | ----------------------------------------------- |
| Silk Touch      | Main Hand | Pickaxes can mine spawners and budding amethyst |
| Feather Falling | Feet      | Prevents farmland from being trampled           |
| Fortune         | Main Hand | Hoes with Fortune II+ auto-replant crops        |

#### Recipes

Custom crafting, smelting, stonecutting, and shapeless recipes:

- Chainmail armor crafting (Iron Bars)
- Blue/packed ice breakdown
- Nether wart block breakdown
- Custom paintings via stonecutter
- Rotten flesh to leather (furnace, smoker, campfire)
- Wood to log conversion
- Wool to string

### Client Bridging (IllyriaBridge)

The `IllyriaBridge` plugin bridges the server to modded clients with no server-side configuration:

- **Fabric/NeoForge clients with JEI** — server recipes are synced to JEI on join
- **Xaero's World Map / Minimap** — a persistent server world ID is sent so maps stay consistent across reconnects

Vanilla clients are unaffected.

## Troubleshooting

### "Plugin disabled itself"

- Verify server version is Paper 26.3
- Check console for version mismatch errors
- Update your server or use a compatible plugin version

### "Enchantments not showing"

- Enchantments are registered during server startup
- Check console for bootstrap errors
- Ensure you're using Paper (not Spigot or Bukkit)

### "Recipes not showing in JEI"

- Ensure the player is on a Fabric or NeoForge client with JEI installed
- Vanilla clients don't show synced recipes (no JEI on vanilla)
- Check console for payload encoding errors from IllyriaBridge

### Build fails

- Verify Java 25 is installed and active:
    ```bash
    java -version
    ```
- Make sure `JAVA_HOME` is set correctly
- Try cleaning the build:
    ```bash
    ./gradlew clean
    ./gradlew shadowJar
    ```

### Ktlint errors

The project uses ktlint for code style. Fix formatting:

```bash
./gradlew ktlintFormat
```

---

<p align="right"><a href="#readme-top">▲</a></p>
