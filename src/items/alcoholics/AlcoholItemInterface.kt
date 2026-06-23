package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.items.ItemInterface
import org.xodium.illyriaplus.mechanics.player.AlcoholMechanic.ALCOHOL_STRENGTH_KEY

/** Represents a contract for reusable item builders within the system. */
@Suppress("UnstableApiUsage")
internal interface AlcoholItemInterface : ItemInterface {
    /** The display name of this alcoholic item, formatted with MiniMessage. */
    val name: String

    /** The alcoholic strength of this item. Zero for non-alcoholic items. */
    val alcoholStrength: Int get() = 0

    /** The potion contents (color and effects) applied to this item. */
    val content: PotionContents

    override fun invoke(): ItemStack =
        ItemStack.of(Material.POTION).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize(name))
            setData(DataComponentTypes.POTION_CONTENTS, content)
            editPersistentDataContainer {
                it.set(ALCOHOL_STRENGTH_KEY, PersistentDataType.INTEGER, alcoholStrength)
            }
        }
}
