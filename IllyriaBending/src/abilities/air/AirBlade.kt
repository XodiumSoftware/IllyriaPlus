package org.xodium.illyriabending.abilities.air

import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * AirBlade: charge a spinning ring of compressed air, then launch it forward.
 *
 * Hold sneak to charge — a growing ring of air particles appears in front of the player.
 * Release sneak to launch the blade. Fully charging multiplies damage, range, radius, and
 * speed by [CHARGE_FACTOR].
 *
 * The blade travels forward as a flat disc, stepping over one-block obstacles and
 * extinguishing fires it passes through. Entities caught in its path take damage and are
 * knocked back and upward.
 */
internal object AirBlade : AbilityInterface {
    override val cooldownMs: Long = 4000

    /** Blade ring radius in blocks (before charge factor). */
    private const val RADIUS = 1.2

    /** Damage in hearts dealt by the blade (before charge factor). */
    private const val DAMAGE = 1.5

    /** Horizontal knockback strength applied to hit entities. */
    private const val KNOCKBACK = 0.8

    /** Vertical knockback component applied to hit entities. */
    private const val KNOCKUP = 0.15

    /** Maximum travel distance in blocks (before charge factor). */
    private const val RANGE = 12.0

    /** How many blocks the blade advances every tick (before charge factor). */
    private const val SPEED = 0.8

    /** How many milliseconds it takes to fully charge. */
    private const val MAX_CHARGE_TIME_MS = 2000L

    /** Damage and range multiplier at full charge. Radius and speed are only halved-affected. */
    private const val CHARGE_FACTOR = 3.0

    /** Ticks between particle renders. */
    private const val TICK_PERIOD = 1L

    /** Current angle for the rotating ring animation, incremented each tick. */
    private var chargingPoint = 0.0

    /** Active casting sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * Holds state for a single AirBlade activation: either charging or launched.
     */
    private class Session(
        val player: Player,
        val direction: Vector,
        val origin: Location,
        var factor: Double = 1.0,
        var charging: Boolean = true,
        val startTime: Long = System.currentTimeMillis(),
        var blade: Blade? = null,
        var task: BukkitTask? = null,
    ) {
        fun cancel() {
            task?.cancel()
        }
    }

    /**
     * The launched blade projectile: a flat disc that travels forward, rendering particles
     * and damaging entities in its path.
     */
    private class Blade(
        val player: Player,
        origin: Location,
        direction: Vector,
        val radius: Double,
        speed: Double,
        val factor: Double,
    ) {
        /** Current center location of the blade. */
        var location: Location = origin.clone()

        /** Normalized travel direction. */
        val dir: Vector = direction.clone().normalize()

        /** Per-tick movement vector. */
        val velocity: Vector = dir.clone().multiply(speed)

        /** Maximum ticks before the blade expires, derived from effective range. */
        val maxTicks: Int = ceil(RANGE * factor / speed).toInt()

        var ticksAlive = 0
        var rotationAngle = 0.0

        /**
         * Advances the blade one tick. Returns `false` when the blade should be removed.
         */
        fun update(): Boolean {
            if (++ticksAlive > maxTicks) return false

            location.add(velocity)

            val block = location.block
            if (!block.isPassable && !block.type.isAir) return false

            val below = location.clone().subtract(0.0, radius + 0.25, 0.0).block
            if (below.type == org.bukkit.Material.FIRE) below.type = org.bukkit.Material.AIR

            if (block.isLiquid) return false

            render()
            damageNearby()
            return true
        }

        /** Renders the rotating ring of air particles around the blade. */
        private fun render() {
            rotationAngle += PI / 36
            val count = ceil(22.0 * radius).toInt()
            val axisY = Vector(0, 1, 0)
            val rotAxis = axisY.clone().crossProduct(dir).normalize()

            for (i in 0 until count) {
                val angle = rotationAngle + i * (2.0 * PI / count)
                val offset = rotate(dir.clone().multiply(radius), rotAxis, angle)
                val point = location.clone().add(offset)
                location.world?.spawnParticle(Particle.CLOUD, point, 1, 0.0, 0.0, 0.0, 0.0)
            }

            if (Math.random() < 1.0 / 8.0) {
                location.world?.playSound(location, Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.0f)
            }
        }

        /** Damages and knocks back living entities overlapping the blade disc. */
        private fun damageNearby() {
            location
                .world
                ?.getNearbyEntities(location, radius, radius, radius)
                ?.filterIsInstance<LivingEntity>()
                ?.filter { it != player }
                ?.forEach { entity ->
                    entity.damage(DAMAGE * factor, player)
                    val knockDir =
                        dir
                            .clone()
                            .setY(KNOCKUP)
                            .normalize()
                            .multiply(KNOCKBACK)
                    entity.velocity = entity.velocity.add(knockDir)
                }
        }
    }

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (sessions.containsKey(player.uniqueId)) return

        val direction =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val maxRadius = RADIUS * CHARGE_FACTOR * 0.5
        val origin =
            player
                .location
                .clone()
                .add(direction.clone())
                .add(0.0, maxRadius, 0.0)

        val session = Session(player, direction, origin)
        sessions[player.uniqueId] = session

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(player.uniqueId) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a session by one tick. Called every [TICK_PERIOD] ticks. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        if (!player.isOnline || player.isDead) {
            remove(uuid)
            return
        }

        if (session.charging) {
            tickCharging(session)
        } else {
            tickBlade(session)
        }
    }

    /** Ticks while the blade is being charged. */
    private fun tickCharging(session: Session) {
        val player = session.player
        val origin = session.origin

        if (origin.block.isLiquid) {
            remove(player.uniqueId)
            return
        }

        val elapsed = System.currentTimeMillis() - session.startTime

        if (player.isSneaking && elapsed > 100) {
            val timeFactor = minOf(0.9, elapsed.toDouble() / MAX_CHARGE_TIME_MS)
            val r = RADIUS * CHARGE_FACTOR * timeFactor * 0.5
            val count = ceil(r * 10).toInt()
            val axisY = Vector(0, 1, 0)
            val rotAxis = axisY.clone().crossProduct(session.direction).normalize()

            chargingPoint += PI / 36
            for (i in 0 until count) {
                val angle = chargingPoint + i * (2.0 * PI / count)
                val offset = rotate(session.direction.clone().multiply(r), rotAxis, angle)
                origin.world?.spawnParticle(Particle.CLOUD, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            }

            var d = 0.1
            while (d < r) {
                val beamOffset = session.direction.clone().multiply(d)
                val rotated = rotate(beamOffset, rotAxis, chargingPoint)
                origin.world?.spawnParticle(Particle.CLOUD, origin.clone().add(rotated), 1, 0.0, 0.0, 0.0, 0.0)
                d += 0.25
            }

            if (Math.random() < 1.0 / 8.0) {
                origin.world?.playSound(origin, Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.0f)
            }
        } else if (!player.isSneaking) {
            launch(session)
        }
    }

    /** Ticks while the blade is in flight. */
    private fun tickBlade(session: Session) {
        val blade =
            session.blade ?: run {
                remove(session.player.uniqueId)
                return
            }

        if (!blade.update()) {
            remove(session.player.uniqueId)
        }
    }

    /** Launches the blade from a charging session. */
    private fun launch(session: Session) {
        val elapsed = System.currentTimeMillis() - session.startTime
        session.factor =
            when {
                elapsed >= MAX_CHARGE_TIME_MS -> {
                    CHARGE_FACTOR
                }

                elapsed > 0.3 * MAX_CHARGE_TIME_MS -> {
                    1.0 + (CHARGE_FACTOR - 1.0) * elapsed.toDouble() / MAX_CHARGE_TIME_MS
                }

                else -> {
                    1.0
                }
            }

        session.charging = false
        session.blade =
            Blade(
                session.player,
                session.origin,
                session.direction,
                RADIUS * session.factor * 0.5,
                SPEED * session.factor * 0.5,
                session.factor,
            )
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.cancel()
    }

    /**
     * Rotates [vec] around [axis] by [angle] radians using Rodrigues' rotation formula.
     *
     * @param vec The vector to rotate.
     * @param axis The rotation axis; will be normalized before use.
     * @param angle The rotation angle in radians.
     * @return The rotated vector.
     */
    private fun rotate(
        vec: Vector,
        axis: Vector,
        angle: Double,
    ): Vector {
        val k = axis.clone().normalize()
        val cosA = cos(angle)
        val sinA = sin(angle)
        val term1 = vec.clone().multiply(cosA)
        val term2 = k.clone().crossProduct(vec).multiply(sinA)
        val term3 = k.clone().multiply(k.dot(vec) * (1.0 - cosA))
        return term1.add(term2).add(term3)
    }
}
