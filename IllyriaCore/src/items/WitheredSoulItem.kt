package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** Stub item implementation for the Withered Soul custom item. */
internal object WitheredSoulItem : ItemsInterface {
    override fun invoke(): ItemStack =
        ItemStack.of(Material.PAPER).apply {
            setData(
                DataComponentTypes.CUSTOM_NAME,
                MM.deserialize("<gradient:#1a1a1a:#4a1942:#1a1a1a>$name</gradient>"),
            )
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize(
                            "A crystallized fragment of the Wither's soul, " +
                                "containing the destructive energy of the Nether.",
                        ),
                    ),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, "item/$id"))
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
