package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Mead. */
@Suppress("UnstableApiUsage")
internal object MeadItem : AlcoholItemInterface {
    override val name: String = "<!i><yellow>Mead"
    override val alcoholStrength: Int = 1
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.YELLOW)
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 45.seconds.toTicks(), 0, false, true, true))
            .build()
}
