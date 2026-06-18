package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.items.ItemInterface
import org.xodium.illyriaplus.items.ItemInterface.Companion.ALCOHOL_STRENGTH_KEY
import org.xodium.illyriaplus.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Rum. */
internal object RumItem : ItemInterface {
    override val alcoholStrength: Int = 3

    @Suppress("UnstableApiUsage")
    override operator fun invoke(): ItemStack =
        ItemStack.of(Material.POTION).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<!i><#8B4513>Rum"))
            setData(
                DataComponentTypes.POTION_CONTENTS,
                PotionContents
                    .potionContents()
                    .customColor(Color.fromRGB(139, 69, 19))
                    .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 90.seconds.toTicks(), 2, false, true, true))
                    .build(),
            )
            editPersistentDataContainer { it.set(ALCOHOL_STRENGTH_KEY, PersistentDataType.INTEGER, alcoholStrength) }
        }
}
