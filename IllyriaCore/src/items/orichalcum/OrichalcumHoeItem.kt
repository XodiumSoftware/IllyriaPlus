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

/** A blessed orichalcum hoe that turns sand into garden soil. */
internal object OrichalcumHoeItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_HOE).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#FFD700:#FF4500>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>A blessed hoe that makes barren earth bloom.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.BLOCK_BREAK_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_hoe_block_break_speed"),
                            0.15,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).addModifier(
                        Attribute.MINING_EFFICIENCY,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_hoe_mining_efficiency"),
                            3.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).addModifier(
                        Attribute.SUBMERGED_MINING_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_hoe_submerged_mining_speed"),
                            0.30,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).build(),
            )
        }
}
