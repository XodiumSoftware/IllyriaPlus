---
name: add-item
description: Scaffolds a new custom item in IllyriaCore and wires it into the main plugin class, optionally creating the resource pack item model and wiki icon.
---

# Add an Item

Use this skill when the user wants to add a new custom item to the IllyriaPlus project.

## Before Writing Code

Ask the user:

1. What is the item name? (e.g., `CrystalMace`)
2. What is the base `Material`? (e.g., `Material.NETHERITE_SWORD`, `Material.BLAZE_ROD`)
3. What custom item model key should it use? (e.g., `illyriacore:crystal_mace`)
4. Does it need custom stats via `ATTRIBUTE_MODIFIERS`? If so, list damage, speed, knockback, movement speed, etc.
5. Does it need a smithing/crafting recipe to obtain it? What are the ingredients?
6. Does it have a special ability triggered by an event? (e.g., right-click fireball, drop from mob)
7. Is there a custom texture in `illyria-resourcepack`? Should the wiki get an icon?

## Creating the Item File

1. Create `IllyriaCore/src/items/<Name>Item.kt`.
2. The object must be `internal object <Name>Item : ItemsInterface`.
3. Use the inherited `id` and `key` properties for naming and PDC markers.
4. Set the `CUSTOM_NAME` (optional but recommended), `LORE`, and `ITEM_MODEL` data components.
5. Use `ItemStack.of(Material.<BASE>)` as the base material.
6. If the item needs attribute modifiers, set `ATTRIBUTE_MODIFIERS` with proper `NamespacedKey` names and `EquipmentSlotGroup.MAINHAND`.
7. If the item needs a recipe, override `val recipes` and return a set of recipes. Use the inherited `register()` from `ItemsInterface` to auto-register listeners + recipes.
8. Add `@EventHandler fun on(event: <EventType>)` methods following the project's naming convention.
9. Keep settings as `private const val` / `private val` directly in the object — no nested `Config` object.

## Wiring

1. Open `IllyriaCore/src/IllyriaCore.kt`.
2. Import the new item.
3. Add it alphabetically to the `items` list in `onEnable()`.
4. If the item has a smithing template item, add that item to the `items` list too.

## Resource Pack Item Model (optional)

If a custom texture exists in the resource pack:

1. Create `illyria-resourcepack/src/assets/illyriacore/items/<id>.json`:

   ```json
   {
     "model": {
       "type": "minecraft:model",
       "model": "illyriacore:item/weapons/<id>"
     }
   }
   ```

2. Create `illyria-resourcepack/src/assets/illyriacore/models/item/weapons/<id>.json` with the correct parent and `layer0` texture.
3. Place the texture at `illyria-resourcepack/src/assets/illyriacore/textures/item/weapons/<id>.png`.

## Wiki Icon (optional)

If the wiki should document the item:

1. Copy the texture from `illyria-resourcepack/src/assets/illyriacore/textures/item/weapons/<id>.png` to `illyria-wiki/src/img/item/<id>.png`.
2. Update the relevant wiki page (e.g., `weapons/index.md`) to show the item.

## Template

```kotlin
package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** Custom <Name> item. */
internal object <Name>Item : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.<BASE>).apply {
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("<short description>"),
                    ),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ATTACK_DAMAGE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_attack_damage"),
                            <damage>,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    )
                    .build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
```

After finishing, summarize the files changed and ask the user if they want to commit.
