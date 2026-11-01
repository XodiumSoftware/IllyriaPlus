package org.xodium.illyriacore.items.soul

import io.papermc.paper.datacomponent.DataComponentTypes
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyrialib.Utils.MM

/** The physical currency of lost souls. Dropped by affixed mobs and recovered from death stains. */
internal object SoulFragmentItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.ECHO_SHARD).apply {
            setData(DataComponentTypes.ITEM_NAME, MM.deserialize("<red>Soul Fragment</red>"))
            lore(
                listOf(
                    MM.deserialize("<gray>A crystallized remnant of a fallen soul.</gray>"),
                    MM.deserialize("<gray>Used to restore power at bonfires.</gray>"),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, itemModelKey)
        }
}
