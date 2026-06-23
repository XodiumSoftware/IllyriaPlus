package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Chorus Wine. */
@Suppress("UnstableApiUsage")
internal object ChorusWineItem : AlcoholItemInterface {
    override val name: String = "<!i><#D8BFD8>Chorus Wine"
    override val alcoholStrength: Int = 3
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.fromRGB(216, 191, 216))
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 90.seconds.toTicks(), 2, false, true, true))
            .build()
}
