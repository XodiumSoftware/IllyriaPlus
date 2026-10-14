package org.xodium.illyriacore.projectiles

import org.bukkit.entity.Player
import org.xodium.illyriacore.enums.BendingTypeEnum

/** Represents a contract for a bending projectile within the system. */
internal interface ProjectileInterface {
    /** The bending type this projectile belongs to. */
    val type: BendingTypeEnum

    /**
     * Launches the projectile from the given player, scaled by level.
     *
     * @param player The player casting the projectile.
     * @param level The projectile level, in `1..5`. Values outside the range are clamped.
     */
    fun launch(
        player: Player,
        level: Int,
    )
}
