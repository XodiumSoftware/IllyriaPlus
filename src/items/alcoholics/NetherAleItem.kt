package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Nether Ale. */
@Suppress("UnstableApiUsage")
internal object NetherAleItem : AlcoholItemInterface {
    override val name: String = "<!i><#8B0000>Nether Ale"
    override val alcoholStrength: Int = 3
    override val content: PotionContents =
        PotionContents
            .potionContents()
            .customColor(Color.fromRGB(139, 0, 0))
            .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 90.seconds.toTicks(), 2, false, true, true))
            .build()
}
