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

/** Ornate orichalcum boots fit for temple guardians. */
internal object OrichalcumBootsItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_BOOTS).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<gradient:#FFD700:#FF4500>$name</gradient>"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>Boots that echo with the footsteps of heroes.</gray>"))),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ARMOR,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_boots_armor"),
                            3.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.FEET,
                        ),
                    ).addModifier(
                        Attribute.ARMOR_TOUGHNESS,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_boots_toughness"),
                            3.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.FEET,
                        ),
                    ).addModifier(
                        Attribute.KNOCKBACK_RESISTANCE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.ID, "orichalcum_boots_knockback_resistance"),
                            0.15,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlotGroup.FEET,
                        ),
                    ).build(),
            )
        }
}
