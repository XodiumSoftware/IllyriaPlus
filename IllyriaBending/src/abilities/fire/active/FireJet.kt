package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID

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
    override val activationType: ActivationType = ActivationType.TOGGLE
    override val hasSession: Boolean = true

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

    /** Per-player jet flight state keyed by player UUID. */
    private val states = mutableMapOf<UUID, JetState>()

    /** Holds per-session state for an active FireJet flight. */
    private class JetState(
        val level: Int,
        var ticks: Long = 0L,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) {
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        if (isOnCooldown(player)) return

        states[uuid] = JetState(level)
        SessionManager.start(this, uuid, TICK_PERIOD)
    }

    override fun tickSession(uuid: UUID) {
        val session = SessionManager.get(this, uuid) ?: return
        val player = session.player
        val state = states[uuid] ?: return

        state.ticks += TICK_PERIOD

        val maxTicks = MAX_DURATION_TICKS + (state.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || state.ticks > maxTicks || !player.isSneaking) {
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        propel(player, state)
        render(player)
        playSound(player)
    }

    /** Pushes the player along their look direction each tick. */
    private fun propel(
        player: Player,
        state: JetState,
    ) {
        val dir =
            player
                .location
                .direction
                .clone()
                .normalize()
        val speed = SPEED + (state.level - 1) * SPEED_PER_LEVEL

        player.velocity = dir.multiply(speed)
    }

    /** Renders the flame trail behind the player. */
    private fun render(player: Player) {
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
    private fun playSound(player: Player) {
        if (Math.random() < 1.0 / 6.0) {
            player.world.playSound(
                player.location,
                Sound.ENTITY_BLAZE_SHOOT,
                0.5f,
                0.7f,
            )
        }
    }
}
