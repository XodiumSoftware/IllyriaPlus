---
name: add-item
description: Scaffolds a new reusable item builder in src/items/{group}/ following the ItemInterface pattern.
---

# Add an Item

Use this skill when the user wants to add a new reusable item builder to the project.

## Before Writing Code

1. Ask the user:
   - What is the item name?
   - Which group/category folder under `src/items/` should it live in? (e.g. `alcoholics`, create a new one if needed)
   - What `Material` should it use?
   - Does it need any custom data components (name, color, lore, potion contents, food, etc.)?
   - Should it set `alcoholStrength`? Only for alcoholic items consumed by the relevant mechanic.
2. If the item is an alcoholic drink, ensure it uses `PotionContents` with a custom color and a nausea effect, and stores the project's alcohol-strength PDC key.

## Creating the Item File

1. Create `src/items/{group}/<Name>Item.kt` using the template below.
2. The object must be `internal object <Name>Item : ItemInterface`.
3. Override `alcoholStrength` only for alcoholic items.
4. Implement `operator fun invoke(): ItemStack` returning a configured stack built with `ItemStack.of(Material.<...>)`.
5. Use `setData(DataComponentTypes.<...>, ...)` for data components.
6. Use `editPersistentDataContainer { }` to attach PDC values.
7. Use the project's text formatting helper (e.g., `Utils.MM`) for any formatted text.
8. Add `@Suppress("UnstableApiUsage")` when using experimental Paper data component APIs.

## Wiring

1. If the item is used by a recipe, import it in the relevant recipe file and reference it as the result item.
2. If the item is alcoholic, ensure it is consumed by a brewing recipe so players can obtain it in-game.
3. No registration in the main plugin class is required for items themselves.

## Documentation

1. Add a concise KDoc comment to the object describing the item.
2. Update `ARCHITECTURE.md`:
   - Add the item to the relevant group list under the Items section.
   - If a new group folder is created, mention it in the project structure and items sections.
3. If the change is significant, run `./gradlew dokkaGenerateHtml`.

## Template

```kotlin
package <project.package>.items.{group}

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import <project.package>.items.ItemInterface

/** Represents <description>. */
internal object <Name>Item : ItemInterface {
    override operator fun invoke(): ItemStack =
        ItemStack.of(Material.<MATERIAL>).apply {
            setData(DataComponentTypes.CUSTOM_NAME, /* ... */)
        }
}
```

Replace `<project.package>` with the actual project package (e.g., `org.xodium.illyriaplus`).

After finishing, summarize the files changed and ask the user if they want to commit.
