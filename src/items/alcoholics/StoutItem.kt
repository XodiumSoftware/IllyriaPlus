package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Stout. */
internal object StoutItem : AlcoholItemInterface {
    override val name: String = "<!i><#3E2723>Stout"
    override val alcoholStrength: Int = 2
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.fromRGB(62, 39, 35))
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 45.seconds.toTicks(), 1, false, true, true))
            .build()
}
