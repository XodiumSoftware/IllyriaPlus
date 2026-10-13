package org.xodium.illyriacore.effects

import org.bukkit.Location
import org.bukkit.Particle
import org.xodium.illyriacore.enums.BendingTypeEnum

/** Represents the water bending effect. */
internal object WaterEffect : EffectInterface {
    override val type = BendingTypeEnum.WATER

    private const val SPLASH_COUNT = 25
    private const val DRIP_COUNT = 10
    private const val OFFSET = 0.4

    override fun spawn(location: Location) {
        Particle.SPLASH
            .builder()
            .location(location)
            .count(SPLASH_COUNT)
            .offset(OFFSET, OFFSET, OFFSET)
            .spawn()

        Particle.DRIPPING_WATER
            .builder()
            .location(location)
            .count(DRIP_COUNT)
            .offset(OFFSET, OFFSET, OFFSET)
            .spawn()
    }
}
