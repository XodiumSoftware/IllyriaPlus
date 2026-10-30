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

/** A regal orichalcum helm burnished like Atlantean sunrise. */
internal object OrichalcumHelmetItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_HELMET).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#FFD700:#FF4500>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A crown-like helm that glitters with lost glory.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ARMOR,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_helmet_armor"),
                            3.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.HEAD,
                        ),
                    ).addModifier(
                        Attribute.ARMOR_TOUGHNESS,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_helmet_toughness"),
                            3.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.HEAD,
                        ),
                    ).addModifier(
                        Attribute.KNOCKBACK_RESISTANCE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_helmet_knockback_resistance"),
                            0.15,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.HEAD,
                        ),
                    ).build(),
            )
        }
}
