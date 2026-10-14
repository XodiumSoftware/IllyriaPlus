package org.xodium.illyriacore.projectiles

import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.util.Vector
import org.xodium.illyriacore.enums.BendingTypeEnum

/** Fire-bending projectile: launches [SmallFireball]s, more at higher levels. */
internal object FireProjectile : ProjectileInterface {
    override val type = BendingTypeEnum.FIRE

    /** Number of fireballs launched per level, index `level - 1`. */
    private val COUNT_BY_LEVEL = intArrayOf(1, 2, 2, 3, 3)

    /** Random spread applied to each fireball beyond the first. */
    private const val SPREAD = 0.2

    override fun launch(
        player: Player,
        level: Int,
    ) {
        val count = COUNT_BY_LEVEL[level.coerceIn(1, COUNT_BY_LEVEL.size) - 1]
        val direction = player.location.direction
        repeat(count) { index ->
            val fireball = player.launchProjectile(SmallFireball::class.java)
            if (index == 0) return@repeat
            fireball.velocity =
                direction
                    .clone()
                    .add(
                        Vector(
                            (Math.random() - 0.5) * SPREAD,
                            (Math.random() - 0.5) * SPREAD,
                            (Math.random() - 0.5) * SPREAD,
                        ),
                    )
        }
    }
}
