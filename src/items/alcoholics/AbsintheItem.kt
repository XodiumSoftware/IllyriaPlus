package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Absinthe. */
internal object AbsintheItem : AlcoholItemInterface {
    override val name: String = "<!i><#9ACD32>Absinthe"
    override val alcoholStrength: Int = 4
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.fromRGB(154, 205, 50))
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 120.seconds.toTicks(), 3, false, true, true))
            .build()
}
