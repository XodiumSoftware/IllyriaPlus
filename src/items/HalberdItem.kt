package org.xodium.illyriaplus.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.AttackRange
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils

/** Represents a Halberd. */
@Suppress("UnstableApiUsage")
internal object HalberdItem : ItemInterface {
    override val key: NamespacedKey = NamespacedKey(instance, "halberd")

    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SPEAR).apply {
            if (!hasData(DataComponentTypes.CUSTOM_NAME) && !hasData(DataComponentTypes.ITEM_NAME)) {
                setData(DataComponentTypes.CUSTOM_NAME, Utils.MM.deserialize("Halberd"))
            }
            setData(DataComponentTypes.ITEM_MODEL, key)
            setData(
                DataComponentTypes.ATTACK_RANGE,
                AttackRange
                    .attackRange()
                    .maxReach(4.0f)
                    .maxCreativeReach(4.0f)
                    .build(),
            )
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ATTACK_DAMAGE,
                        AttributeModifier(key, 9.5, AttributeModifier.Operation.ADD_NUMBER),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ATTACK_SPEED,
                        AttributeModifier(key, 1.3, AttributeModifier.Operation.ADD_NUMBER),
                        EquipmentSlotGroup.MAINHAND,
                    ).build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.INTEGER, 1) }
        }
}
