package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Material
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
 * EarthSurf: ride a churning crest of uprooted dirt like a wave.
 *
 * Earth bunches and rolls beneath the caster's feet, carrying them swiftly along the
 * ground in the direction they face. The bender steers by looking, and the rolling
 * berm sprays loam and gravel in its wake. Sneak again while riding to dismount early;
 * the wave subsides after [MAX_DURATION_TICKS].
 *
 * Wave speed, ride duration, and the size of the trailing berm scale with level.
 */
internal object EarthSurf : AbilityInterface {
    override val cooldownMs: Long = 9000

    /** Horizontal propulsion speed per tick (before level scaling). */
    private const val SPEED = 0.55

    /** Additional speed per level. */
    private const val SPEED_PER_LEVEL = 0.08

    /** Maximum ride duration in ticks (before level scaling). */
    private const val MAX_DURATION_TICKS = 120L

    /** Additional ride duration per level. */
    private const val DURATION_PER_LEVEL = 20L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Height the rider is held above the ground. */
    private const val RIDE_HEIGHT = 0.05

    /** Radius of the particle berm rendered around the caster. */
    private const val BERM_RADIUS = 0.7

    /** Active surfs keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for a single EarthSurf ride. */
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

        player.world.playSound(player.location, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.6f)

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(uuid) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a surf session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD

        val maxTicks = MAX_DURATION_TICKS + (session.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || session.ticks > maxTicks) {
            remove(uuid)
            return
        }

        propel(session)
        render(session)
        playSound(session)
    }

    /** Pushes the player horizontally along their facing direction each tick. */
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
        // Keep any downward fall so the rider doesn't hover; preserve a gentle slide.
        val y = if (current.y < 0.0) RIDE_HEIGHT else current.y
        player.velocity = Vector(velocity.x, y, velocity.z)
    }

    /** Renders a churning ring of earth and gravel around the caster's feet. */
    private fun render(session: Session) {
        val player = session.player
        val world = player.world
        val feet = player.location.clone()

        val dirtData = Material.DIRT.createBlockData()
        val gravelData = Material.GRAVEL.createBlockData()

        val spokes = 10
        for (i in 0 until spokes) {
            val angle = session.ticks * 0.35 + i * (2.0 * Math.PI / spokes)
            val x = kotlin.math.cos(angle) * BERM_RADIUS
            val z = kotlin.math.sin(angle) * BERM_RADIUS
            val point = feet.clone().add(x, 0.15, z)
            world.spawnParticle(Particle.BLOCK, point, 1, 0.05, 0.05, 0.05, 0.0, dirtData)
        }

        // Scatter loose gravel in the wake.
        val behind =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
                .multiply(-0.6)
        world.spawnParticle(
            Particle.BLOCK,
            feet.clone().add(behind.x, 0.1, behind.z),
            4,
            0.15,
            0.05,
            0.15,
            0.02,
            gravelData,
        )
    }

    /** Plays rolling earth sounds periodically while riding. */
    private fun playSound(session: Session) {
        if (Math.random() < 1.0 / 8.0) {
            session.player.world.playSound(
                session.player.location,
                Sound.BLOCK_GRAVEL_STEP,
                0.7f,
                0.9f,
            )
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
