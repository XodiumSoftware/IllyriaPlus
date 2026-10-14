package org.xodium.illyriacore.projectiles

import org.bukkit.entity.Player
import org.bukkit.entity.WindCharge
import org.bukkit.util.Vector
import org.xodium.illyriacore.enums.BendingTypeEnum
import org.xodium.illyriacore.pdcs.ProjectilePDC.tagDamage

/** Air-bending projectile: launches [WindCharge] gusts, more at higher levels. */
internal object AirProjectile : ProjectileInterface {
    override val type = BendingTypeEnum.AIR

    /** Number of gusts launched per level, index `level - 1`. */
    private val COUNT_BY_LEVEL = intArrayOf(1, 1, 2, 2, 3)

    /** Direct damage dealt per gust on impact, indexed by `level - 1`. */
    private val DAMAGE_BY_LEVEL = floatArrayOf(1.0f, 2.0f, 2.0f, 3.0f, 4.0f)

    /** Random horizontal spread applied to each gust beyond the first. */
    private const val SPREAD = 0.15

    override fun launch(
        player: Player,
        level: Int,
    ) {
        val index = level.coerceIn(1, COUNT_BY_LEVEL.size) - 1
        val count = COUNT_BY_LEVEL[index]
        val direction = player.location.direction
        repeat(count) { i ->
            val gust = player.launchProjectile(WindCharge::class.java)
            gust.tagDamage(DAMAGE_BY_LEVEL[index])
            if (i == 0) return@repeat
            gust.velocity =
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
