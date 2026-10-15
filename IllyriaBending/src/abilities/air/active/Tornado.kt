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
 * Tornado: summon a towering vortex of wind ahead of the caster.
 *
 * On activation, a spinning funnel of air particles appears at a target location a short
 * distance in front of the player. Nearby entities are drawn toward the funnel's center,
 * lifted upward, and flung out at the top. The tornado expires after
 * [MAX_DURATION_TICKS].
 *
 * Funnel height, pull strength, and duration scale with level.
 */
internal object Tornado : AbilityInterface {
    override val cooldownMs: Long = 12000

    /** Horizontal distance ahead of the player where the tornado is summoned. */
    private const val SPAWN_DISTANCE = 4.0

    /** Base funnel height in blocks (before level scaling). */
    private const val HEIGHT = 6.0

    /** Additional funnel height per level. */
    private const val HEIGHT_PER_LEVEL = 1.0

    /** Radius of the suction area around the funnel. */
    private const val PULL_RADIUS = 5.0

    /** Pull strength applied per tick toward the funnel center. */
    private const val PULL = 0.35

    /** Upward lift applied per tick to entities inside the funnel. */
    private const val LIFT = 0.45

    /** Horizontal fling speed applied when entities reach the funnel top. */
    private const val FLING = 0.9

    /** Maximum tornado duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 100L

    /** Additional duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 20L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 2L

    /** Rotational increment per tick for the spiral render. */
    private const val ROTATION_STEP = 0.6

    /** Active tornado sessions keyed by caster UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * Holds state for an active tornado.
     */
    private class Session(
        val player: Player,
        val center: Location,
        val height: Double,
        val maxTicks: Long,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (sessions.containsKey(player.uniqueId)) return

        val forward =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val center =
            player
                .location
                .clone()
                .add(forward.multiply(SPAWN_DISTANCE))
        val height = HEIGHT + (level - 1) * HEIGHT_PER_LEVEL
        val maxTicks = MAX_DURATION_TICKS + (level - 1) * DURATION_PER_LEVEL

        val session = Session(player, center, height, maxTicks)
        sessions[player.uniqueId] = session

        center.world?.playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 1.2f, 0.7f)

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(player.uniqueId) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a tornado session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD
        session.angle += ROTATION_STEP

        if (!player.isOnline || session.ticks > session.maxTicks) {
            remove(uuid)
            return
        }

        render(session)
        suck(session)
        playSound(session)
    }

    /** Renders the spinning funnel of air particles from ground to peak. */
    private fun render(session: Session) {
        val world = session.center.world ?: return

        var y = 0.0
        while (y <= session.height) {
            val progress = y / session.height
            val radiusAt = 0.4 + progress * 1.4
            val rays = 6
            for (i in 0 until rays) {
                val angle = session.angle + i * (2.0 * PI / rays) + y * 0.5
                val offset =
                    Vector(
                        cos(angle) * radiusAt,
                        y,
                        sin(angle) * radiusAt,
                    )
                world.spawnParticle(
                    Particle.CLOUD,
                    session.center.clone().add(offset),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                )
            }
            y += 0.5
        }
    }

    /** Pulls nearby entities toward the funnel, lifts them, and flings them at the top. */
    private fun suck(session: Session) {
        val world = session.center.world ?: return

        world
            .getNearbyEntities(session.center, PULL_RADIUS, session.height, PULL_RADIUS)
            .filterIsInstance<LivingEntity>()
            .filter { it != session.player }
            .filter { it.location.y <= session.center.y + session.height + 2.0 }
            .forEach { entity ->
                val toCenter =
                    session
                        .center
                        .toVector()
                        .subtract(entity.location.toVector())
                        .setY(0.0)
                val dist = toCenter.length()
                val current = entity.velocity

                if (dist > 0.8) {
                    val pull = toCenter.normalize().multiply(PULL)
                    entity.velocity = Vector(current.x + pull.x, LIFT, current.z + pull.z)
                } else if (entity.location.y >= session.center.y + session.height - 1.0) {
                    val angle = Math.random() * 2.0 * PI
                    val fling = Vector(cos(angle) * FLING, 0.3, sin(angle) * FLING)
                    entity.velocity = current.add(fling)
                } else {
                    entity.velocity = Vector(current.x * 0.6, LIFT, current.z * 0.6)
                }
            }
    }

    /** Plays periodic wind sound. */
    private fun playSound(session: Session) {
        if (Math.random() < 1.0 / 6.0) {
            session.center.world?.playSound(
                session.center,
                Sound.ENTITY_BREEZE_IDLE_GROUND,
                0.8f,
                0.9f,
            )
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
