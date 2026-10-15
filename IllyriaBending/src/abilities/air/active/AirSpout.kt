package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * AirSpout: create an updraft beneath the caster that lifts them while sneaking.
 *
 * A column of spinning air particles rises under the player, providing enough lift to
 * hover. Nearby entities caught in the column are also pushed upward. The spout ends
 * when sneak is released or after the maximum duration.
 *
 * Lift strength and hover duration scale with level.
 */
internal object AirSpout : AbilityInterface {
    override val cooldownMs: Long = 3500
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.DURATION
    override val hasSession: Boolean = true

    /** Vertical lift applied per tick to the caster (before level scaling). */
    private const val LIFT = 0.18

    /** Additional lift per level. */
    private const val LIFT_PER_LEVEL = 0.03

    /** Lift applied to nearby entities per tick. */
    private const val ENTITY_LIFT = 0.25

    /** Spout column radius affecting nearby entities. */
    private const val COLUMN_RADIUS = 1.5

    /** Maximum spout duration in ticks. */
    private const val MAX_DURATION_TICKS = 120L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Height of the spout column for the render. */
    private const val RENDER_HEIGHT = 3.0

    /** Per-session spout state keyed by player UUID. */
    private val states = mutableMapOf<UUID, SpoutState>()

    /** Holds per-session state for an active spout. */
    private class SpoutState(
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return
        if (SessionManager.has(this, player.uniqueId)) return

        states[player.uniqueId] = SpoutState(level)
        SessionManager.start(this, player.uniqueId, TICK_PERIOD)
    }

    override fun tickSession(uuid: UUID) {
        val session = SessionManager.get(this, uuid) ?: return
        val player = session.player
        val state = states[uuid] ?: return

        state.ticks += TICK_PERIOD
        state.angle += 0.5

        if (!player.isOnline || player.isDead || !player.isSneaking || state.ticks > MAX_DURATION_TICKS) {
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        lift(player, state)
        render(player, state)
        playSound(player)
    }

    /** Applies lift to the caster and nearby entities. */
    private fun lift(
        player: Player,
        state: SpoutState,
    ) {
        val lift = LIFT + (state.level - 1) * LIFT_PER_LEVEL

        val current = player.velocity
        player.velocity = Vector(current.x, lift, current.z)

        player
            .world
            .getNearbyEntities(player.location, COLUMN_RADIUS, COLUMN_RADIUS, COLUMN_RADIUS)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                val eCurrent = entity.velocity
                entity.velocity = Vector(eCurrent.x, ENTITY_LIFT, eCurrent.z)
            }
    }

    /** Renders the spinning column of air beneath the player. */
    private fun render(
        player: Player,
        state: SpoutState,
    ) {
        val world = player.world
        val base = player.location.clone().subtract(0.0, 0.5, 0.0)

        var y = 0.0
        while (y <= RENDER_HEIGHT) {
            val radiusAt = 0.6 - (y / RENDER_HEIGHT) * 0.3
            val rays = 6
            for (i in 0 until rays) {
                val angle = state.angle + i * (2.0 * PI / rays) + y * 0.8
                val offset =
                    Vector(
                        cos(angle) * radiusAt,
                        -y,
                        sin(angle) * radiusAt,
                    )
                world.spawnParticle(Particle.CLOUD, base.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            }
            y += 0.4
        }
    }

    /** Plays periodic sound. */
    private fun playSound(player: Player) {
        if (Math.random() < 1.0 / 8.0) {
            player.world.playSound(
                player.location,
                Sound.ENTITY_BREEZE_IDLE_GROUND,
                0.5f,
                1.4f,
            )
        }
    }
}
