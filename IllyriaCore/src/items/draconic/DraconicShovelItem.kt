package org.xodium.illyriacore.items.draconic

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

/** A draconic shovel whose edge glows with inner heat. */
internal object DraconicShovelItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SHOVEL).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#4B0082:#8B0000>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A spade whose edge smolders with draconic heat.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.BLOCK_BREAK_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "draconic_shovel_block_break_speed"),
                            0.20,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).addModifier(
                        Attribute.MINING_EFFICIENCY,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "draconic_shovel_mining_efficiency"),
                            4.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).addModifier(
                        Attribute.SUBMERGED_MINING_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "draconic_shovel_submerged_mining_speed"),
                            0.40,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).build(),
            )
        }
}
