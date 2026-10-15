package org.xodium.illyriabending.abilities.fire.sequence

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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

    /** Active spin sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for an active FireSpin. */
    private class Session(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
        var verticalAngle: Double = 0.0,
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId
        if (sessions.containsKey(uuid)) return

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

    /** Advances the spin by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD
        session.angle += ROTATION_STEP
        session.verticalAngle += ROTATION_STEP * 0.5

        val maxTicks = MAX_SPIN_TICKS + (session.level - 1) * SPIN_PER_LEVEL
        if (!player.isOnline || player.isDead || session.ticks > maxTicks) {
            remove(uuid)
            return
        }

        render(session)
        damage(session)
        playSound(session)
    }

    /** Renders the spinning spiral of flame around the player. */
    private fun render(session: Session) {
        val player = session.player
        val world = player.world
        val radius = RADIUS + (session.level - 1) * RADIUS_PER_LEVEL
        val origin = player.location.clone().add(0.0, 1.0, 0.0)

        val arms = 4
        for (arm in 0 until arms) {
            val baseAngle = session.angle + arm * (2.0 * PI / arms)
            val vertical = sin(session.verticalAngle + arm * (2.0 * PI / arms)) * 0.7

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
    private fun damage(session: Session) {
        val player = session.player
        val radius = RADIUS + (session.level - 1) * RADIUS_PER_LEVEL
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val damage = DAMAGE + (session.level - 1) * DAMAGE_PER_LEVEL

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
    private fun playSound(session: Session) {
        if (Math.random() < 1.0 / 6.0) {
            session.player.world.playSound(
                session.player.location,
                Sound.BLOCK_FIRE_AMBIENT,
                0.6f,
                1.2f,
            )
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
