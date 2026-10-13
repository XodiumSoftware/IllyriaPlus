package org.xodium.illyriacore.effects

import org.bukkit.Location
import org.xodium.illyriacore.enums.BendingTypeEnum

/** Represents a contract for a particle effect within the system. */
internal interface EffectInterface {
    /** The bending type this effect belongs to. */
    val type: BendingTypeEnum

    /**
     * Spawns the particle effect at the given location.
     *
     * @param location The location to spawn the effect at.
     */
    fun spawn(location: Location)
}
