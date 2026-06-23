package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Glowshine. */
@Suppress("UnstableApiUsage")
internal object GlowshineItem : AlcoholItemInterface {
    override val name: String = "<!i><#E6E6FA>Glowshine"
    override val alcoholStrength: Int = 2
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.fromRGB(230, 230, 250))
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 45.seconds.toTicks(), 1, false, true, true))
            .build()
}
