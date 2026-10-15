package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.SessionManager
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
    override val activationType = ActivationTypeEnum.INSTANT
    override val hasSession = true

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

    /** Number of radial particle rays emitted around the ring. */
    private const val RING_RAYS = 32

    /** Per-player wave state for active sessions. */
    private val waves = ConcurrentHashMap<UUID, WaveData>()

    /** Holds state for a single expanding Shockwave. */
    private class WaveData(
        val player: Player,
        val level: Int,
        var radius: Double = 0.0,
        val hitEntities: MutableSet<UUID> = mutableSetOf(),
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) return

        waves[uuid] = WaveData(player, level)
        SessionManager.start(this, uuid)

        val world = player.world
        world.playSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.6f)
        world.playSound(player.location, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.7f)
    }

    override fun tickSession(uuid: UUID) {
        val data = waves[uuid] ?: return
        val player = data.player

        if (!player.isOnline || player.isDead) {
            waves.remove(uuid)
            SessionManager.remove(this, uuid)
            return
        }

        val maxRadius = RADIUS + (data.level - 1) * RADIUS_PER_LEVEL
        data.radius += GROWTH_PER_TICK
        if (data.radius > maxRadius) {
            waves.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        render(data, player.location.clone())
        damage(data, player, maxRadius)
    }

    /** Renders the leading ring of earth particles at the current radius. */
    private fun render(
        data: WaveData,
        center: Location,
    ) {
        val world = center.world ?: return
        val blockData =
            if (data.radius.toInt() % 2 == 0) {
                Material.DIRT.createBlockData()
            } else {
                Material.COBBLESTONE.createBlockData()
            }

        for (i in 0 until RING_RAYS) {
            val angle = i * (2.0 * Math.PI / RING_RAYS)
            val x = cos(angle) * data.radius
            val z = sin(angle) * data.radius
            val point = center.clone().add(x, 0.15, z)
            world.spawnParticle(Particle.BLOCK, point, 2, 0.08, 0.15, 0.08, 0.02, blockData)
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
        data: WaveData,
        player: Player,
        maxRadius: Double,
    ) {
        val world = player.world
        val center = player.location.clone()
        val damage = DAMAGE + (data.level - 1) * DAMAGE_PER_LEVEL

        world
            .getNearbyEntities(center, maxRadius + 1.0, 2.5, maxRadius + 1.0)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { it.uniqueId !in data.hitEntities }
            .filter { entity ->
                val distance =
                    entity
                        .location
                        .toVector()
                        .subtract(center.toVector())
                        .setY(0.0)
                        .length()
                // Only strike entities near the leading edge.
                distance >= data.radius - GROWTH_PER_TICK * 2.0 &&
                    distance <= data.radius + 0.5 &&
                    entity.location.y >= center.y - 1.0 &&
                    entity.location.y <= center.y + 2.0
            }.forEach { entity ->
                data.hitEntities += entity.uniqueId
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
}
