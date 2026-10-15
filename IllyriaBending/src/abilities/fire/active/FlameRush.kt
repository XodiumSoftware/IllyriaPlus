package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID

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
    override val activationType: ActivationType = ActivationType.DURATION
    override val hasSession: Boolean = true

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

    /** Active rush states keyed by player UUID. */
    private val states = mutableMapOf<UUID, RushState>()

    /** Holds state for a FlameRush dash. */
    private class RushState(
        val level: Int,
        val direction: Vector,
        val distance: Double,
        var travelled: Double = 0.0,
        val hitEntities: MutableSet<LivingEntity> = mutableSetOf(),
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return
        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) return

        val dir =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val distance = DISTANCE + (level - 1) * DISTANCE_PER_LEVEL

        states[uuid] = RushState(level, dir, distance)
        SessionManager.start(this, uuid, TICK_PERIOD)

        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.9f)
    }

    override fun tickSession(uuid: UUID) {
        val session = SessionManager.get(this, uuid) ?: return
        val player = session.player
        val state = states[uuid] ?: return

        fun end() {
            states.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
        }

        if (!player.isOnline || player.isDead) {
            end()
            return
        }

        if (state.travelled >= state.distance) {
            end()
            return
        }

        val step = DASH_SPEED.coerceAtMost(state.distance - state.travelled)

        // Stop if the player runs into a wall.
        val nextBlock =
            player
                .location
                .clone()
                .add(state.direction.clone().multiply(step + 0.5))
                .block
        if (!nextBlock.isPassable && !nextBlock.type.isAir) {
            end()
            return
        }

        player.velocity = state.direction.clone().multiply(step)
        state.travelled += step

        render(player)
        damage(player, state)
    }

    /** Renders the trail of flame behind the rushing player. */
    private fun render(player: Player) {
        val world = player.world
        val loc =
            player
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
    private fun damage(
        player: Player,
        state: RushState,
    ) {
        val world = player.world
        val damage = DAMAGE + (state.level - 1) * DAMAGE_PER_LEVEL

        world
            .getNearbyEntities(player.location, 1.8, 1.8, 1.8)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { it !in state.hitEntities }
            .forEach { entity ->
                state.hitEntities.add(entity)
                entity.damage(damage, player)
                entity.fireTicks = maxOf(entity.fireTicks, FIRE_TICKS)

                // Perpendicular shove off the rush line.
                val perp = Vector(-state.direction.z, 0.2, state.direction.x)
                val side = if (Math.random() < 0.5) perp else perp.multiply(-1.0)
                entity.velocity = entity.velocity.add(side.multiply(KNOCKBACK))
            }
    }
}
