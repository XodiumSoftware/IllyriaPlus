package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * AirSwipe: sweep a wide horizontal crescent of compressed air in front of the caster.
 *
 * On activation, a fan-shaped slash of air particles arcs across the player's facing.
 * Living entities caught in the crescent take damage and are knocked away from the
 * caster along the arc direction. The swipe extends roughly perpendicular to the
 * player's look direction.
 *
 * The arc width and range scale with level.
 */
internal object AirSwipe : AbilityInterface {
    override val cooldownMs: Long = 2500

    /** Maximum reach of the swipe in blocks. */
    private const val RANGE = 4.5

    /** Additional range per level. */
    private const val RANGE_PER_LEVEL = 0.5

    /** Total arc of the swipe in degrees at level 1. */
    private const val ARC_DEG = 120.0

    /** Additional arc degrees per level. */
    private const val ARC_DEG_PER_LEVEL = 10.0

    /** Damage in hearts dealt to caught entities. */
    private const val DAMAGE = 1.5

    /** Horizontal knockback strength (scales with level). */
    private const val KNOCKBACK = 1.2

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.25

    /** Number of particle rays spread across the arc. */
    private const val RAYS = 24

    /** Particle density step along each ray. */
    private const val STEP = 0.35

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val origin = player.eyeLocation.clone().subtract(0.0, 0.4, 0.0)
        val world = origin.world ?: return

        val forward =
            origin
                .direction
                .clone()
                .setY(0)
                .normalize()
        val range = RANGE + (level - 1) * RANGE_PER_LEVEL
        val arcRad = Math.toRadians(ARC_DEG + (level - 1) * ARC_DEG_PER_LEVEL)
        val knockScale = 1.0 + (level - 1) * 0.2

        render(origin, forward, range, arcRad)
        world.playSound(origin, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.3f)

        strike(player, origin, forward, range, arcRad, knockScale)
    }

    /**
     * Renders a horizontal crescent of air particles spreading from [origin].
     *
     * @param origin The starting point of the swipe (roughly chest height).
     * @param forward The caster's horizontal facing direction.
     * @param range The reach of the swipe in blocks.
     * @param arcRad The total arc angle in radians.
     */
    private fun render(
        origin: Location,
        forward: Vector,
        range: Double,
        arcRad: Double,
    ) {
        val world = origin.world ?: return
        val baseYaw = atan2(-forward.x, forward.z)

        for (i in 0 until RAYS) {
            val offset = (i.toDouble() / (RAYS - 1) - 0.5) * arcRad
            val yaw = baseYaw + offset
            val dir = Vector(-sin(yaw), 0.0, cos(yaw))

            var d = STEP
            while (d <= range) {
                val point =
                    origin
                        .clone()
                        .add(dir.clone().multiply(d))
                        .add(0.0, sin(d / range * PI) * 0.2, 0.0)
                world.spawnParticle(Particle.CLOUD, point, 1, 0.0, 0.0, 0.0, 0.0)
                d += STEP
            }
        }
    }

    /**
     * Damages and knocks back living entities caught inside the crescent.
     *
     * @param player The casting player; used as the damage source.
     * @param origin The starting point of the swipe.
     * @param forward The caster's horizontal facing direction.
     * @param range The reach of the swipe in blocks.
     * @param arcRad The total arc angle in radians.
     * @param knockScale Multiplier applied to [KNOCKBACK].
     */
    private fun strike(
        player: Player,
        origin: Location,
        forward: Vector,
        range: Double,
        arcRad: Double,
        knockScale: Double,
    ) {
        val world = origin.world ?: return

        world
            .getNearbyEntities(origin, range, range, range)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { entity ->
                val toEntity =
                    entity
                        .location
                        .toVector()
                        .subtract(origin.toVector())
                        .setY(0.0)
                val dist = toEntity.length()
                if (dist !in 0.5..range) return@filter false
                val cosAngle = toEntity.normalize().dot(forward)
                cosAngle >= cos(arcRad / 2.0)
            }.forEach { entity ->
                entity.damage(DAMAGE, player)
                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(origin.toVector())
                        .setY(0.0)
                        .normalize()
                        .setY(KNOCKUP)
                        .multiply(KNOCKBACK * knockScale)
                entity.velocity = entity.velocity.add(push)
            }
    }
}
