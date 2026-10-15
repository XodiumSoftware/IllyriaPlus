package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * AirBreath: exhale a sustained cone of compressed air while sneaking.
 *
 * The cone pushes entities away, extinguishes fire, and briefly grants water breathing
 * to the caster. The stream persists until the player stops sneaking or the maximum
 * duration expires.
 *
 * The cone widens as level increases, and knockback scales with level.
 */
internal object AirBreath : AbilityInterface {
    override val cooldownMs: Long = 5000

    /** Cone reach in blocks. */
    private const val RANGE = 6.0

    /** Cone half-angle in degrees (widens with level). */
    private const val CONE_ANGLE_DEG = 25.0

    /** Horizontal knockback strength (scales with level). */
    private const val PUSH = 0.6

    /** Vertical knockback component. */
    private const val LIFT = 0.15

    /** Maximum duration of a breath in ticks. */
    private const val MAX_DURATION_TICKS = 100L

    /** Ticks between breath pulses. */
    private const val TICK_PERIOD = 2L

    /** Water-breathing effect duration in ticks. */
    private const val WATER_BREATHING_TICKS = 60

    /** Particle density step along each ray. */
    private const val STEP = 0.4

    /** Active casting sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, BukkitTask>()

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (sessions.containsKey(player.uniqueId)) return

        val angleRad = Math.toRadians(CONE_ANGLE_DEG + (level - 1) * 5.0)
        val pushScale = 1.0 + (level - 1) * 0.25

        player.addPotionEffect(
            PotionEffect(PotionEffectType.WATER_BREATHING, WATER_BREATHING_TICKS, 0, false, false),
        )

        var ticks = 0L
        sessions[player.uniqueId] =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable {
                    ticks += TICK_PERIOD
                    if (!player.isOnline || player.isDead || !player.isSneaking || ticks > MAX_DURATION_TICKS) {
                        remove(player.uniqueId)
                        return@Runnable
                    }
                    breathe(player, angleRad, pushScale)
                },
                0L,
                TICK_PERIOD,
            )
    }

    /** Removes an active session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.cancel()
    }

    /**
     * Emits a single breath pulse: renders particles, extinguishes fires, and pushes entities.
     *
     * @param player The casting player.
     * @param angleRad The cone half-angle in radians.
     * @param pushScale Multiplier applied to [PUSH].
     */
    private fun breathe(
        player: Player,
        angleRad: Double,
        pushScale: Double,
    ) {
        val origin = player.eyeLocation.clone()
        val direction = origin.direction.clone().normalize()
        val world = origin.world ?: return

        world.playSound(origin, Sound.ENTITY_BREEZE_IDLE_GROUND, 0.4f, 1.2f)

        render(origin, direction, angleRad)
        extinguish(origin, direction, angleRad)
        push(player, origin, direction, angleRad, pushScale)
    }

    /**
     * Renders a cone of air particles emanating from [origin] along [direction].
     *
     * @param origin The starting point of the breath (caster's eye location).
     * @param direction The normalized travel direction.
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

        val rays = 12
        for (i in 0 until rays) {
            val ringAngle = i * (2.0 * PI / rays)
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
                    0.05,
                )
                d += STEP
            }
        }
    }

    /**
     * Extinguishes fire blocks inside the cone.
     *
     * @param origin The starting point of the breath.
     * @param direction The normalized travel direction.
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
     * Pushes living entities caught in the cone away from the player.
     *
     * @param player The casting player; used as the push source.
     * @param origin The starting point of the breath.
     * @param direction The normalized travel direction.
     * @param angleRad The cone half-angle in radians.
     * @param pushScale Multiplier applied to [PUSH].
     */
    private fun push(
        player: Player,
        origin: Location,
        direction: Vector,
        angleRad: Double,
        pushScale: Double,
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
                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .setY(LIFT)
                        .multiply(PUSH * pushScale)
                entity.velocity = entity.velocity.add(push)
            }
    }
}
