package org.xodium.illyriabending.abilities.fire.sequence

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * FireSpin: whirl on the spot, spinning flame outward in all directions.
 *
 * For a few ticks, the caster spins in place while a corkscrew of flame spirals outward
 * from their chest, scorching and pushing anyone near. Entities take multiple hits as the
 * spiral sweeps past.
 *
 * Spin duration and spiral damage scale with level.
 */
internal object FireSpin : AbilityInterface {
    override val cooldownMs: Long = 4000
    override val activationType: ActivationTypeEnum = ActivationTypeEnum.DURATION
    override val hasSession: Boolean = true

    /** Spiral radius in blocks (before level scaling). */
    private const val RADIUS = 3.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.4

    /** Damage in hearts dealt per hit on entities in the spiral path. */
    private const val DAMAGE = 0.5

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.1

    /** Maximum spin duration in ticks. */
    private const val MAX_SPIN_TICKS = 30L

    /** Additional spin duration per level. */
    private const val SPIN_PER_LEVEL = 4L

    /** Rotational increment per tick for the spiral. */
    private const val ROTATION_STEP = PI / 6

    /** Ticks of fire applied to hit entities. */
    private const val FIRE_TICKS = 60

    /** Knockback strength applied to hit entities. */
    private const val KNOCKBACK = 0.8

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Active spin states keyed by player UUID. */
    private val states = mutableMapOf<UUID, SpinState>()

    /** Holds state for an active FireSpin. */
    private class SpinState(
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
        var verticalAngle: Double = 0.0,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return
        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) return

        states[uuid] = SpinState(level)
        SessionManager.start(this, uuid, TICK_PERIOD)
    }

    override fun tickSession(uuid: UUID) {
        val session = SessionManager.get(this, uuid) ?: return
        val player = session.player
        val state = states[uuid] ?: return

        state.ticks += TICK_PERIOD
        state.angle += ROTATION_STEP
        state.verticalAngle += ROTATION_STEP * 0.5

        val maxTicks = MAX_SPIN_TICKS + (state.level - 1) * SPIN_PER_LEVEL
        if (!player.isOnline || player.isDead || state.ticks > maxTicks) {
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        render(player, state)
        damage(player, state)
        playSound(player)
    }

    /** Renders the spinning spiral of flame around the player. */
    private fun render(
        player: Player,
        state: SpinState,
    ) {
        val world = player.world
        val radius = RADIUS + (state.level - 1) * RADIUS_PER_LEVEL
        val origin = player.location.clone().add(0.0, 1.0, 0.0)

        val arms = 4
        for (arm in 0 until arms) {
            val baseAngle = state.angle + arm * (2.0 * PI / arms)
            val vertical = sin(state.verticalAngle + arm * (2.0 * PI / arms)) * 0.7

            var d = 0.3
            while (d < radius) {
                val x = cos(baseAngle) * d
                val z = sin(baseAngle) * d
                val point = origin.clone().add(x, vertical, z)
                world.spawnParticle(Particle.FLAME, point, 1, 0.0, 0.02, 0.0, 0.0)
                d += 0.3
            }
        }
    }

    /** Damages and hurls entities in the spiral's reach. */
    private fun damage(
        player: Player,
        state: SpinState,
    ) {
        val radius = RADIUS + (state.level - 1) * RADIUS_PER_LEVEL
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val damage = DAMAGE + (state.level - 1) * DAMAGE_PER_LEVEL

        player
            .world
            .getNearbyEntities(origin, radius, radius, radius)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .forEach { entity ->
                entity.damage(damage, player)
                entity.fireTicks = maxOf(entity.fireTicks, FIRE_TICKS)

                val push =
                    entity
                        .location
                        .toVector()
                        .subtract(origin.toVector())
                        .normalize()
                        .setY(0.25)
                        .multiply(KNOCKBACK)
                entity.velocity = entity.velocity.add(push)
            }
    }

    /** Plays periodic fire sound while spinning. */
    private fun playSound(player: Player) {
        if (Math.random() < 1.0 / 6.0) {
            player.world.playSound(
                player.location,
                Sound.BLOCK_FIRE_AMBIENT,
                0.6f,
                1.2f,
            )
        }
    }
}
