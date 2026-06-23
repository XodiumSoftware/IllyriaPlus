package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.toTicks
import kotlin.time.Duration.Companion.minutes

/** Represents a bottle of Red Wine. */
@Suppress("UnstableApiUsage")
internal object RedWineItem : AlcoholItemInterface {
    override val name: String = "<!i><dark_red>Red Wine"
    override val alcoholStrength: Int = 2
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.MAROON)
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 1.minutes.toTicks(), 1, false, true, true))
            .build()
}
