---
name: add-item
description: Scaffolds a new IllyriaPlus reusable item builder in src/items/{group}/ following the ItemInterface pattern.
---

# Add an Item

Use this skill when the user wants to add a new reusable item builder to IllyriaPlus.

## Before Writing Code

1. Ask the user:
   - What is the item name?
   - Which group/category folder under `src/items/` should it live in? (e.g. `alcoholics`, create a new one if needed)
   - What `Material` should it use?
   - Does it need any custom data components (name, color, lore, potion contents, food, etc.)?
   - Should it set `alcoholStrength`? Only for alcoholic items consumed by `AlcoholMechanic`.
2. If the item is an alcoholic drink, ensure it uses `PotionContents` with a custom color and a nausea effect, and stores `ALCOHOL_STRENGTH_KEY`.

## Creating the Item File

1. Create `src/items/{group}/<Name>Item.kt` using the template below.
2. The object must be `internal object <Name>Item : ItemInterface`.
3. Override `alcoholStrength` only for alcoholic items.
4. Implement `operator fun invoke(): ItemStack` returning a configured stack built with `ItemStack.of(Material.<...>)`.
5. Use `setData(DataComponentTypes.<...>, ...)` for data components.
6. Use `editPersistentDataContainer { }` to attach PDC values like `ALCOHOL_STRENGTH_KEY`.
7. Use `Utils.MM` for any formatted text.
8. Add `@Suppress("UnstableApiUsage")` when using experimental Paper data component APIs.

## Wiring

1. If the item is used by a recipe, import it in the relevant `src/recipes/vanilla/<Name>Recipe.kt` and reference it as the result item.
2. If the item is alcoholic, ensure it is consumed by `AlcoholRecipe` (or another brewing recipe) so players can obtain it in-game.
3. No registration in `IllyriaPlus.kt` is required for items themselves.

## Documentation

1. Add a concise KDoc comment to the object describing the item.
2. Update `ARCHITECTURE.md`:
   - Add the item to the relevant group list under the Items section.
   - If a new group folder is created, mention it in the project structure and items sections.
3. If the change is significant, run `./gradlew dokkaGenerateHtml`.

## Template

```kotlin
package org.xodium.illyriaplus.items.{group}

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.items.ItemInterface

/** Represents <description>. */
internal object <Name>Item : ItemInterface {
    override operator fun invoke(): ItemStack =
        ItemStack.of(Material.<MATERIAL>).apply {
            setData(DataComponentTypes.CUSTOM_NAME, /* ... */)
        }
}
```

For alcoholic items:

```kotlin
package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.items.ItemInterface
import org.xodium.illyriaplus.items.ItemInterface.Companion.ALCOHOL_STRENGTH_KEY
import org.xodium.illyriaplus.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of <Name>. */
internal object <Name>Item : ItemInterface {
    override val alcoholStrength: Int = <STRENGTH>

    @Suppress("UnstableApiUsage")
    override operator fun invoke(): ItemStack =
        ItemStack.of(Material.POTION).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<!i><#COLOR><Name>"))
            setData(
                DataComponentTypes.POTION_CONTENTS,
                PotionContents
                    .potionContents()
                    .customColor(Color.fromRGB(<R>, <G>, <B>))
                    .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, <DURATION>.toTicks(), <AMPLIFIER>, false, true, true))
                    .build(),
            )
            editPersistentDataContainer { it.set(ALCOHOL_STRENGTH_KEY, PersistentDataType.INTEGER, alcoholStrength) }
        }
}
```

After finishing, summarize the files changed and ask the user if they want to commit.
