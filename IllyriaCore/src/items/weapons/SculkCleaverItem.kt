package org.xodium.illyriacore.items.weapons

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils
import org.xodium.illyriacore.items.ItemInterface

/** Represents a Sculk Cleaver. */
internal object SculkCleaverItem : ItemInterface {
    override val key: NamespacedKey = NamespacedKey(instance, "sculkcleaver")

    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SWORD).apply {
            if (!hasData(DataComponentTypes.CUSTOM_NAME) && !hasData(DataComponentTypes.ITEM_NAME)) {
                setData(DataComponentTypes.CUSTOM_NAME, Utils.MM.deserialize("Sculk Cleaver"))
            }
            setData(DataComponentTypes.ITEM_MODEL, key)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ATTACK_DAMAGE,
                        AttributeModifier(key, 8.0, AttributeModifier.Operation.ADD_NUMBER),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ATTACK_SPEED,
                        AttributeModifier(key, 1.6, AttributeModifier.Operation.ADD_NUMBER),
                        EquipmentSlotGroup.MAINHAND,
                    ).build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.INTEGER, 1) }
        }
}
