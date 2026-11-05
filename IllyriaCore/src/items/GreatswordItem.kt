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
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** Custom Greatsword item. */
internal object GreatswordItem : ItemsInterface {
    override val recipes =
        setOf(
            SmithingTransformRecipe(
                NamespacedKey(IllyriaCore.instance, "${id}_smithing_transform"),
                invoke(),
                RecipeChoice.exactChoice(GreatswordTemplateItem.invoke()),
                RecipeChoice.MaterialChoice(Material.NETHERITE_SWORD),
                RecipeChoice.MaterialChoice(Material.NETHERITE_BLOCK),
            ),
        )

    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SWORD).apply {
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("<gray>Type: $id <dark_gray>[<yellow>Rare<dark_gray>]"),
                        MM.deserialize("<dark_gray>An oversized blade built for sweeping, devastating strikes."),
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
                            12.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ATTACK_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_attack_speed"),
                            -2.9,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.MOVEMENT_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_movement_speed"),
                            -0.05,
                            AttributeModifier.Operation.ADD_SCALAR,
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
                    ).build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
