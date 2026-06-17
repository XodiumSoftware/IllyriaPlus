---
name: add-enchantment
description: Scaffolds a new IllyriaPlus enchantment object, places it in the correct utility/ or vanilla/ package, and wires it into IllyriaPlusBootstrap and IllyriaPlus.kt when needed.
---

# Add an Enchantment

Use this skill when the user wants to add a new enchantment to IllyriaPlus.

## Before Writing Code

1. Ask the user:
   - What is the enchantment name?
   - Is it a custom registry enchantment (`utility/`) or a vanilla behavior override (`vanilla/`)?
   - What slot group should it use? (e.g. `MAINHAND`, `FEET`, `SADDLE`)
   - What item tag or items should it support?
   - Max level, weight, anvil cost, and optional level cost?
   - Which event(s) should it listen to?
2. If the user is unsure about slot group or supported items, suggest sensible defaults based on similar enchantments in the codebase.

## Creating the Enchantment File

1. Create `src/enchantments/{utility|vanilla}/<Name>Enchantment.kt` using the template below, adjusted for the user's choices.
2. The object must be `internal object <Name>Enchantment : EnchantmentInterface`.
3. For `utility/` enchantments, implement `invoke(builder)` and configure at minimum:
   - `description(key.displayName())`
   - `anvilCost()`
   - `maxLevel()`
   - `weight()`
   - `activeSlots()`
   - optionally `minimumCost()` / `maximumCost()`
4. For `vanilla/` enchantments, do not configure registry properties in `invoke`. Instead add `@EventHandler fun on(event: ...)` methods that check for the vanilla enchantment on the relevant item.
5. Add `@EventHandler fun on(event: <EventType>)` methods following the project's `fun on(event: ...)` naming convention.
6. Keep settings as `private const val` / `private val` directly in the object — no nested `Config` object.

## Wiring

### Utility enchantments only

1. Open `src/IllyriaPlusBootstrap.kt`.
2. In the `RegistryEvents.ENCHANTMENT` handler, add a new `register(<Name>Enchantment.key) { <Name>Enchantment.invoke(it).supportedItems(event.getOrCreateTag(<TAG>)) }` call.
3. Add `<Name>Enchantment.key` to the `enchants` set used for `TRADEABLE`, `NON_TREASURE`, and `IN_ENCHANTING_TABLE` tags.
4. If the enchantment needs a new item tag, add it in the `LifecycleEvents.TAGS.preFlatten` handler.

### All enchantments

1. Open `src/IllyriaPlus.kt`.
2. Import the new enchantment.
3. Add it alphabetically to the `enchantments` list in `onEnable()`.

## Documentation

1. Update `ARCHITECTURE.md`:
   - Add the enchantment to the appropriate table.
   - If it is a utility enchantment, add it to the bootstrap-registered table.
   - If it is a vanilla override, add it to the event-handlers list.
2. Add a concise KDoc comment to the object explaining its behavior.
3. If the change is significant, run `./gradlew dokkaGenerateHtml` to regenerate docs.

## Template

```kotlin
package org.xodium.illyriaplus.enchantments.{utility|vanilla}

import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.event.EventHandler
import org.bukkit.event.<EventType>
import org.bukkit.inventory.EquipmentSlotGroup
import org.xodium.illyriaplus.Utils.Enchantment.displayName
import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/** Represents an object handling <description> within the system. */
@Suppress("UnstableApiUsage")
internal object <Name>Enchantment : EnchantmentInterface {
    override fun invoke(builder: EnchantmentRegistryEntry.Builder): EnchantmentRegistryEntry.Builder =
        builder
            .description(key.displayName())
            .anvilCost(<cost>)
            .maxLevel(<maxLevel>)
            .weight(<weight>)
            .activeSlots(EquipmentSlotGroup.<SLOT>)

    @EventHandler
    fun on(event: <EventType>) {
        // implementation
    }
}
```

After finishing, summarize the files changed and ask the user if they want to commit.
