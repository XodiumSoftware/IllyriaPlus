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

/** A jagged draconic axe that tears through bark and bone. */
internal object DraconicAxeItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_AXE).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#4B0082:#8B0000>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(MM.deserialize("<gray>An axe jagged enough to tear through scale and bone.</gray>")),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ATTACK_DAMAGE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "draconic_axe_damage"),
                            3.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).addModifier(
                        Attribute.ATTACK_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "draconic_axe_speed"),
                            -0.1,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.MAINHAND,
                        ),
                    ).build(),
            )
        }
}
