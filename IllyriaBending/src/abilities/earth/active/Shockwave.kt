package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shockwave: send a shivering ring of compressed earth rolling ahead of the caster.
 *
 * The caster slams forward, calling a disc of broken ground to march outward in the
 * facing direction. Each tick the wave's radius expands, picking up debris and
 * anything caught in its path — entities at the wave's leading edge take damage and
 * are tossed upward and away.
 *
 * Wave reach, damage, and launch strength scale with level.
 */
internal object Shockwave : AbilityInterface {
    override val cooldownMs: Long = 8000

    /** Maximum wave radius in blocks (before level scaling). */
    private const val RADIUS = 8.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 1.0

    /** Radial growth per tick in blocks. */
    private const val GROWTH_PER_TICK = 0.5

    /** Damage in hearts dealt to entities caught at the wave's edge (before level scaling). */
    private const val DAMAGE = 1.75

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.3

    /** Vertical launch applied to entities hit by the wave. */
    private const val KNOCKUP = 0.7

    /** Horizontal knockback applied to entities hit by the wave. */
    private const val KNOCKBACK = 0.8

    /** Ticks between task updates. */
    private const val TICK_PERIOD = 1L

    /** Number of radial particle rays emitted around the ring. */
    private const val RING_RAYS = 32

    /** Active waves keyed by player UUID. */
    private val sessions = ConcurrentHashMap<UUID, Session>()

    /** Holds state for a single expanding Shockwave. */
    private class Session(
        val player: Player,
        val level: Int,
        var radius: Double = 0.0,
        val hitEntities: MutableSet<java.util.UUID> = mutableSetOf(),
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

        val world = player.world
        world.playSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.6f)
        world.playSound(player.location, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.7f)

        session.task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { tick(uuid) },
                0L,
                TICK_PERIOD,
            )
    }

    /** Advances a Shockwave one tick. */
    private fun tick(uuid: UUID) {
        val session = sessions[uuid] ?: return
        val player = session.player

        if (!player.isOnline || player.isDead) {
            remove(uuid)
            return
        }

        val maxRadius = RADIUS + (session.level - 1) * RADIUS_PER_LEVEL
        session.radius += GROWTH_PER_TICK
        if (session.radius > maxRadius) {
            remove(uuid)
            return
        }

        render(session, player.location.clone())
        damage(session, player, maxRadius)
    }

    /** Renders the leading ring of earth particles at the current radius. */
    private fun render(
        session: Session,
        center: Location,
    ) {
        val world = center.world ?: return
        val data =
            if (session.radius.toInt() % 2 == 0) {
                Material.DIRT.createBlockData()
            } else {
                Material.COBBLESTONE.createBlockData()
            }

        for (i in 0 until RING_RAYS) {
            val angle = i * (2.0 * Math.PI / RING_RAYS)
            val x = cos(angle) * session.radius
            val z = sin(angle) * session.radius
            val point = center.clone().add(x, 0.15, z)
            world.spawnParticle(Particle.BLOCK, point, 2, 0.08, 0.15, 0.08, 0.02, data)
            world.spawnParticle(Particle.SMOKE, point.clone().add(0.0, 0.1, 0.0), 1, 0.0, 0.0, 0.0, 0.0)
        }

        if (Math.random() < 1.0 / 6.0) {
            world.playSound(center, Sound.BLOCK_STONE_STEP, 0.7f, 0.8f)
        }
    }

    /**
     * Damages entities standing near the wave's leading edge, only once per entity.
     * Entities closer than the previous edge have already been hit.
     */
    private fun damage(
        session: Session,
        player: Player,
        maxRadius: Double,
    ) {
        val world = player.world
        val center = player.location.clone()
        val damage = DAMAGE + (session.level - 1) * DAMAGE_PER_LEVEL

        world
            .getNearbyEntities(center, maxRadius + 1.0, 2.5, maxRadius + 1.0)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { it.uniqueId !in session.hitEntities }
            .filter { entity ->
                val distance =
                    entity
                        .location
                        .toVector()
                        .subtract(center.toVector())
                        .setY(0.0)
                        .length()
                // Only strike entities near the leading edge.
                distance >= session.radius - GROWTH_PER_TICK * 2.0 &&
                    distance <= session.radius + 0.5 &&
                    entity.location.y >= center.y - 1.0 &&
                    entity.location.y <= center.y + 2.0
            }.forEach { entity ->
                session.hitEntities += entity.uniqueId
                entity.damage(damage, player)
                val away =
                    entity
                        .location
                        .toVector()
                        .subtract(center.toVector())
                        .setY(0.0)
                        .let { if (it.lengthSquared() < 1.0e-4) Vector(1.0, 0.0, 0.0) else it.normalize() }
                entity.velocity =
                    entity
                        .velocity
                        .add(away.multiply(KNOCKBACK))
                        .add(Vector(0.0, KNOCKUP, 0.0))
                world.spawnParticle(
                    Particle.BLOCK,
                    entity.location.clone().add(0.0, 0.5, 0.0),
                    10,
                    0.3,
                    0.3,
                    0.3,
                    0.05,
                    Material.DIRT.createBlockData(),
                )
            }
    }

    /** Cancels and removes a session. */
    private fun remove(uuid: UUID) {
        sessions.remove(uuid)?.task?.cancel()
    }
}
