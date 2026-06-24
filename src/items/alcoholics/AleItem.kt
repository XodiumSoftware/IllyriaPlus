package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Ale. */
@Suppress("UnstableApiUsage")
internal object AleItem : AlcoholItemInterface {
    override val name: String = "<!i><gold>Ale"
    override val alcoholStrength: Int = 1
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.ORANGE)
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 30.seconds.toTicks(), 0, false, true, true))
            .build()
}
