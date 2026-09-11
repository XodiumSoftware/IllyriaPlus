# Installation

## Table of Contents

- [Prerequisites](#prerequisites)
- [Download Nightly Build](#download-nightly-build)
- [Build from Source](#build-from-source)
- [Installation](#installation-1)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

- [Paper](https://papermc.io/) Minecraft server 26.2
- Java 25 or newer

## Download Nightly Build

Download pre-built JARs from GitHub releases.

### Setup

1. Download the latest release:
   ```bash
   curl -L -o IllyriaBridge.jar https://github.com/XodiumSoftware/IllyriaBridge/releases/download/nightly/IllyriaBridge.jar
   ```

2. Place the JAR in your server's `plugins/` directory

## Build from Source

Build the plugin using Gradle.

### Prerequisites

- [JDK 21](https://adoptium.net/) or newer
- [Git](https://git-scm.com/)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/XodiumSoftware/IllyriaBridge.git
   cd IllyriaBridge
   ```

2. Build the plugin:
   ```bash
   ./gradlew shadowJar
   ```

3. The output JAR is at:
   ```
   build/libs/IllyriaBridge-*.jar
   ```

### Run a Test Server

To quickly test the plugin:

```bash
./gradlew runServer
```

This automatically downloads Paper 26.2 and starts a local test server with the plugin.

## Installation

1. Place the JAR in your server's `plugins/` directory
2. Start or restart the server
3. The plugin will enable automatically

## Configuration

IllyriaBridge requires **no configuration** — it works out of the box.

The plugin automatically:

- Detects Fabric and NeoForge clients via their client brand
- Syncs server recipes to JEI when players join
- Uses vanilla plugin channels (no additional setup required)

### Client Requirements

For players to see synced recipes, they need:

- **Fabric clients:** JEI mod installed
- **NeoForge clients:** JEI mod installed

## Troubleshooting

### "Plugin disabled itself"

- Verify server version is Paper 26.2
- Check console for version mismatch errors
- Update your server or use a compatible plugin version

### "Recipes not showing in JEI"

- Ensure players have JEI installed on their client
- Verify client is Fabric or NeoForge (not vanilla)
- Check console for payload encoding errors

### "Unknown client brand"

- Client brand detection requires the player to have fully joined
- Some modded clients may report non-standard brands
- Vanilla clients are not supported (no JEI on vanilla)

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

The project uses EditorConfig for code style. Ensure your IDE respects `.editorconfig` settings.

---

<p align="right"><a href="#readme-top">▲</a></p>
