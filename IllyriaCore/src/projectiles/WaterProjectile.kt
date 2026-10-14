package org.xodium.illyriacore.projectiles

import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.util.Vector
import org.xodium.illyriacore.enums.BendingTypeEnum

/** Water-bending projectile: launches [Snowball]s, more at higher levels. */
internal object WaterProjectile : ProjectileInterface {
    override val type = BendingTypeEnum.WATER

    /** Number of snowballs launched per level, index `level - 1`. */
    private val COUNT_BY_LEVEL = intArrayOf(1, 2, 3, 4, 5)

    /** Random spread applied to each snowball beyond the first. */
    private const val SPREAD = 0.25

    override fun launch(
        player: Player,
        level: Int,
    ) {
        val count = COUNT_BY_LEVEL[level.coerceIn(1, COUNT_BY_LEVEL.size) - 1]
        val direction = player.location.direction
        repeat(count) { index ->
            val snowball = player.launchProjectile(Snowball::class.java)
            if (index == 0) return@repeat
            snowball.velocity =
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
