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

/** A gleaming orichalcum spear steeped in ancient legend. */
internal object OrichalcumSpearItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SPEAR).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#FFD700:#FF4500>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A gleaming spear said to channel ancient power.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ATTACK_DAMAGE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_spear_damage"),
                            1.5,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).addModifier(
                        Attribute.ATTACK_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_spear_speed"),
                            0.1,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).build(),
            )
        }
}
