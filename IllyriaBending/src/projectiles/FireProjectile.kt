package org.xodium.illyriabending.projectiles

import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.util.Vector
import org.xodium.illyriabending.enums.BendingTypeEnum
import org.xodium.illyriabending.pdcs.ProjectilePDC.tagDamage

/** Fire-bending projectile: launches [SmallFireball]s, more at higher levels. */
internal object FireProjectile : ProjectileInterface {
    override val type = BendingTypeEnum.FIRE

    /** Number of fireballs launched per level, index `level - 1`. */
    private val COUNT_BY_LEVEL = intArrayOf(1, 2, 2, 3, 3)

    /** Direct damage dealt per fireball on impact, indexed by `level - 1`. */
    private val DAMAGE_BY_LEVEL = floatArrayOf(2.0f, 3.0f, 4.0f, 5.0f, 6.0f)

    /** Random spread applied to each fireball beyond the first. */
    private const val SPREAD = 0.2

    override fun launch(
        player: Player,
        level: Int,
    ) {
        val index = level.coerceIn(1, COUNT_BY_LEVEL.size) - 1
        val count = COUNT_BY_LEVEL[index]
        val direction = player.location.direction
        repeat(count) { i ->
            val fireball = player.launchProjectile(SmallFireball::class.java)
            fireball.tagDamage(DAMAGE_BY_LEVEL[index])
            if (i == 0) return@repeat
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
