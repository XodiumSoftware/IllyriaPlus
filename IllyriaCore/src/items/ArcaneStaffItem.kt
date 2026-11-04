package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** Custom Arcane Staff item. */
internal object ArcaneStaffItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.BLAZE_ROD).apply {
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("A staff humming with raw arcane energy, waiting to be unleashed."),
                    ),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
