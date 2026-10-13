package org.xodium.illyriacore.effects

import org.bukkit.Location
import org.bukkit.Particle
import org.xodium.illyriacore.enums.BendingTypeEnum

/** Represents the air bending effect. */
internal object AirEffect : EffectInterface {
    override val type = BendingTypeEnum.AIR

    private const val CLOUD_COUNT = 20
    private const val GUST_COUNT = 10
    private const val OFFSET = 0.5

    override fun spawn(location: Location) {
        Particle.CLOUD
            .builder()
            .location(location)
            .count(CLOUD_COUNT)
            .offset(OFFSET, OFFSET, OFFSET)
            .spawn()

        Particle.SWEEP_ATTACK
            .builder()
            .location(location)
            .count(GUST_COUNT)
            .offset(OFFSET, OFFSET, OFFSET)
            .spawn()
    }
}
