package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

/**
 * Torrent: summon a surging wave of water that crashes forward from the caster.
 *
 * A wall of water particles and temporary water blocks rolls ahead of the player,
 * shoving entities along its face. The torrent travels for [RANGE] blocks, then
 * drains back out of existence. Water blocks left behind are automatically restored
 * after [DRAIN_AFTER_TICKS].
 *
 * Wave width, travel speed, and push strength scale with level.
 */
internal object Torrent : AbilityInterface {
    override val cooldownMs: Long = 8500
    override val activationType = ActivationTypeEnum.DURATION
    override val hasSession = true

    /** Wave travel range in blocks (before level scaling). */
    private const val RANGE = 10.0

    /** Additional range per level. */
    private const val RANGE_PER_LEVEL = 1.5

    /** Wave half-width in blocks (before level scaling). */
    private const val HALF_WIDTH = 2

    /** Additional half-width every other level. */
    private const val WIDTH_PER_TWO_LEVELS = 1

    /** Wave height in blocks. */
    private const val HEIGHT = 2

    /** How far the wave front advances per tick (before level scaling). */
    private const val SPEED = 0.7

    /** Additional speed per level. */
    private const val SPEED_PER_LEVEL = 0.1

    /** Horizontal push strength applied to entities touching the wave. */
    private const val PUSH = 0.9

    /** Vertical push component applied to entities. */
    private const val KNOCKUP = 0.2

    /** Damage in hearts dealt to entities directly run over. */
    private const val DAMAGE = 0.75

    /** Delay before temporary water blocks are drained. */
    private const val DRAIN_AFTER_TICKS = 30L

    /** Per-player torrent state for active sessions. */
    private val torrents = ConcurrentHashMap<UUID, TorrentData>()

    /** Holds state for an active Torrent. */
    private class TorrentData(
        val player: Player,
        val direction: Vector,
        val origin: Location,
        val level: Int,
        var ticks: Long = 0L,
        val placed: MutableList<Block> = mutableListOf(),
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) return

        val direction =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val origin = player.location.clone().add(direction.clone().multiply(1.5))

        torrents[uuid] = TorrentData(player, direction, origin, level)
        SessionManager.start(this, uuid)
        player.world.playSound(origin, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.7f)
    }

    override fun tickSession(uuid: UUID) {
        val data = torrents[uuid] ?: return
        val player = data.player

        data.ticks += 1L

        val range = RANGE + (data.level - 1) * RANGE_PER_LEVEL
        val speed = SPEED + (data.level - 1) * SPEED_PER_LEVEL
        val maxTicks = ceil(range / speed).toLong()

        if (!player.isOnline || player.isDead || data.ticks > maxTicks) {
            torrents.remove(uuid)
            SessionManager.remove(this, uuid)
            scheduleDrain(data)
            return
        }

        advance(data, speed)
        render(data)
        push(data)
    }

    /**
     * Moves the wave front forward; lays down temporary water blocks where the wave
     * passes through air.
     */
    private fun advance(
        data: TorrentData,
        speed: Double,
    ) {
        val world = data.origin.world ?: return
        val depth = data.ticks * speed
        val halfWidth = HALF_WIDTH + (data.level - 1) / 2 * WIDTH_PER_TWO_LEVELS
        val side = Vector(-data.direction.z, 0.0, data.direction.x)

        for (lateralIndex in -halfWidth..halfWidth) {
            for (vertical in 0 until HEIGHT) {
                val block =
                    world.getBlockAt(
                        (data.origin.x + data.direction.x * depth + side.x * lateralIndex).toInt(),
                        data.origin.blockY + vertical,
                        (data.origin.z + data.direction.z * depth + side.z * lateralIndex).toInt(),
                    )
                if (block.type == Material.AIR) {
                    block.type = Material.WATER
                    data.placed.add(block)
                } else if (!block.isPassable && block.type != Material.WATER) {
                    // Wave breaks against solid terrain; skip this cell.
                    continue
                }
            }
        }
    }

    /** Renders the wave front as a rolling wall of splash particles. */
    private fun render(data: TorrentData) {
        val world = data.origin.world ?: return
        val origin = data.origin
        val direction = data.direction
        val depth = data.ticks * (SPEED + (data.level - 1) * SPEED_PER_LEVEL)
        val halfWidth = HALF_WIDTH + (data.level - 1) / 2 * WIDTH_PER_TWO_LEVELS
        val side = Vector(-direction.z, 0.0, direction.x)

        for (lateralIndex in -halfWidth..halfWidth) {
            for (vertical in 0..HEIGHT) {
                val point =
                    origin
                        .clone()
                        .add(direction.clone().multiply(depth))
                        .add(side.clone().multiply(lateralIndex.toDouble()))
                        .add(0.0, vertical.toDouble() * 0.8, 0.0)

                world.spawnParticle(Particle.SPLASH, point, 3, 0.1, 0.1, 0.1, 0.0)
                if (Math.random() < 0.4) {
                    world.spawnParticle(Particle.BUBBLE, point, 2, 0.0, 0.0, 0.0, 0.0)
                }
                if (Math.random() < 0.15) {
                    world.spawnParticle(
                        Particle.FALLING_WATER,
                        point.clone().add(0.0, 0.5, 0.0),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                    )
                }
            }
        }

        if (Math.random() < 1.0 / 5.0) {
            world.playSound(
                origin.clone().add(direction.clone().multiply(depth)),
                Sound.ENTITY_PLAYER_SPLASH,
                0.8f,
                1.0f,
            )
        }
    }

    /** Pushes entities standing on or near the wave front along the travel direction. */
    private fun push(data: TorrentData) {
        val world = data.origin.world ?: return
        val origin = data.origin
        val direction = data.direction
        val depth = data.ticks * (SPEED + (data.level - 1) * SPEED_PER_LEVEL)
        val halfWidth = HALF_WIDTH + (data.level - 1) / 2 * WIDTH_PER_TWO_LEVELS
        val speed = SPEED + (data.level - 1) * SPEED_PER_LEVEL
        val center = origin.clone().add(direction.clone().multiply(depth))

        world
            .getNearbyEntities(center, halfWidth + 1.0, HEIGHT + 1.0, halfWidth + 1.0)
            .filterIsInstance<LivingEntity>()
            .filter { it != data.player }
            .forEach { entity ->
                entity.damage(DAMAGE, data.player)
                val push =
                    direction
                        .clone()
                        .multiply(PUSH * speed)
                        .add(Vector(0.0, KNOCKUP, 0.0))
                entity.velocity = entity.velocity.add(push)
            }
    }

    /** Drains temporary water blocks after the torrent has passed. */
    private fun scheduleDrain(data: TorrentData) {
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                data.placed.forEach { block ->
                    if (block.type == Material.WATER) {
                        block.type = Material.AIR
                    }
                }
            },
            DRAIN_AFTER_TICKS,
        )
    }
}
