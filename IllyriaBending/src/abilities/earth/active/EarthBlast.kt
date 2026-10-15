package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * EarthBlast: rip a chunk of earth from the ground and hurl it forward in the air.
 *
 * The caster gouges a small crater directly below themselves, then launches a compact
 * ball of stone and dirt in a flat trajectory toward their facing direction. The bolt
 * tracks along a straight line, damaging and staggering the first living entity it
 * strikes and spraying earth particles across the impact site.
 *
 * Damage, knockback, and bolt range scale with level.
 */
internal object EarthBlast : AbilityInterface {
    override val cooldownMs: Long = 1400
    override val activationType = ActivationTypeEnum.INSTANT

    /** Bolt travel range in blocks (before level scaling). */
    private const val RANGE = 14.0

    /** Additional range per level. */
    private const val RANGE_PER_LEVEL = 2.0

    /** Damage in hearts dealt to the struck entity (before level scaling). */
    private const val DAMAGE = 1.75

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Horizontal knockback strength (before level scaling). */
    private const val KNOCKBACK = 0.8

    /** Additional knockback per level. */
    private const val KNOCKBACK_PER_LEVEL = 0.12

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.25

    /** Particle density step along the bolt. */
    private const val STEP = 0.4

    /** Collision radius (half-extent) used to detect entity hits each sample step. */
    private const val HIT_RADIUS = 0.8

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val origin = player.eyeLocation.clone()
        val direction = origin.direction.clone().normalize()
        val world = origin.world ?: return

        val range = RANGE + (level - 1) * RANGE_PER_LEVEL
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL
        val knockback = KNOCKBACK + (level - 1) * KNOCKBACK_PER_LEVEL

        gougeCrater(player)
        world.playSound(origin, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.9f)
        world.playSound(origin, Sound.BLOCK_STONE_BREAK, 0.8f, 1.1f)

        val impact = trace(player, origin, direction, range, damage, knockback)
        render(origin, direction, impact ?: origin.clone().add(direction.clone().multiply(range)))
        complete(player)
    }

    /**
     * Puffs earth particles from the ground at the caster's feet and cracks any soft
     * earth blocks within the immediate footprint, simulating ripped-out sod.
     */
    private fun gougeCrater(player: Player) {
        val world = player.world
        val feet = player.location.clone().subtract(0.0, 0.25, 0.0)
        world.spawnParticle(
            Particle.BLOCK,
            feet,
            24,
            0.5,
            0.2,
            0.5,
            0.06,
            Material.DIRT.createBlockData(),
        )
        world.spawnParticle(
            Particle.BLOCK,
            feet,
            12,
            0.4,
            0.15,
            0.4,
            0.05,
            Material.STONE.createBlockData(),
        )
    }

    /**
     * Traces the bolt until it strikes a living entity or a solid block. Returns the
     * impact location if an entity was hit, or null if the bolt expired or hit terrain.
     */
    private fun trace(
        player: Player,
        origin: Location,
        direction: Vector,
        range: Double,
        damage: Double,
        knockback: Double,
    ): Location? {
        val world = origin.world ?: return null
        var d = 0.5
        while (d <= range) {
            val point = origin.clone().add(direction.clone().multiply(d))
            val entity =
                world
                    .getNearbyEntities(point, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)
                    .filterIsInstance<LivingEntity>()
                    .firstOrNull { it != player }
            if (entity != null) {
                entity.damage(damage, player)
                val kb =
                    direction
                        .clone()
                        .setY(KNOCKUP)
                        .normalize()
                        .multiply(knockback)
                entity.velocity = entity.velocity.add(kb)
                world.playSound(point, Sound.BLOCK_STONE_HIT, 1.0f, 0.9f)
                world.spawnParticle(
                    Particle.BLOCK,
                    point,
                    20,
                    0.4,
                    0.4,
                    0.4,
                    0.05,
                    Material.COBBLESTONE.createBlockData(),
                )
                return point
            }
            val block = point.block
            if (!block.isPassable && !block.isLiquid && !block.type.isAir) return null
            d += STEP
        }
        return null
    }

    /**
     * Renders the spinning bolt of packed earth between two points.
     */
    private fun render(
        origin: Location,
        direction: Vector,
        end: Location,
    ) {
        val world = origin.world ?: return
        val totalDist = origin.distance(end)
        val steps = ceil(totalDist / STEP).toInt().coerceAtLeast(1)
        var t = 0.0
        var twist = 0.0
        val stoneData = Material.COBBLESTONE.createBlockData()
        val dirtData = Material.DIRT.createBlockData()
        for (i in 0..steps) {
            val base = origin.clone().add(direction.clone().multiply(t))
            twist += 0.5
            val ox = cos(twist) * 0.15
            val oz = sin(twist) * 0.15
            world.spawnParticle(Particle.BLOCK, base.clone().add(ox, 0.0, oz), 2, 0.05, 0.05, 0.05, 0.0, stoneData)
            world.spawnParticle(Particle.BLOCK, base.clone().add(-ox, 0.0, -oz), 1, 0.05, 0.05, 0.05, 0.0, dirtData)
            t += STEP
            if (t > totalDist) break
        }
    }
}
