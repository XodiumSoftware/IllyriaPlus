package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * WaterManipulation: whip a fast bolt of water in a straight line ahead of the caster.
 *
 * The classic waterbending strike: a spinning stream of water rockets forward from the
 * player's eye, dealing damage and knocking back the first living entity it connects
 * with. Fires along the bolt's path are extinguished and the impact site puffs with
 * rain particles.
 *
 * Damage, knockback, and range scale with level.
 */
internal object WaterManipulation : AbilityInterface {
    override val cooldownMs: Long = 1200
    override val activationType = ActivationType.CHARGE

    /** Bolt travel range in blocks (before level scaling). */
    private const val RANGE = 12.0

    /** Additional range in blocks per level. */
    private const val RANGE_PER_LEVEL = 2.0

    /** Damage in hearts dealt to the struck entity (before level scaling). */
    private const val DAMAGE = 1.5

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Horizontal knockback strength (before level scaling). */
    private const val KNOCKBACK = 0.7

    /** Additional knockback per level. */
    private const val KNOCKBACK_PER_LEVEL = 0.1

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.2

    /** Particle density step along the bolt. */
    private const val STEP = 0.35

    /** Collision radius (half-extent) used to detect entity hits each sample step. */
    private const val HIT_RADIUS = 0.7

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

        world.playSound(origin, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.8f, 1.4f)

        val impact = trace(player, origin, direction, range, damage, knockback)
        render(origin, direction, impact ?: origin.clone().add(direction.clone().multiply(range)))
        if (impact == null) {
            wetTerminal(origin, direction, range)
        }

        complete(player)
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
                world.playSound(point, Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.2f)
                return point
            }
            val block = point.block
            if (!block.isPassable && !block.isLiquid && !block.type.isAir) return null
            d += STEP
        }
        return null
    }

    /**
     * Renders the spinning bolt of water particles between two points.
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
        for (i in 0..steps) {
            val base = origin.clone().add(direction.clone().multiply(t))
            twist += 0.4
            val ox = cos(twist) * 0.12
            val oz = sin(twist) * 0.12
            world.spawnParticle(Particle.SPLASH, base.clone().add(ox, 0.0, oz), 3, 0.05, 0.05, 0.05, 0.0)
            world.spawnParticle(Particle.BUBBLE, base.clone().add(ox, 0.0, oz), 1, 0.0, 0.0, 0.0, 0.0)
            t += STEP
            if (t > totalDist) break
        }
    }

    /**
     * Snuffs out fires and emits a small rain puff at the bolt's natural endpoint when
     * it never strikes an entity.
     */
    private fun wetTerminal(
        origin: Location,
        direction: Vector,
        range: Double,
    ) {
        val world = origin.world ?: return
        val end = origin.clone().add(direction.clone().multiply(range))
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    val block = world.getBlockAt(end.blockX + x, end.blockY + y, end.blockZ + z)
                    if (block.type == Material.FIRE) block.type = Material.AIR
                }
            }
        }
        world.spawnParticle(Particle.RAIN, end, 25, 0.6, 0.6, 0.6, 0.0)
        world.playSound(end, Sound.WEATHER_RAIN, 0.5f, 1.3f)
    }
}
