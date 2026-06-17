package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.items.ItemInterface
import org.xodium.illyriaplus.toTicks
import kotlin.time.Duration.Companion.minutes

/** Represents a bottle of Red Wine. */
internal object RedWineItem : ItemInterface {
    @Suppress("UnstableApiUsage")
    override operator fun invoke(): ItemStack =
        ItemStack.of(Material.POTION).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<!i><dark_red>Red Wine"))
            setData(
                DataComponentTypes.POTION_CONTENTS,
                PotionContents
                    .potionContents()
                    .customColor(Color.MAROON)
                    .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 1.minutes.toTicks(), 1, false, true, true))
                    .build(),
            )
        }
}
