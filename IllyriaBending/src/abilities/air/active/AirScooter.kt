package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * AirScooter: summon an air bubble that propels the caster forward.
 *
 * Activating (sneak-hold) mounts the player on a spinning disc of air and launches
 * them along the ground. Re-toggling sneak ends the ride early; otherwise the scooter
 * expires after [MAX_DURATION_TICKS].
 *
 * Speed scales with level.
 */
internal object AirScooter : AbilityInterface {
    override val cooldownMs: Long = 4000

    /** Horizontal propulsion speed per tick (before level scaling). */
    private const val SPEED = 0.4

    /** Additional speed per level. */
    private const val SPEED_PER_LEVEL = 0.1

    /** Maximum scooter duration in ticks before auto-expire. */
    private const val MAX_DURATION_TICKS = 100L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Height offset of particles under the player. */
    private const val PARTICLE_Y_OFFSET = -0.35

    /** Active scooter sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * Holds state for an active scooter ride.
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
        val uuid = player.uniqueId
        if (sessions.containsKey(uuid)) {
            remove(uuid)
            return
        }

        val session = Session(player, level)
        sessions[uuid] = session

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(uuid) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a scooter session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD
        session.angle += 0.4

        if (!player.isOnline || player.isDead || session.ticks > MAX_DURATION_TICKS) {
            remove(uuid)
            return
        }

        propel(session)
        render(session)
        playSound(session)
    }

    /** Pushes the player horizontally each tick. */
    private fun propel(session: Session) {
        val player = session.player
        val dir =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val speed = SPEED + (session.level - 1) * SPEED_PER_LEVEL

        val velocity = dir.multiply(speed)
        val current = player.velocity
        player.velocity = Vector(velocity.x, current.y, velocity.z)
    }

    /** Renders the spinning disc of air under the player's feet. */
    private fun render(session: Session) {
        val player = session.player
        val world = player.world
        val origin = player.location.clone().add(0.0, PARTICLE_Y_OFFSET, 0.0)

        val rays = 12
        for (i in 0 until rays) {
            val angle = session.angle + i * (2.0 * Math.PI / rays)
            val offset =
                Vector(
                    Math.cos(angle) * 0.5,
                    0.0,
                    Math.sin(angle) * 0.5,
                )
            world.spawnParticle(Particle.CLOUD, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
        }

        world.spawnParticle(Particle.CLOUD, origin, 3, 0.2, 0.05, 0.2, 0.05)
    }

    /** Plays periodic sound while riding. */
    private fun playSound(session: Session) {
        if (Math.random() < 1.0 / 10.0) {
            session.player.world.playSound(
                session.player.location,
                Sound.ENTITY_BREEZE_IDLE_GROUND,
                0.4f,
                1.6f,
            )
        }
    }

    /** Cancels and removes a scooter session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
