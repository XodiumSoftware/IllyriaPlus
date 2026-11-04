package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** Custom Greatsword item. */
internal object GreatswordItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SWORD).apply {
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("An oversized blade built for sweeping, devastating strikes."),
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
                            NamespacedKey(IllyriaCore.instance, ""),
                            0.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                    ),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
