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

- [Paper](https://papermc.io/) Minecraft server 26.2
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
    ./gradlew :IllyriaKingdoms:shadowJar
    ```

3. The output JARs are at:
    ```
    IllyriaCore/build/libs/IllyriaPlus-*.jar
    IllyriaKingdoms/build/libs/IllyriaKingdoms-*.jar
    ```

### Run a Test Server

To quickly test a plugin:

```bash
./gradlew :IllyriaCore:runServer
./gradlew :IllyriaKingdoms:runServer
```

This automatically downloads Paper 26.2 and starts a local test server with the plugin.

## Installation

1. Place the JAR in your server's `plugins/` directory
2. Start or restart the server
3. The plugin will enable automatically

## Configuration

IllyriaPlus uses **compile-time configuration** — there are no config files to edit. All settings are hardcoded as constants directly in each module object.

To customize behavior, you must:

1. Fork the repository
2. Edit the constants in the relevant mechanic, enchantment, or recipe object
3. Rebuild the plugin

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

Enhances base gameplay with custom enchantments, items, and mechanics.

### Wandering Trader Shop

Wandering traders open a custom shop GUI instead of their vanilla trades. The stock is shared globally, persists across restarts, and starts empty — it is filled by players selling items via the Sell items button. Any item except emeralds and emerald blocks is accepted. Prices start at 2 emeralds per item and drift with supply and demand (every 32 net items bought or sold shifts the price by one emerald, clamped to 1-64, re-priced per item within bulk trades); selling pays out at half the current price. Left-click buys one unit, middle-click ten, right-click a hundred (capped by stock and balance). The shop view refreshes live for all players viewing it.

### Enchantments

Custom enchantments are divided into two groups:

**Custom utility enchantments** (registered in Paper's registry):

| Enchantment | Slot      | Description                     |
| ----------- | --------- | ------------------------------- |
| Vinemine    | Main Hand | Pickaxe special ability         |
| Tether      | Main Hand | Applies to tools and weapons    |
| Nimbus      | Saddle    | Happy Ghast harness enhancement |
| Embertread  | Feet      | Foot armor enhancement          |

**Vanilla behavior overrides** (event listeners, not registered as custom enchantments):

| Enchantment     | Slot      | Description                                 |
| --------------- | --------- | ------------------------------------------- |
| Silk Touch      | Main Hand | Allows mining spawners and budding amethyst |
| Feather Falling | Feet      | Prevents farmland from being trampled       |
| Fortune         | Main Hand | Hoes with Fortune II+ auto-replant crops    |

#### Recipes

Custom crafting, smelting, stonecutting, and shapeless recipes:

- Chainmail armor crafting
- Diamond recycle (blasting)
- Blue/packed ice breakdown
- Nether wart block breakdown
- Custom paintings via stonecutter
- Rotten flesh to leather
- Log/wood crafting improvements
- Wool to string

## Troubleshooting

### "Plugin disabled itself"

- Verify server version is Paper 26.1.2
- Check console for version mismatch errors
- Update your server or use a compatible plugin version

### "Enchantments not showing"

- Enchantments are registered during server startup
- Check console for bootstrap errors
- Ensure you're using Paper (not Spigot or Bukkit)

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
