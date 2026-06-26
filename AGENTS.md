# IllyriaPlus — Agents Context

## Project at a Glance

- **Name:** IllyriaPlus
- **Type:** Single-module Minecraft Paper plugin project (server-side only)
- **MC Version:** 26.1.2
- **Language:** Kotlin (JVM 25)
- **Build Tool:** Gradle with Kotlin DSL

## APIs & Tools

| Category            | Technology                              | Purpose                            |
|---------------------|-----------------------------------------|------------------------------------|
| **Core API**        | [Paper API](https://papermc.io/) 26.1.2 | Minecraft server plugin API        |
| **Language**        | Kotlin 2.4.0                            | JVM language                       |
| **Build Tool**      | Gradle (Kotlin DSL)                     | Build automation                   |
| **Gradle Plugins**  | Shadow 9.4.2                            | Fat JAR creation                   |
|                     | run-paper 3.0.2                         | Local test server                  |
|                     | resource-factory 1.3.1                  | `paper-plugin.yml` generation      |
|                     | foojay-resolver 1.0.0                   | Auto-download JVM toolchains       |
|                     | Dokka 2.2.0                             | Kotlin API documentation           |
|                     | ktlint 12.3.0                           | Kotlin linting                     |
| **Text Formatting** | MiniMessage                             | Adventure API component-based text |
| **Docs**            | Dokka                                   | Kotlin API documentation           |
| **Code Style**      | ktlint                                  | Kotlin linting (IDE plugin)        |

### Paper API Resources

- **Documentation**: https://docs.papermc.io/paper/dev/
- **JavaDoc**: https://jd.papermc.io/paper/26.1.2/ (matches project version)

### Paper API Notes

- Uses modern lifecycle/registry APIs (experimental — requires `@Suppress("UnstableApiUsage")`)
- Custom enchantments use `RegistryEvents.ENCHANTMENT`
- Item tags created via `LifecycleEvents.TAGS.preFlatten`
- Enchantment tags created via `LifecycleEvents.TAGS.postFlatten`
- Plugin bootstrapper pattern for early registry access

### Dokka

Documentation is generated with Dokka from KDoc comments in the source code.

- Run `./gradlew dokkaGenerateHtml` to generate documentation
- Output goes to `docs/` directory (published to GitHub Pages automatically)
- Auto-deployed via GitHub Actions on pushes to main
- Key files to document: interfaces and public APIs
- Use KDoc format: `/** ... */` with Markdown support

## Quick Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run local test server (auto-downloads Paper 26.1.2)
./gradlew runServer

# Generate Dokka documentation
./gradlew dokkaGenerateHtml

# Run linting
./gradlew ktlintCheck

# Fix linting issues
./gradlew ktlintFormat
```

## Project Structure

```
IllyriaPlus/
├── build.gradle.kts          # Build configuration
├── settings.gradle.kts         # Project settings
├── src/                        # Source directory
│   ├── IllyriaPlus.kt          # Main plugin class
│   ├── IllyriaPlusBootstrap.kt # Bootstrap class
│   ├── Utils.kt                # Utility functions
│   ├── mechanics/              # Feature mechanics (entity, player, server, world subfolders)
│   ├── enchantments/           # Enchantment implementations
│   │   ├── utility/            # Custom utility enchantments (registered in bootstrap)
│   │   └── vanilla/            # Vanilla enchantment behavior overrides
│   ├── recipes/                # Recipe implementations
│   │   └── vanilla/            # Vanilla-style custom recipes
│   ├── data/                   # Data classes
│   └── pdcs/                   # PlayerPDC
└── docs/                       # Generated documentation
```

## Architecture

### Entry Points

1. **IllyriaPlusBootstrap** (`PluginBootstrap`) — Runs before plugin enable. Creates item tags (`illyriaplus:tools`, `illyriaplus:weapons`, `illyriaplus:tether_items`), registers custom enchantments into Paper's registry, and tags them as tradeable/non-treasure/in-enchanting-table.
2. **IllyriaPlus** (`JavaPlugin`) — Main class. Validates server version, registers recipes, mechanics, and enchantment event listeners.

### Module System

- All mechanics are Kotlin `object` singletons implementing `MechanicInterface` (extends Bukkit `Listener`)
- All enchantments are Kotlin `object` singletons implementing `EnchantmentInterface` (extends Bukkit `Listener`)
- All recipes are Kotlin `object` singletons implementing `RecipeInterface`
- Modules self-register in `IllyriaPlus.onEnable()` via their `register()` function
- **There is no file-based configuration and no nested `Config` object** — all settings are compile-time constants declared directly in each module object
- To disable a module, remove it from the corresponding list in `IllyriaPlus.onEnable()`

### Enchantments

Custom enchantments implement `EnchantmentInterface` with:

- Auto-generated `TypedKey<Enchantment>` from class name (e.g., `VinemineEnchantment` → `illyriaplus:vinemine`)
- `invoke(builder)` to configure registry entry (description, cost, levels, weight, slots)
- `get()` to retrieve live `Enchantment` instance from registry
- Event handling via `@EventHandler fun on(event: <EventType>)` methods

Only utility enchantments in `src/enchantments/utility/` are registered in `IllyriaPlusBootstrap`. Enchantments in `src/enchantments/vanilla/` listen to events and check for vanilla enchantments on items instead.

### Key Conventions

- All internal classes use `internal` visibility
- All mechanics/enchantments/recipes are `object` singletons
- Use MiniMessage (`Utils.MM`) for all text formatting
- Add `@Suppress("UnstableApiUsage")` when using Paper's experimental APIs
- ktlint is enforced; wildcard imports are disabled globally via `.editorconfig`
- Recipe `NamespacedKey` naming: `{descriptive_name}_{recipe_type}`
- **Import types instead of using fully qualified paths** — e.g., `import org.bukkit.inventory.meta.PotionMeta` instead of `org.bukkit.inventory.meta.PotionMeta`
- **Use `it` for single-parameter lambdas** — e.g., `list.forEach { it.doSomething() }` instead of `list.forEach { item -> item.doSomething() }`
- **Use `ItemStack.of()` instead of `ItemStack()` constructor** — Paper's modern API for creating item stacks
- **Don't create intermediate `const val` for override properties** — assign directly to the override, e.g., `override val key: String = "illyriaplus:my_potion"` instead of creating a `const val KEY` and then `override val key = KEY`
- **Don't add KDoc to implemented overrides** — the base interface/class already has documentation; let it inherit naturally
- **Use data class builders** — e.g., `potion(PotionData(color = X, displayName = Y))` instead of lambda receivers for simpler configuration
- **Use explicit named factory functions** — prefer `potion()` and `splash()` over `invoke()` operator for clarity
- **Alphabetical order for static collections** — static maps/lists should be sorted alphabetically by key

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
- **`private const val`** and **`private val`** should not have KDoc comments — private constants/values are self-explanatory by their name or enforced by the compiler
- When KDoc is used, follow Kotlin idiomatic style: document parameters with `@param`, return values with `@return`, and exceptions with `@throws` where they add value
- **`public`** members go above **`private`** members

## Testing

- No automated tests in this project
- Test by running `./gradlew runServer` and manually verifying in-game

## Important Notes

- No file-based configuration — all settings are compile-time constants declared directly in module objects
- Enchantments must be registered in `IllyriaPlusBootstrap` AND tagged as tradeable/non-treasure/enchanting-table to appear in vanilla systems
- Project uses Paper's modern lifecycle/registry APIs extensively

## Claude Code Workflow

### Task Management

**When creating tasks:**

- Number tasks in the name (e.g., "1. Add Verdance enchantment", "2. Update nickname system")
- This makes it easy to reference specific tasks in conversation

**After completing each task:**

- Ask the user if they want to git commit the changes or adjust before committing

**When all tasks in a worktree are complete:**

- Ask the user if they want to git publish (push) the changes or adjust before publishing

### After Making Edits

**Always update documentation when code changes:**

1. **ARCHITECTURE.md** — Update if you:
    - Add/remove enchantments, mechanics, recipes, or managers
    - Change the mechanic system or interfaces
    - Change project structure or conventions

2. **GUIDE.md** — Update if you:
    - Change build commands or installation steps
    - Add/remove major features

3. **KDoc comments** — Add/update if you:
    - Add new public APIs (interfaces, managers, utils)
    - Change existing function signatures or behavior
    - Add complex logic that needs explanation
    - **Run `./gradlew dokkaGenerateHtml`** to regenerate docs after changes

**Rule of thumb:** If a code change would confuse someone reading the docs, update the docs.

## CI/CD

GitHub Actions workflows in `.github/workflows/`:

- **kotlin.yml** — Builds shadow JAR on push/PR, uploads artifacts, creates nightly release
- **enforce_pr_title.yml** — Validates PR titles follow conventional commits

## Adding Components

### Adding an Enchantment

1. Create new file in `src/enchantments/utility/YournameEnchantment.kt` for custom registry enchantments, or `src/enchantments/vanilla/YournameEnchantment.kt` for vanilla behavior overrides
2. Implement `EnchantmentInterface` as an `object`
3. In `invoke(builder)`, configure: `description()`, `anvilCost()`, `maxLevel()`, `weight()`, `activeSlots()`, and optionally `supportedItems()`
4. In `IllyriaPlusBootstrap.kt` (only for `utility/` enchantments):
    - Add `YournameEnchantment` to the registry handler, usually chaining `.supportedItems()` with a tag from the registry event
    - Add it to the tags (tradeable, non-treasure, enchanting-table)
    - Add supported items to appropriate `ItemTag` if needed
5. In `IllyriaPlus.kt`:
    - Add `YournameEnchantment` to the `enchantments` list
6. Update `ARCHITECTURE.md` enchantment table
7. Add KDoc comments to explain the enchantment's behavior
8. Run `./gradlew dokkaGenerateHtml` to regenerate documentation

### Adding a Mechanic

1. Create new file in `src/mechanics/{category}/YourMechanic.kt` (e.g., `src/mechanics/player/YourMechanic.kt`)
2. Implement `MechanicInterface` as an `object`
3. Hardcode settings as `private const val` / `private val` properties directly in the object (no nested `Config` object)
4. Implement `@EventHandler` methods for events
5. Register commands/permissions by overriding `cmds` and `perms` if needed
6. In `IllyriaPlus.kt`, add `YourMechanic` to the `mechanics` list in `onEnable()`
7. Add KDoc comments explaining the mechanic's purpose and features, use samples from other mechanics.
8. Update `ARCHITECTURE.md` mechanic count and list

### Adding a Recipe

1. Create new file in `src/recipes/vanilla/YourRecipe.kt`
2. Implement `RecipeInterface` as an `object`
3. Define `recipes` list for crafting/smelting recipes, or `potions` list for brewing recipes
4. Use naming pattern `{descriptive_name}_{recipe_type}` for `NamespacedKey`
5. In `IllyriaPlus.kt`, add `YourRecipe` to the `recipes` list in `onEnable()`
6. Add KDoc comments describing the recipe
7. Update `ARCHITECTURE.md` recipe list

### Adding a PDC (Persistent Data Container)

1. For player data: edit `src/pdcs/PlayerPDC.kt`
2. Add a new property delegate using `by` with `NamespacedKey(instance, "key_name")`
3. Use primitive `PersistentDataType` values or custom serializers for complex data
4. Access via `player.yourField`, etc. directly in code
5. Document the new PDC field in `ARCHITECTURE.md` if significant

### Adding a Data Class

1. Create new file in `src/data/YourData.kt`
2. Define `data class` with properties for structured data
3. Keep data classes immutable (`val` properties)
4. Add appropriate helper methods or companion object factory functions
5. Document if used across multiple mechanics

### Adding Utilities

1. Edit `src/Utils.kt` or add a nested object inside `Utils`
2. Keep utility functions `internal` visibility
3. Prefer extension functions on existing types
4. Use `Utils.MM` for MiniMessage formatting
5. Add KDoc comments for complex utilities

## Memory System

This project uses Claude Code's persistent memory in `.claude/memory/`. These files persist across sessions and different PCs. Review `MEMORY.md` for existing context about the user and project.
