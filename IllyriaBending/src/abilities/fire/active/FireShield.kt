package org.xodium.illyriabending.abilities.fire.active

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
 * FireShield: while sneaking, maintain a swirling shell of flame that scorches attackers
 * and incinerates projectiles.
 *
 * The shield persists until sneak is released or the maximum duration expires.
 * The shell radius and burn effect both scale with level.
 */
internal object FireShield : AbilityInterface {
    override val cooldownMs: Long = 5000

    /** Shield shell radius in blocks (before level scaling). */
    private const val RADIUS = 2.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 0.3

    /** Damage in hearts dealt to entities touching the shield per pulse. */
    private const val DAMAGE = 0.5

    /** Ticks of fire applied to entities touching the shield. */
    private const val FIRE_TICKS = 60

    /** Repulsion force applied to entities touching the shield. */
    private const val REPEL = 0.6

    /** Maximum shield duration in ticks. */
    private const val MAX_DURATION_TICKS = 160L

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 2L

    /** Rotational increment per tick for the spiral render. */
    private const val ROTATION_STEP = PI / 24

    /** Active shield sessions keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for an active FireShield session. */
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

    /** Renders the swirling shell of flame particles around the player. */
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
                world.spawnParticle(Particle.FLAME, origin.clone().add(offset), 1, 0.0, 0.0, 0.0, 0.0)
            }
        }
    }

    /** Burns and repels nearby entities; incinerates projectiles. */
    private fun repel(session: Session) {
        val player = session.player
        val origin = player.location.clone().add(0.0, 1.0, 0.0)
        val world = origin.world ?: return
        val radius = RADIUS + (session.level - 1) * RADIUS_PER_LEVEL

        world
            .getNearbyEntities(origin, radius, radius, radius)
            .forEach { entity ->
                when (entity) {
                    is Projectile -> {
                        entity.remove()
                        world.spawnParticle(
                            Particle.FLAME,
                            entity.location,
                            8,
                            0.2,
                            0.2,
                            0.2,
                            0.0,
                        )
                    }

                    is LivingEntity -> {
                        if (entity == player) return@forEach
                        entity.damage(DAMAGE, player)
                        entity.fireTicks = FIRE_TICKS
                        val away =
                            entity
                                .location
                                .toVector()
                                .subtract(origin.toVector())
                                .setY(0.2)
                                .normalize()
                                .multiply(REPEL)
                        entity.velocity = entity.velocity.add(away)
                    }
                }
            }
    }

    /** Plays periodic flame sound. */
    private fun playSound(session: Session) {
        if (Math.random() < 1.0 / 8.0) {
            session.player.world.playSound(
                session.player.location,
                Sound.BLOCK_FIRE_AMBIENT,
                0.6f,
                1.0f,
            )
        }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
