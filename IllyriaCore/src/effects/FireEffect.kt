package org.xodium.illyriacore.effects

import org.bukkit.Location
import org.bukkit.Particle
import org.xodium.illyriacore.enums.BendingTypeEnum

/** Represents the fire bending effect. */
internal object FireEffect : EffectInterface {
    override val type = BendingTypeEnum.FIRE

    private const val FLAME_COUNT = 25
    private const val SMOKE_COUNT = 10
    private const val OFFSET = 0.4

    override fun spawn(location: Location) {
        Particle
            .FLAME
            .builder()
            .location(location)
            .count(FLAME_COUNT)
            .offset(OFFSET, OFFSET, OFFSET)
            .spawn()

        Particle
            .SMOKE
            .builder()
            .location(location)
            .count(SMOKE_COUNT)
            .offset(OFFSET, OFFSET, OFFSET)
            .spawn()
    }
}
