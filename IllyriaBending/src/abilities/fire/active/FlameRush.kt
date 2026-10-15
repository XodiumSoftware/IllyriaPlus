package org.xodium.illyriabending.abilities.fire.active

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

/**
 * FlameRush: dash forward wreathed in flame, scorching anything in the way.
 *
 * The player charges ahead along their facing direction (horizontal only), leaving a
 * brief trail of fire. Living entities caught in the path take damage, are set aflame,
 * and are knocked to the side.
 *
 * Dash distance, damage, and trail length scale with level.
 */
internal object FlameRush : AbilityInterface {
    override val cooldownMs: Long = 5500

    /** Dash distance in blocks (before level scaling). */
    private const val DISTANCE = 8.0

    /** Additional dash distance per level. */
    private const val DISTANCE_PER_LEVEL = 1.0

    /** Dash speed in blocks per tick. */
    private const val DASH_SPEED = 1.2

    /** Damage in hearts dealt to entities caught in the path (before level scaling). */
    private const val DAMAGE = 1.5

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Ticks of fire applied to entities caught in the path. */
    private const val FIRE_TICKS = 60

    /** Sideways knockback strength. */
    private const val KNOCKBACK = 0.8

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Active rush sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for a FlameRush dash. */
    private class Session(
        val player: Player,
        val level: Int,
        val direction: Vector,
        val distance: Double,
        var travelled: Double = 0.0,
        val hitEntities: MutableSet<LivingEntity> = mutableSetOf(),
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId
        if (sessions.containsKey(uuid)) return

        val dir =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val distance = DISTANCE + (level - 1) * DISTANCE_PER_LEVEL

        val session = Session(player, level, dir, distance)
        sessions[uuid] = session

        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.9f)

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(uuid) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a rush session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        if (!player.isOnline || player.isDead) {
            remove(uuid)
            return
        }

        if (session.travelled >= session.distance) {
            remove(uuid)
            return
        }

        val step = DASH_SPEED.coerceAtMost(session.distance - session.travelled)

        // Stop if the player runs into a wall.
        val nextBlock =
            player
                .location
                .clone()
                .add(session.direction.clone().multiply(step + 0.5))
                .block
        if (!nextBlock.isPassable && !nextBlock.type.isAir) {
            remove(uuid)
            return
        }

        player.velocity = session.direction.clone().multiply(step)
        session.travelled += step

        render(session)
        damage(session)
    }

    /** Renders the trail of flame behind the rushing player. */
    private fun render(session: Session) {
        val world = session.player.world
        val loc =
            session
                .player
                .location
                .clone()
                .add(0.0, 0.5, 0.0)

        world.spawnParticle(Particle.FLAME, loc, 12, 0.3, 0.3, 0.3, 0.0)
        world.spawnParticle(Particle.LAVA, loc, 2, 0.2, 0.2, 0.2, 0.0)

        if (Math.random() < 1.0 / 4.0) {
            world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.4f, 1.0f)
        }
    }

    /** Damages entities in the path. */
    private fun damage(session: Session) {
        val world = session.player.world
        val damage = DAMAGE + (session.level - 1) * DAMAGE_PER_LEVEL

        world
            .getNearbyEntities(session.player.location, 1.8, 1.8, 1.8)
            .filterIsInstance<LivingEntity>()
            .filter { it != session.player }
            .filter { it !in session.hitEntities }
            .forEach { entity ->
                session.hitEntities.add(entity)
                entity.damage(damage, session.player)
                entity.fireTicks = maxOf(entity.fireTicks, FIRE_TICKS)

                // Perpendicular shove off the rush line.
                val perp = Vector(-session.direction.z, 0.2, session.direction.x)
                val side = if (Math.random() < 0.5) perp else perp.multiply(-1.0)
                entity.velocity = entity.velocity.add(side.multiply(KNOCKBACK))
            }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
