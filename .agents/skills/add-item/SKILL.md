---
name: add-item
description: Scaffolds a new custom item object implementing ItemInterface with auto-generated keys and resource pack setup instructions.
---

# Add an Item

Use this skill when the user wants to add a new custom item to the project.

## Before Writing Code

1. Ask the user:
   - What is the item name?
   - What material should be used as the base? (e.g., `Material.TRIAL_KEY`, `Material.NETHER_STAR`)
   - What should the display name be? (MiniMessage format, can include gradients)
   - What lore/description should it have? (optional)
   - Should it have any special data components? (e.g., enchantment glint, custom model)
   - Does it need a custom texture in the resource pack?

## Creating the Item File

1. Create `IllyriaCore/src/items/<Name>Item.kt` using the template below.
2. The object must be `internal object <Name>Item : ItemInterface`.
3. The key is auto-generated from the class name (strips "Item" suffix, converts to snake_case).
4. Use `MM.deserialize()` for the custom name with MiniMessage formatting.
5. Set `ITEM_MODEL` data component to `key` for custom model mapping.
6. Add KDoc comment explaining the item's purpose.

## Resource Pack Setup (if custom texture needed)

1. Create model file: `IllyriaResourcePack/assets/illyriacore/models/item/<item_name>.json`
2. Add texture: `IllyriaResourcePack/assets/illyriacore/textures/item/<item_name>.png`
3. Create or update override in `IllyriaResourcePack/assets/minecraft/items/<base_material>.json`
   - Add case for the item's key: `illyriacore:<item_name>`
   - Point to the custom model

## Template

```kotlin
package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyrialib.Utils.MM

/** Represents the <description> item. */
internal object <Name>Item : ItemInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.<BASE_MATERIAL>).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<display_name>"))
            setData(DataComponentTypes.ITEM_MODEL, key)
        }
}
```

## Example (Incendium Key)

```kotlin
package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyrialib.Utils.MM

/** Represents the Incendium Key item used to unlock Nether portals. */
internal object IncendiumKeyItem : ItemInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.TRIAL_KEY).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#8B0000:#FF4500:#FF6347>Incendium Key"))
            setData(DataComponentTypes.ITEM_MODEL, key)
        }
}
```

After finishing, summarize the files changed and remind the user to:
- Add the item to any recipes if craftable
- Add mechanics/event handlers if the item has special behavior
- Create resource pack assets if custom texture is needed
- Ask the user if they want to commit
