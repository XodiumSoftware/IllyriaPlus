package org.xodium.illyriabending.abilities.fire.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * FirePropagate: cause existing fire to surge outward in all directions.
 *
 * On activation, any fire blocks near the caster burst outward, spreading flames to
 * adjacent flammable blocks over a short duration. Living entities adjacent to the
 * propagating fire take damage and are ignited.
 *
 * Spread radius, duration, and damage scale with level.
 */
internal object FirePropagate : AbilityInterface {
    override val cooldownMs: Long = 7000

    /** Radius in which existing fires are detected (before level scaling). */
    private const val DETECT_RADIUS = 8

    /** How many blocks outward from existing flames the propagation travels per pulse. */
    private const val SPREAD_PER_TICK = 2

    /** Damage in hearts dealt to caught entities per pulse. */
    private const val DAMAGE = 0.75

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Maximum propagation duration in ticks. */
    private const val DURATION_TICKS = 60L

    /** Additional propagation duration per level. */
    private const val DURATION_PER_LEVEL = 10L

    /** Fire ticks applied to entities caught in propagating flame. */
    private const val FIRE_TICKS = 80

    /** Ticks between propagation pulses. */
    private const val TICK_PERIOD = 3L

    /** Active propagation sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for an active propagation: tracks visited fire blocks and spread frontier. */
    private class Session(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
        val frontier: MutableSet<Block> = mutableSetOf(),
        val visited: MutableSet<Block> = mutableSetOf(),
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

        val center = player.location
        val r = DETECT_RADIUS + (level - 1)
        val world = center.world ?: return

        for (dx in -r..r) {
            for (dy in -r..r) {
                for (dz in -r..r) {
                    val block = world.getBlockAt(center.blockX + dx, center.blockY + dy, center.blockZ + dz)
                    if (block.type == Material.FIRE) session.frontier.add(block)
                }
            }
        }

        if (session.frontier.isEmpty()) {
            sessions.remove(uuid)
            return
        }

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(uuid) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a propagation session by one pulse. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD

        val maxTicks = DURATION_TICKS + (session.level - 1) * DURATION_PER_LEVEL
        if (!player.isOnline || player.isDead || session.ticks > maxTicks || session.frontier.isEmpty()) {
            remove(uuid)
            return
        }

        spread(session)
        damage(session)
    }

    /**
     * Advances the fire frontier outward, igniting adjacent flammable blocks.
     */
    private fun spread(session: Session) {
        val newFrontier = mutableSetOf<Block>()

        for (block in session.frontier) {
            session.visited.add(block)
            val world = block.world

            val adjacent =
                listOf(
                    block.getRelative(1, 0, 0),
                    block.getRelative(-1, 0, 0),
                    block.getRelative(0, 1, 0),
                    block.getRelative(0, -1, 0),
                    block.getRelative(0, 0, 1),
                    block.getRelative(0, 0, -1),
                )

            for (next in adjacent) {
                if (next in session.visited) continue
                if (next in newFrontier) continue

                if (next.type == Material.FIRE) {
                    newFrontier.add(next)
                } else if (next.type.isFlammable) {
                    next.type = Material.FIRE
                    newFrontier.add(next)
                    world.spawnParticle(
                        Particle.FLAME,
                        next.location.clone().add(0.5, 0.5, 0.5),
                        6,
                        0.2,
                        0.2,
                        0.2,
                    )
                }
            }
        }

        if (Math.random() < 0.5 && session.frontier.isNotEmpty()) {
            session.frontier.first().world.playSound(
                session.frontier.first().location,
                Sound.BLOCK_FIRE_AMBIENT,
                0.7f,
                1.0f,
            )
        }

        session.frontier.clear()
        session.frontier.addAll(newFrontier.take(SPREAD_PER_TICK * 24 + (session.level - 1) * 8))
    }

    /**
     * Damages living entities adjacent to active flames.
     */
    private fun damage(session: Session) {
        val damage = DAMAGE + (session.level - 1) * DAMAGE_PER_LEVEL
        for (block in session.frontier) {
            block
                .world
                .getNearbyEntities(block.location.clone().add(0.5, 0.5, 0.5), 1.5, 1.5, 1.5)
                .filterIsInstance<LivingEntity>()
                .filter { it != session.player }
                .forEach { entity ->
                    entity.damage(damage, session.player)
                    entity.fireTicks = maxOf(entity.fireTicks, FIRE_TICKS)
                }
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
