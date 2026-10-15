package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Location
import org.bukkit.Material
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
 * AirBlast: emit a short-ranged cone of compressed air that shoves enemies and extinguishes fires.
 *
 * On activation, the player exhales a violent gust directly ahead. Living entities caught in
 * the cone take light damage and are hurled backwards (and slightly upward). Fires in the
 * blast's path are snuffed out.
 *
 * The cone widens as level increases, and knockback scales with level.
 */
internal object AirBlast : AbilityInterface {
    override val cooldownMs: Long = 3000

    /** Cone reach in blocks. */
    private const val RANGE = 8.0

    /** Cone half-angle in degrees (widens with level). */
    private const val CONE_ANGLE_DEG = 30.0

    /** Damage in hearts dealt to caught entities. */
    private const val DAMAGE = 1.0

    /** Horizontal knockback strength (scales with level). */
    private const val KNOCKBACK = 1.4

    /** Vertical knockback component. */
    private const val KNOCKUP = 0.35

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
        val knockScale = 1.0 + (level - 1) * 0.25

        render(origin, direction, angleRad)
        world.playSound(origin, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.0f)

        extinguish(origin, direction, angleRad)
        knockback(player, origin, direction, angleRad, knockScale)
    }

    /**
     * Renders a cone of air particles emanating from [origin] along [direction].
     *
     * @param origin The starting point of the blast (usually the caster's eye location).
     * @param direction The normalized travel direction of the blast.
     * @param angleRad The cone half-angle in radians.
     */
    private fun render(
        origin: Location,
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
                    Particle.CLOUD,
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
     * Extinguishes fire blocks inside the cone.
     *
     * @param origin The starting point of the blast.
     * @param direction The normalized travel direction of the blast.
     * @param angleRad The cone half-angle in radians.
     */
    private fun extinguish(
        origin: Location,
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
                        if (block.type == Material.FIRE) block.type = Material.AIR
                    }
                }
            }
        }
    }

    /**
     * Damages and shoves living entities caught in the cone away from the player.
     *
     * @param player The casting player; used as the damage source.
     * @param origin The starting point of the blast.
     * @param direction The normalized travel direction of the blast.
     * @param angleRad The cone half-angle in radians.
     * @param knockScale Multiplier applied to [KNOCKBACK].
     */
    private fun knockback(
        player: Player,
        origin: Location,
        direction: Vector,
        angleRad: Double,
        knockScale: Double,
    ) {
        val world = origin.world ?: return
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
                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .setY(KNOCKUP)
                        .multiply(KNOCKBACK * knockScale)
                entity.velocity = entity.velocity.add(push)
            }
    }
}
