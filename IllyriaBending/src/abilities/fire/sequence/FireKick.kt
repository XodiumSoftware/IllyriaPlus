package org.xodium.illyriabending.abilities.fire.sequence

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * FireKick: a fast, close-ranged kick that releases a sharp burst of flame.
 *
 * The player snaps a foot forward, lashing out with flame at the area immediately in
 * front. Enemies in the small cone are damaged, ignited, and shoved backwards.
 *
 * Low cooldown, low damage — a bread-and-butter close-combat ability.
 * Cone width and damage scale with level.
 */
internal object FireKick : AbilityInterface {
    override val cooldownMs: Long = 1500
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.INSTANT

    /** Cone reach in blocks. */
    private const val RANGE = 3.5

    /** Additional reach per level. */
    private const val RANGE_PER_LEVEL = 0.4

    /** Cone half-angle in degrees. */
    private const val CONE_ANGLE_DEG = 40.0

    /** Damage in hearts dealt to caught entities (before level scaling). */
    private const val DAMAGE = 0.75

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.15

    /** Ticks of fire applied to caught entities. */
    private const val FIRE_TICKS = 40

    /** Horizontal knockback strength. */
    private const val KNOCKBACK = 0.9

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.3

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val origin = player.eyeLocation.clone().add(0.0, -0.5, 0.0)
        val direction = origin.direction.clone().normalize()
        val world = origin.world ?: return

        val range = RANGE + (level - 1) * RANGE_PER_LEVEL
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL
        val angleRad = Math.toRadians(CONE_ANGLE_DEG)

        render(origin, direction, angleRad, range)
        damage(player, origin, direction, angleRad, range, damage)

        world.playSound(origin, Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.4f)

        complete(player)
    }

    /**
     * Renders a quick cone of flame particles.
     */
    private fun render(
        origin: Location,
        direction: Vector,
        angleRad: Double,
        range: Double,
    ) {
        val world = origin.world ?: return
        val axisY = Vector(0, 1, 0)
        val side = axisY.clone().crossProduct(direction).normalize()
        val up = direction.clone().crossProduct(side).normalize()

        val rays = 12
        for (i in 0 until rays) {
            val ringAngle = i * (2.0 * PI / rays)
            val coneDir =
                direction
                    .clone()
                    .multiply(cos(angleRad * Math.random() * 0.5))
                    .add(side.clone().multiply(sin(angleRad * Math.random()) * cos(ringAngle) * 0.4))
                    .add(up.clone().multiply(sin(angleRad * Math.random()) * sin(ringAngle) * 0.4))
                    .normalize()

            var d = 0.4
            while (d <= range) {
                world.spawnParticle(
                    Particle.FLAME,
                    origin.clone().add(coneDir.clone().multiply(d)),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                )
                d += 0.4
            }
        }
    }

    /**
     * Damages and shoves living entities caught in the cone.
     */
    private fun damage(
        player: Player,
        origin: Location,
        direction: Vector,
        angleRad: Double,
        range: Double,
        damage: Double,
    ) {
        val world = origin.world ?: return
        world
            .getNearbyEntities(origin, range, range, range)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { entity ->
                val toEntity = entity.eyeLocation.toVector().subtract(origin.toVector())
                val dist = toEntity.length()
                if (dist !in 0.3..range) return@filter false
                val cosAngle = toEntity.normalize().dot(direction)
                cosAngle >= cos(angleRad)
            }.forEach { entity ->
                entity.damage(damage, player)
                entity.fireTicks = maxOf(entity.fireTicks, FIRE_TICKS)
                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .setY(KNOCKUP)
                        .multiply(KNOCKBACK)
                entity.velocity = entity.velocity.add(push)
            }
    }
}
