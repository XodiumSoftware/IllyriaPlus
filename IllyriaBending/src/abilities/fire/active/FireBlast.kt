package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * FireBlast: emit a short-ranged cone of flame that scorches enemies and ignites terrain.
 *
 * On activation, the player exhales a violent cone of flame directly ahead. Living
 * entities caught in the cone take fire damage, are set aflame, and are shoved slightly
 * backwards. The cone widens as level increases.
 *
 * FireBlast also ignites flammable blocks in its path.
 */
internal object FireBlast : AbilityInterface {
    override val cooldownMs: Long = 3200

    /** Cone reach in blocks. */
    private const val RANGE = 9.0

    /** Cone half-angle in degrees (widens with level). */
    private const val CONE_ANGLE_DEG = 35.0

    /** Damage in hearts dealt to caught entities. */
    private const val DAMAGE = 1.5

    /** Ticks of fire applied to caught entities. */
    private const val FIRE_TICKS = 80

    /** Horizontal knockback strength. */
    private const val KNOCKBACK = 0.7

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.15

    /** Particle density step along each ray. */
    private const val STEP = 0.5

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val origin = player.eyeLocation.clone()
        val direction = origin.direction.clone().normalize()
        val world = origin.world ?: return

        val angleRad = Math.toRadians(CONE_ANGLE_DEG + (level - 1) * 5.0)

        render(origin, direction, angleRad)
        world.playSound(origin, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f)

        burn(player, origin, direction, angleRad)
    }

    /**
     * Renders a cone of flame particles emanating from [origin] along [direction].
     *
     * @param origin The starting point of the blast.
     * @param direction The normalized travel direction of the blast.
     * @param angleRad The cone half-angle in radians.
     */
    private fun render(
        origin: org.bukkit.Location,
        direction: Vector,
        angleRad: Double,
    ) {
        val world = origin.world ?: return
        val axisY = Vector(0, 1, 0)
        val side = axisY.clone().crossProduct(direction).normalize()
        val up = direction.clone().crossProduct(side).normalize()

        val rays = 24
        for (i in 0 until rays) {
            val ringAngle = i * (2.0 * Math.PI / rays)
            val coneDir =
                direction
                    .clone()
                    .multiply(cos(angleRad * Math.random() * 0.5))
                    .add(side.clone().multiply(sin(angleRad * Math.random()) * cos(ringAngle) * 0.5))
                    .add(up.clone().multiply(sin(angleRad * Math.random()) * sin(ringAngle) * 0.5))
                    .normalize()

            var d = STEP
            while (d <= RANGE) {
                world.spawnParticle(
                    Particle.FLAME,
                    origin.clone().add(coneDir.clone().multiply(d)),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                )
                d += STEP
            }
        }
    }

    /**
     * Burns and knocks back living entities caught in the cone, and ignites flammable blocks.
     *
     * @param player The casting player; used as the damage source.
     * @param origin The starting point of the blast.
     * @param direction The normalized travel direction of the blast.
     * @param angleRad The cone half-angle in radians.
     */
    private fun burn(
        player: Player,
        origin: org.bukkit.Location,
        direction: Vector,
        angleRad: Double,
    ) {
        val world = origin.world ?: return

        for (i in 1..RANGE.toInt()) {
            val center = origin.clone().add(direction.clone().multiply(i.toDouble()))
            val radius = i * tan(angleRad)
            for (x in -radius.toInt()..radius.toInt()) {
                for (y in -radius.toInt()..radius.toInt()) {
                    for (z in -radius.toInt()..radius.toInt()) {
                        val block = world.getBlockAt(center.blockX + x, center.blockY + y, center.blockZ + z)
                        if (block.type.isFlammable && Math.random() < 0.1) {
                            block.type = org.bukkit.Material.FIRE
                        }
                    }
                }
            }
        }

        world
            .getNearbyEntities(origin, RANGE, RANGE, RANGE)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { entity ->
                val toEntity = entity.eyeLocation.toVector().subtract(origin.toVector())
                val dist = toEntity.length()
                if (dist !in 0.5..RANGE) return@filter false
                val cosAngle = toEntity.normalize().dot(direction)
                cosAngle >= cos(angleRad)
            }.forEach { entity ->
                entity.damage(DAMAGE, player)
                entity.fireTicks = FIRE_TICKS
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
