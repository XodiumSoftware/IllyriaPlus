package org.xodium.illyriabending.projectiles

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.enums.BendingTypeEnum
import java.util.UUID

/** Earth-bending projectile: lobs a falling-block boulder that grows faster and hits harder per level. */
internal object EarthProjectile : ProjectileInterface {
    override val type = BendingTypeEnum.EARTH

    /** [NamespacedKey] used to tag the boulder with its launcher's UUID for kill attribution. */
    private val SHOOTER_KEY = NamespacedKey(instance, "earth_boulder_shooter")

    /** The material the boulder is made of. */
    private val BOULDER_MATERIAL: Material = Material.MUD

    /** Forward speed applied to the boulder. */
    private const val SPEED = 1.5

    /** Upward bias applied to the boulder so it arcs. */
    private const val LIFT = 0.3

    /** Damage dealt per fallen block, indexed by `level - 1`. */
    private val DAMAGE_PER_BLOCK_BY_LEVEL = floatArrayOf(2.0f, 3.0f, 4.0f, 5.0f, 6.0f)

    /** Maximum damage a boulder can deal to a single entity, indexed by `level - 1`. */
    private val MAX_DAMAGE_BY_LEVEL = intArrayOf(6, 8, 10, 12, 15)

    /**
     * Tags this boulder with the given player's UUID so damage events can attribute kills
     * back to the launcher.
     */
    private fun FallingBlock.tagShooter(player: Player) {
        persistentDataContainer.set(
            SHOOTER_KEY,
            PersistentDataType.STRING,
            player.uniqueId.toString(),
        )
    }

    /**
     * Resolves the tagged shooter UUID on this boulder, or `null` if untagged.
     */
    fun FallingBlock.shooterUuid(): UUID? =
        persistentDataContainer
            .get(SHOOTER_KEY, PersistentDataType.STRING)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    override fun launch(
        player: Player,
        level: Int,
    ) {
        val index = level.coerceIn(1, DAMAGE_PER_BLOCK_BY_LEVEL.size) - 1
        val direction = player.location.direction
        player.world.spawn(player.eyeLocation, FallingBlock::class.java) { boulder ->
            boulder.blockData = BOULDER_MATERIAL.createBlockData()
            boulder.velocity = direction.multiply(SPEED).setY(direction.y + LIFT)
            boulder.setHurtEntities(true)
            boulder.damagePerBlock = DAMAGE_PER_BLOCK_BY_LEVEL[index]
            boulder.maxDamage = MAX_DAMAGE_BY_LEVEL[index]
            boulder.dropItem = false
            boulder.cancelDrop = true
            boulder.tagShooter(player)
        }
    }
}
