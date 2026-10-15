package org.xodium.illyriabending.abilities.earth.sequence

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
 * EarthShards: shatter the ground and rain a storm of broken stone around the caster.
 *
 * A sequence ability. The caster slams both fists down and the earth answers with
 * a hail of rock shards: dozens of small stone projectiles burst outward in every
 * direction, arcing and falling, damaging everything they hit. The storm keeps
 * firing for [STORM_TICKS], then subsides.
 *
 * Shard count per volley, damage, and storm duration scale with level.
 */
internal object EarthShards : AbilityInterface {
    override val cooldownMs: Long = 15000
    override val activationType = ActivationTypeEnum.INSTANT
    override val hasSession = true

    /** Base shards per volley (before level scaling). */
    private const val SHARDS_PER_VOLLEY = 8

    /** Additional shards per volley per level. */
    private const val SHARDS_PER_LEVEL = 2

    /** Damage in hearts dealt per shard (before level scaling). */
    private const val DAMAGE = 0.8

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.1

    /** Base horizontal launch speed of a shard in blocks per tick. */
    private const val SHARD_SPEED = 0.7

    /** Upward launch applied so shards arc over the caster. */
    private const val SHARD_ARC = 0.45

    /** Maximum tick lifetime of a single shard. */
    private const val SHARD_LIFETIME_TICKS = 30

    /** Total storm duration in ticks. */
    private const val STORM_TICKS = 40L

    /** Additional storm ticks per level. */
    private const val STORM_TICKS_PER_LEVEL = 5L

    /** Ticks between volleys. */
    private const val VOLLEY_INTERVAL_TICKS = 5L

    /** Per-player storm state for active sessions. */
    private val storms = ConcurrentHashMap<UUID, StormData>()

    /** Holds state for a single EarthShards storm. */
    private class StormData(
        val player: Player,
        val level: Int,
        var ticks: Long = 0L,
        val shards: MutableList<Shard> = mutableListOf(),
    )

    /** A single hurtling rock shard. */
    private class Shard(
        val player: Player,
        origin: Location,
        velocity: Vector,
        val damage: Double,
    ) {
        /** Current shard location. */
        var location: Location = origin.clone()

        /** Per-tick velocity; gravity pulls it down over time. */
        var velocity: Vector = velocity.clone()

        var ticksAlive = 0

        /** Advances the shard one tick. Returns `false` when it should despawn. */
        fun update(): Boolean {
            if (++ticksAlive > SHARD_LIFETIME_TICKS) {
                impact()
                return false
            }

            // Pull the shard downward as it travels, like real debris.
            velocity = velocity.clone().add(Vector(0.0, -0.05, 0.0))
            location.add(velocity)

            val block = location.block
            if (!block.isPassable && !block.type.isAir) {
                impact()
                return false
            }
            if (block.isLiquid) {
                impact()
                return false
            }

            render()
            return !hit()
        }

        /** Renders trailing rock particles. */
        private fun render() {
            location.world?.spawnParticle(
                Particle.BLOCK,
                location,
                2,
                0.03,
                0.03,
                0.03,
                0.0,
                Material.COBBLESTONE.createBlockData(),
            )
        }

        /** Damages the first living entity the shard overlaps. */
        private fun hit(): Boolean {
            val world = location.world ?: return false
            val entity =
                world
                    .getNearbyEntities(location, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)
                    .filterIsInstance<LivingEntity>()
                    .firstOrNull { it != player }
                    ?: return false
            entity.damage(damage, player)
            entity.velocity = entity.velocity.add(velocity.clone().multiply(0.5).setY(0.15))
            impact()
            return true
        }

        /** Shatters the shard into particles at its final location. */
        private fun impact() {
            location.world?.spawnParticle(
                Particle.BLOCK,
                location,
                8,
                0.15,
                0.15,
                0.15,
                0.04,
                Material.COBBLESTONE.createBlockData(),
            )
            location.world?.playSound(location, Sound.BLOCK_STONE_HIT, 0.5f, 1.3f)
        }

        /** Collision radius for shard hit detection. */
        private companion object {
            const val HIT_RADIUS = 0.6
        }
    }

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) return

        storms[uuid] = StormData(player, level)
        SessionManager.start(this, uuid)

        player.world.playSound(player.location, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f)
        player.world.playSound(player.location, Sound.BLOCK_GRAVEL_BREAK, 0.9f, 0.7f)
    }

    override fun tickSession(uuid: UUID) {
        val data = storms[uuid] ?: return
        val player = data.player

        val maxTicks = STORM_TICKS + (data.level - 1) * STORM_TICKS_PER_LEVEL
        if (!player.isOnline || player.isDead || data.ticks > maxTicks) {
            storms.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(player)
            return
        }

        data.ticks += 1L

        if (data.ticks % VOLLEY_INTERVAL_TICKS == 0L) {
            spawnVolley(data)
        }

        val iterator = data.shards.iterator()
        while (iterator.hasNext()) {
            val shard = iterator.next()
            if (!shard.update()) {
                iterator.remove()
            }
        }
    }

    /**
     * Launches a fresh ring of shards outward from the caster in a circle. Each shard
     * has a small vertical arc so the volley clears the caster before raining down.
     */
    private fun spawnVolley(data: StormData) {
        val player = data.player
        val shardsInVolley = SHARDS_PER_VOLLEY + (data.level - 1) * SHARDS_PER_LEVEL
        val damage = DAMAGE + (data.level - 1) * DAMAGE_PER_LEVEL

        val origin = player.eyeLocation.clone().subtract(0.0, 0.5, 0.0)

        for (i in 0 until shardsInVolley) {
            val angle = i * (2.0 * Math.PI / shardsInVolley) + data.ticks * 0.1
            // Slight random jitter per shard so the storm feels organic.
            val jitter =
                Vector(
                    (Math.random() - 0.5) * 0.05,
                    (Math.random() - 0.5) * 0.05,
                    (Math.random() - 0.5) * 0.05,
                )
            val velocity =
                Vector(
                    cos(angle) * SHARD_SPEED,
                    SHARD_ARC,
                    sin(angle) * SHARD_SPEED,
                ).add(jitter)

            data.shards += Shard(player, origin, velocity, damage)
        }

        player.world.playSound(
            player.location,
            Sound.BLOCK_GRAVEL_BREAK,
            0.6f,
            0.8f,
        )
    }
}
