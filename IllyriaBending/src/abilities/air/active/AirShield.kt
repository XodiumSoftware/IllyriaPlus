package org.xodium.illyriabending.abilities.air.active

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * AirShield: while sneaking, maintain a swirling shell of wind that deflects projectiles
 * and pushes away nearby entities.
 *
 * The shield persists until sneak is released or the maximum duration expires.
 * The shell radius and repel force both scale with level.
 */
internal object AirShield : AbilityInterface {
    override val cooldownMs: Long = 4000

    /** Shield shell radius in blocks (before level scaling). */
    private const val RADIUS = 2.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.3

    /** Repulsion force applied to entities touching the shield. */
    private const val REPEL = 0.8

    /** Projectile deflection velocity multiplier. */
    private const val DEFLECT = 0.6

    /** Maximum shield duration in ticks. */
    private const val MAX_DURATION_TICKS = 160L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 2L

    /** Rotational increment per tick for the spiral render. */
    private const val ROTATION_STEP = PI / 24

    /** Active shield sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * Holds state for an active AirShield session.
     */
    private class Session(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
        var angle: Double = 0.0,
        var task: BukkitTask? = null,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (sessions.containsKey(player.uniqueId)) return

        val session = Session(player, level)
        sessions[player.uniqueId] = session

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(player.uniqueId) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a shield session by one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        session.ticks += TICK_PERIOD
        session.angle += ROTATION_STEP

        if (!player.isOnline || player.isDead || !player.isSneaking || session.ticks > MAX_DURATION_TICKS) {
            remove(uuid)
            return
        }

        render(session)
        repel(session)
        playSound(session)
    }

    /** Renders the swirling shell of air particles around the player. */
    private fun render(session: Session) {
        val player = session.player
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val world = origin.world ?: return
        val radius = RADIUS + (session.level - 1) * RADIUS_PER_LEVEL

        val rings = 3
        for (ring in 0 until rings) {
            val phi = ring * PI / rings + session.angle
            val rays = 16
            for (i in 0 until rays) {
                val theta = i * (2.0 * PI / rays) + session.angle
                val offset =
                    Vector(
                        cos(theta) * sin(phi) * radius,
                        cos(phi) * radius,
                        sin(theta) * sin(phi) * radius,
                    )
                world.spawnParticle(Particle.CLOUD, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            }
        }
    }

    /** Repels nearby entities and deflects projectiles. */
    private fun repel(session: Session) {
        val player = session.player
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val world = origin.world ?: return
        val radius = RADIUS + (session.level - 1) * RADIUS_PER_LEVEL
        val levelScale = 1.0 + (session.level - 1) * 0.2

        world
            .getNearbyEntities(origin, radius, radius, radius)
            .forEach { entity ->
                when (entity) {
                    is Projectile -> {
                        val away =
                            entity
                                .location
                                .toVector()
                                .subtract(origin.toVector())
                                .normalize()
                        entity.velocity = away.multiply(entity.velocity.length() * DEFLECT)
                    }

                    is LivingEntity -> {
                        if (entity == player) return@forEach
                        val away =
                            entity
                                .location
                                .toVector()
                                .subtract(origin.toVector())
                                .setY(0.2)
                                .normalize()
                                .multiply(REPEL * levelScale)
                        entity.velocity = entity.velocity.add(away)
                    }
                }
            }
    }

    /** Plays periodic wind sound. */
    private fun playSound(session: Session) {
        if (Math.random() < 1.0 / 8.0) {
            session.player.world.playSound(
                session.player.location,
                Sound.ENTITY_BREEZE_IDLE_GROUND,
                0.5f,
                1.2f,
            )
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
