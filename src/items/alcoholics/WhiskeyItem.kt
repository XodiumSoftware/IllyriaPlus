package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Whiskey. */
internal object WhiskeyItem : AlcoholItemInterface {
    override val name: String = "<!i><#D2691E>Whiskey"
    override val alcoholStrength: Int = 3
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.fromRGB(210, 105, 30))
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 90.seconds.toTicks(), 2, false, true, true))
            .build()
}
