package org.xodium.illyriacore.items.orichalcum

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** A radiant orichalcum cuirass warm to the touch. */
internal object OrichalcumChestplateItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_CHESTPLATE).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#FFD700:#FF4500>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A breastplate said to glow beneath temple fires.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ARMOR,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_chestplate_armor"),
                            3.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.CHEST,
                        ),
                    ).addModifier(
                        Attribute.ARMOR_TOUGHNESS,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_chestplate_toughness"),
                            3.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.CHEST,
                        ),
                    ).addModifier(
                        Attribute.KNOCKBACK_RESISTANCE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_chestplate_knockback_resistance"),
                            0.15,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.CHEST,
                        ),
                    ).build(),
            )
        }
}
