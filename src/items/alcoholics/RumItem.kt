package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Rum. */
@Suppress("UnstableApiUsage")
internal object RumItem : AlcoholItemInterface {
    override val name: String = "<!i><#8B4513>Rum"
    override val alcoholStrength: Int = 3
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.fromRGB(139, 69, 19))
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 90.seconds.toTicks(), 2, false, true, true))
            .build()
}
