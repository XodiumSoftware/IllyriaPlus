package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * FireJet: propel yourself through the air on a roaring jet of flame.
 *
 * Sneak-hold to ignite the jets. The player is launched along their look direction
 * (including vertical) at sustained speed, leaving a blazing trail of flame particles
 * behind them. Release sneak (or land) to extinguish the jets.
 *
 * Flight speed scales with level. Duration is capped at [MAX_DURATION_TICKS].
 */
internal object FireJet : AbilityInterface {
    override val cooldownMs: Long = 6000

    /** Sustained jet speed in blocks per tick (before level scaling). */
    private const val SPEED = 0.65

    /** Additional speed per level. */
    private const val SPEED_PER_LEVEL = 0.08

    /** Maximum flight duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 60L

    /** Additional duration in ticks per level. */
    private const val DURATION_PER_LEVEL = 15L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Active jet sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for a FireJet flight. */
    private class Session(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
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

    /** Advances a jet session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD

        val maxTicks = MAX_DURATION_TICKS + (session.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || session.ticks > maxTicks || !player.isSneaking) {
            remove(uuid)
            return
        }

        propel(session)
        render(session)
        playSound(session)
    }

    /** Pushes the player along their look direction each tick. */
    private fun propel(session: Session) {
        val player = session.player
        val dir =
            player
                .location
                .direction
                .clone()
                .normalize()
        val speed = SPEED + (session.level - 1) * SPEED_PER_LEVEL

        player.velocity = dir.multiply(speed)
    }

    /** Renders the flame trail behind the player. */
    private fun render(session: Session) {
        val player = session.player
        val world = player.world
        val dir =
            player
                .location
                .direction
                .clone()
                .normalize()

        val trail =
            player
                .location
                .clone()
                .subtract(dir.clone().multiply(0.5))
                .add(0.0, -0.3, 0.0)

        world.spawnParticle(Particle.FLAME, trail, 8, 0.2, 0.2, 0.2, 0.05)
        world.spawnParticle(Particle.SMOKE, trail, 2, 0.15, 0.15, 0.15, 0.0)
        world.spawnParticle(Particle.LAVA, trail, 1, 0.1, 0.1, 0.1, 0.0)
    }

    /** Plays the periodic roar of the jets. */
    private fun playSound(session: Session) {
        if (Math.random() < 1.0 / 6.0) {
            session.player.world.playSound(
                session.player.location,
                Sound.ENTITY_BLAZE_SHOOT,
                0.5f,
                0.7f,
            )
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
