package org.xodium.illyriabending.projectiles

import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.util.Vector
import org.xodium.illyriabending.enums.BendingTypeEnum
import org.xodium.illyriabending.pdcs.ProjectilePDC.tagDamage

/** Water-bending projectile: launches [Snowball]s, more at higher levels. */
internal object WaterProjectile : ProjectileInterface {
    override val type = BendingTypeEnum.WATER

    /** Number of snowballs launched per level, index `level - 1`. */
    private val COUNT_BY_LEVEL = intArrayOf(1, 2, 3, 4, 5)

    /** Direct damage dealt per snowball on impact, indexed by `level - 1`. */
    private val DAMAGE_BY_LEVEL = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)

    /** Random spread applied to each snowball beyond the first. */
    private const val SPREAD = 0.25

    override fun launch(
        player: Player,
        level: Int,
    ) {
        val index = level.coerceIn(1, COUNT_BY_LEVEL.size) - 1
        val count = COUNT_BY_LEVEL[index]
        val direction = player.location.direction
        repeat(count) { i ->
            val snowball = player.launchProjectile(Snowball::class.java)
            snowball.tagDamage(DAMAGE_BY_LEVEL[index])
            if (i == 0) return@repeat
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
