package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

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
    override val activationType: ActivationType = ActivationType.DURATION
    override val hasSession: Boolean = true

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

    /** Per-session scooter state keyed by player UUID. */
    private val states = mutableMapOf<UUID, ScooterState>()

    /** Holds per-session state for an active scooter ride. */
    private class ScooterState(
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) {
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }
        if (isOnCooldown(player)) return

        states[uuid] = ScooterState(level)
        SessionManager.start(this, uuid, TICK_PERIOD)
    }

    override fun tickSession(uuid: UUID) {
        val session = SessionManager.get(this, uuid) ?: return
        val player = session.player
        val state = states[uuid] ?: return

        state.ticks += TICK_PERIOD
        state.angle += 0.4

        if (!player.isOnline || player.isDead || state.ticks > MAX_DURATION_TICKS) {
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        propel(player, state)
        render(player, state)
        playSound(player)
    }

    /** Pushes the player horizontally each tick. */
    private fun propel(
        player: Player,
        state: ScooterState,
    ) {
        val dir =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val speed = SPEED + (state.level - 1) * SPEED_PER_LEVEL

        val velocity = dir.multiply(speed)
        val current = player.velocity
        player.velocity = Vector(velocity.x, current.y, velocity.z)
    }

    /** Renders the spinning disc of air under the player's feet. */
    private fun render(
        player: Player,
        state: ScooterState,
    ) {
        val world = player.world
        val origin = player.location.clone().add(0.0, PARTICLE_Y_OFFSET, 0.0)

        val rays = 12
        for (i in 0 until rays) {
            val angle = state.angle + i * (2.0 * Math.PI / rays)
            val offset =
                Vector(
                    cos(angle) * 0.5,
                    0.0,
                    sin(angle) * 0.5,
                )
            world.spawnParticle(Particle.CLOUD, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
        }

        world.spawnParticle(Particle.CLOUD, origin, 3, 0.2, 0.05, 0.2, 0.05)
    }

    /** Plays periodic sound while riding. */
    private fun playSound(player: Player) {
        if (Math.random() < 1.0 / 10.0) {
            player.world.playSound(
                player.location,
                Sound.ENTITY_BREEZE_IDLE_GROUND,
                0.4f,
                1.6f,
            )
        }
    }
}
