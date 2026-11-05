package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/**
 * Custom Nullspire item.
 *
 * A slender, void-touched spear whose purple core pierces armor and wards alike. Unbound from
 * mortal limits, it strikes harder and faster than any mortal weapon while reaching well beyond
 * its wielder's arm.
 */
internal object NullspireItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SWORD).apply {
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("A needle of collapsed void, its violet core humming with nothingness."),
                    ),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ATTACK_DAMAGE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_attack_damage"),
                            20.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ATTACK_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_attack_speed"),
                            2.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ATTACK_KNOCKBACK,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_attack_knockback"),
                            1.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ENTITY_INTERACTION_RANGE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_entity_interaction_range"),
                            2.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.MOVEMENT_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_movement_speed"),
                            0.2,
                            AttributeModifier.Operation.ADD_SCALAR,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
