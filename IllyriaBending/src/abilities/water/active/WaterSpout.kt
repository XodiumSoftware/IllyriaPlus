package org.xodium.illyriabending.abilities.water.active

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
 * WaterSpout: summon a swirling column of water that lifts the caster upward.
 *
 * A vertical spout of splash and droplet particles rises beneath the player, granting
 * steady vertical lift while sneaking. Nearby entities caught in the column are also
 * pushed up. The spout ends when sneak is released, the caster leaves the water-less
 * ground too far behind, or the maximum duration expires.
 *
 * Lift strength and duration scale with level.
 */
internal object WaterSpout : AbilityInterface {
    override val cooldownMs: Long = 4000

    /** Vertical lift applied per tick to the caster (before level scaling). */
    private const val LIFT = 0.16

    /** Additional lift per level. */
    private const val LIFT_PER_LEVEL = 0.03

    /** Lift applied to nearby entities per tick. */
    private const val ENTITY_LIFT = 0.24

    /** Spout column radius affecting nearby entities. */
    private const val COLUMN_RADIUS = 1.6

    /** Maximum spout duration in ticks. */
    private const val MAX_DURATION_TICKS = 120L

    /** Additional duration per level. */
    private const val DURATION_PER_LEVEL = 20L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Height of the spout column for the render. */
    private const val RENDER_HEIGHT = 3.2

    /** Rotational increment per tick for the render. */
    private const val ROTATION_STEP = 0.5

    /** Active spout sessions keyed by caster UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * Holds state for an active WaterSpout.
     */
    private class Session(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (sessions.containsKey(player.uniqueId)) return

        val session = Session(player, level)
        sessions[player.uniqueId] = session

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(player.uniqueId) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a spout session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD
        session.angle += ROTATION_STEP

        val maxTicks = MAX_DURATION_TICKS + (session.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || !player.isSneaking || session.ticks > maxTicks) {
            remove(uuid)
            return
        }

        lift(session)
        render(session)
        playSound(session)
    }

    /** Applies lift to the caster and nearby entities. */
    private fun lift(session: Session) {
        val player = session.player
        val lift = LIFT + (session.level - 1) * LIFT_PER_LEVEL

        val current = player.velocity
        player.velocity = Vector(current.x, lift, current.z)

        val world = player.world
        world
            .getNearbyEntities(player.location, COLUMN_RADIUS, COLUMN_RADIUS, COLUMN_RADIUS)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                val eCurrent = entity.velocity
                entity.velocity = Vector(eCurrent.x, ENTITY_LIFT, eCurrent.z)
            }
    }

    /** Renders the spinning column of water beneath the player. */
    private fun render(session: Session) {
        val player = session.player
        val world = player.world
        val base = player.location.clone().subtract(0.0, 0.5, 0.0)

        var y = 0.0
        while (y <= RENDER_HEIGHT) {
            val radiusAt = 0.7 - (y / RENDER_HEIGHT) * 0.3
            val rays = 5
            for (i in 0 until rays) {
                val angle = session.angle + i * (2.0 * PI / rays) + y * 0.9
                val offset =
                    Vector(
                        cos(angle) * radiusAt,
                        -y,
                        sin(angle) * radiusAt,
                    )
                world.spawnParticle(
                    Particle.SPLASH,
                    base.clone().add(offset),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                )
                if (Math.random() < 0.2) {
                    world.spawnParticle(
                        Particle.BUBBLE_COLUMN_UP,
                        base.clone().add(offset.multiply(0.8)),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                    )
                }
            }
            y += 0.35
        }
    }

    /** Plays periodic water sounds. */
    private fun playSound(session: Session) {
        if (Math.random() < 1.0 / 6.0) {
            session.player.world.playSound(
                session.player.location,
                Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT,
                0.6f,
                1.1f,
            )
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
