package org.xodium.illyriabending.abilities.air.active

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
import kotlin.math.cos
import kotlin.math.sin

/**
 * AirBurst: charge a radial shockwave while sneaking, then release to detonate.
 *
 * While sneaking a sphere of air particles tightens around the caster. Releasing sneak
 * (or reaching max charge) erupts a shockwave that damages and hurls nearby entities.
 * Charge multiplies damage, range, and knockback up to [CHARGE_FACTOR].
 */
internal object AirBurst : AbilityInterface {
    override val cooldownMs: Long = 6000

    /** Blast radius in blocks (before charge factor). */
    private const val RADIUS = 5.0

    /** Damage in hearts dealt (before charge factor). */
    private const val DAMAGE = 2.0

    /** Horizontal knockback strength (before charge factor). */
    private const val KNOCKBACK = 1.8

    /** Vertical knockback component (before charge factor). */
    private const val KNOCKUP = 0.6

    /** How many milliseconds it takes to fully charge. */
    private const val MAX_CHARGE_TIME_MS = 1500L

    /** Damage/range/knockback multiplier at full charge. */
    private const val CHARGE_FACTOR = 2.5

    /** Ticks between charge-phase renders. */
    private const val TICK_PERIOD = 2L

    /** Particle density step along render rays. */
    private const val STEP = 0.4

    /** Active charging sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * Holds state for a charging AirBurst activation.
     */
    private class Session(
        val player: Player,
        val startTime: Long = System.currentTimeMillis(),
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (sessions.containsKey(player.uniqueId)) return

        val session = Session(player)
        sessions[player.uniqueId] = session

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(player.uniqueId, level) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a charging session by one tick. */
    private fun tick(
        uuid: UUID,
        level: Int,
    ) {
        val session = sessions[uuid] ?: return
        val player = session.player

        if (!player.isOnline || player.isDead) {
            remove(uuid)
            return
        }

        val elapsed = System.currentTimeMillis() - session.startTime
        val chargeFactor = minOf(1.0, elapsed.toDouble() / MAX_CHARGE_TIME_MS)

        if (!player.isSneaking || elapsed >= MAX_CHARGE_TIME_MS * 2L) {
            val factor = 1.0 + (CHARGE_FACTOR - 1.0) * chargeFactor
            remove(uuid)
            detonate(player, level, factor)
            return
        }

        renderCharge(player, chargeFactor)
    }

    /** Renders the tightening sphere while charging. */
    private fun renderCharge(
        player: Player,
        chargeFactor: Double,
    ) {
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val world = origin.world ?: return
        val radius = RADIUS * (1.0 - chargeFactor * 0.7)

        val rays = 24
        for (i in 0 until rays) {
            val ringAngle = i * (2.0 * PI / rays)
            val coneDir =
                Vector(cos(ringAngle) * sin(PI / 2), cos(PI / 2), sin(ringAngle) * sin(PI / 2))
                    .normalize()

            var d = radius
            while (d <= radius + STEP * 2) {
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

        if (Math.random() < 1.0 / 6.0) {
            world.playSound(origin, Sound.ENTITY_BREEZE_IDLE_GROUND, 0.4f, 1.5f)
        }
    }

    /** Detonates the burst at [player]'s current location with the given charge [factor]. */
    private fun detonate(
        player: Player,
        level: Int,
        factor: Double,
    ) {
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val world = origin.world ?: return

        val effectiveRadius = RADIUS * factor
        val levelScale = 1.0 + (level - 1) * 0.25

        world.playSound(origin, Sound.ENTITY_BREEZE_WIND_BURST, 1.5f, 0.8f)
        world.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f)
        renderBurst(origin, effectiveRadius)

        world
            .getNearbyEntities(origin, effectiveRadius, effectiveRadius, effectiveRadius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                entity.damage(DAMAGE * factor * levelScale, player)
                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .setY(KNOCKUP)
                        .multiply(KNOCKBACK * factor * levelScale)
                entity.velocity = entity.velocity.add(push)
            }
    }

    /** Renders the expanding shockwave particles. */
    private fun renderBurst(
        origin: Location,
        radius: Double,
    ) {
        val world = origin.world ?: return
        val rays = 48
        for (i in 0 until rays) {
            val phi = (i * PI * 2) / rays
            for (j in 0 until 8) {
                val theta = j * PI / 8
                val dir =
                    Vector(
                        cos(phi) * sin(theta),
                        cos(theta),
                        sin(phi) * sin(theta),
                    ).normalize()
                var d = 0.5
                while (d <= radius) {
                    world.spawnParticle(
                        Particle.CLOUD,
                        origin.clone().add(dir.clone().multiply(d)),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                    )
                    d += STEP * 2
                }
            }
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
