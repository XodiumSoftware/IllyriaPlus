package org.xodium.illyriacore.items.mithril

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

/** Agile leggings woven from rings of pure mithril. */
internal object MithrilLeggingsItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_LEGGINGS).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#C0C0C0:#E0FFFF>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>Leg guards as supple as elven-mail.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ARMOR,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "mithril_leggings_armor"),
                            1.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.LEGS,
                        ),
                    ).addModifier(
                        Attribute.ARMOR_TOUGHNESS,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "mithril_leggings_armor_toughness"),
                            1.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.LEGS,
                        ),
                    ).addModifier(
                        Attribute.KNOCKBACK_RESISTANCE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "mithril_leggings_knockback_resistance"),
                            0.05,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.LEGS,
                        ),
                    ).build(),
            )
        }
}
