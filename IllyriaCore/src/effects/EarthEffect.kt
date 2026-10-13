package org.xodium.illyriacore.effects

import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.xodium.illyriacore.enums.BendingTypeEnum

/** Represents the earth bending effect. */
internal object EarthEffect : EffectInterface {
    override val type = BendingTypeEnum.EARTH

    private const val DUST_COUNT = 30
    private const val BLOCK_COUNT = 15
    private const val OFFSET = 0.4
    private const val DUST_SIZE = 1.5f

    private val DUST_COLOR: Color = Color.fromRGB(121, 85, 58)

    override fun spawn(location: Location) {
        Particle.DUST
            .builder()
            .location(location)
            .count(DUST_COUNT)
            .offset(OFFSET, OFFSET, OFFSET)
            .data(Particle.DustOptions(DUST_COLOR, DUST_SIZE))
            .spawn()

        Particle.BLOCK_CRUMBLE
            .builder()
            .location(location)
            .count(BLOCK_COUNT)
            .offset(OFFSET, OFFSET, OFFSET)
            .spawn()
    }
}
