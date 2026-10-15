package org.xodium.illyriabending.abilities.water.active

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * IceCrawl: send a wave of frost crawling across the ground ahead of the caster.
 *
 * A widening band of ground in the caster's facing direction flash-freezes: water
 * becomes [Material.FROSTED_ICE], dirt and grass become [Material.ICE], and snow
 * deepens. Entities standing on the affected ground are damaged and slowed. The ice
 * decays back to air after [MELT_AFTER_TICKS].
 *
 * Band length, width, and damage scale with level.
 */
internal object IceCrawl : AbilityInterface {
    override val cooldownMs: Long = 5500
    override val activationType = ActivationType.INSTANT

    /** Band length in blocks ahead of the player (before level scaling). */
    private const val LENGTH = 6

    /** Additional length per level. */
    private const val LENGTH_PER_LEVEL = 1

    /** Band half-width in blocks (before level scaling). */
    private const val HALF_WIDTH = 1

    /** Additional half-width every other level. */
    private const val WIDTH_PER_TWO_LEVELS = 1

    /** Damage in hearts dealt to entities standing on the frozen ground. */
    private const val DAMAGE = 1.0

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** Slowness duration in ticks applied to entities on the ice. */
    private const val SLOWNESS_TICKS = 50

    /** Slowness amplifier (0 = Slowness I). */
    private const val SLOWNESS_AMPLIFIER = 0

    /** How long converted blocks remain before melting, in ticks. */
    private const val MELT_AFTER_TICKS = 60L

    /** Particles spawned per converted surface block. */
    private const val PARTICLES_PER_BLOCK = 2

    /** Active crawl sessions keyed by caster UUID. */
    private val sessions = ConcurrentHashMap<UUID, Boolean>()

    /** A single converted block: its previous material is tracked so it can melt back. */
    private data class Converted(
        val block: Block,
        val previous: Material,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        // Re-cast while a previous crawl is still active is silently ignored.
        if (sessions.put(player.uniqueId, true) != null) return

        val length = LENGTH + (level - 1) * LENGTH_PER_LEVEL
        val halfWidth = HALF_WIDTH + (level - 1) / 2 * WIDTH_PER_TWO_LEVELS
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL

        val converted = freeze(player, length, halfWidth)
        val center =
            player
                .location
                .clone()
                .add(
                    player
                        .location
                        .direction
                        .clone()
                        .setY(0)
                        .normalize()
                        .multiply(length / 2.0),
                )

        if (converted.isNotEmpty()) {
            player.world.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.7f)
            player.world.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.4f)
            punish(player, length, halfWidth, damage)
            scheduleMelt(player.uniqueId, converted)
            complete(player)
        } else {
            sessions.remove(player.uniqueId)
        }
    }

    /**
     * Walks a rectangular band from the player forward and converts eligible surface
     * blocks. Returns the converted cells.
     */
    private fun freeze(
        player: Player,
        length: Int,
        halfWidth: Int,
    ): List<Converted> {
        val world = player.world
        val forward =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val side = Vector(-forward.z, 0.0, forward.x)

        val converted = mutableListOf<Converted>()
        for (depth in 1..length) {
            // Widen the band progressively as it advances.
            val width = (halfWidth * depth / length.toDouble()).toInt() + 1
            for (lateral in -width..width) {
                val base =
                    player
                        .location
                        .clone()
                        .add(forward.clone().multiply(depth.toDouble()))
                        .add(side.clone().multiply(lateral.toDouble()))

                // Find the surface block: probe downward through a few layers.
                var probeY = base.blockY + 1
                var surface: Block? = null
                repeat(4) {
                    val candidate = world.getBlockAt(base.blockX, probeY, base.blockZ)
                    if (!candidate.isPassable && candidate.type != Material.WATER && surface == null) {
                        surface = candidate
                    } else if (candidate.type == Material.WATER && surface == null) {
                        surface = candidate
                    }
                    probeY--
                }
                val target = surface ?: continue
                val previous = target.type

                val replacement =
                    when (previous) {
                        Material.WATER -> Material.FROSTED_ICE
                        Material.ICE -> Material.PACKED_ICE
                        Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.DIRT_PATH -> Material.ICE
                        Material.SAND -> Material.SNOW_BLOCK
                        else -> null
                    } ?: continue

                target.type = replacement
                converted.add(Converted(target, previous))

                world.spawnParticle(
                    Particle.SNOWFLAKE,
                    target.location.clone().add(0.5, 1.0, 0.5),
                    PARTICLES_PER_BLOCK,
                    0.3,
                    0.05,
                    0.3,
                    0.0,
                )
            }
        }
        return converted
    }

    /**
     * Damages and slows entities standing on or near the frozen band.
     */
    private fun punish(
        player: Player,
        length: Int,
        halfWidth: Int,
        damage: Double,
    ) {
        val world = player.world
        val forward =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val mid =
            player
                .location
                .clone()
                .add(forward.clone().multiply(length / 2.0))

        world
            .getNearbyEntities(mid, length.toDouble(), 2.0, halfWidth + 1.0)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { entity ->
                val toEntity =
                    entity
                        .location
                        .toVector()
                        .subtract(player.location.toVector())
                        .setY(0.0)
                val along = toEntity.dot(forward)
                along in 0.5..length.toDouble()
            }.forEach { entity ->
                entity.damage(damage, player)
                entity.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.SLOWNESS,
                        SLOWNESS_TICKS,
                        SLOWNESS_AMPLIFIER,
                        false,
                        false,
                        true,
                    ),
                )
            }
    }

    /**
     * Melts all converted blocks back to their original materials after a delay.
     */
    private fun scheduleMelt(
        uuid: UUID,
        converted: List<Converted>,
    ) {
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                converted.forEach { convertedCell ->
                    val block = convertedCell.block
                    // Only revert blocks we converted — never stomp later changes.
                    if (block.type == Material.FROSTED_ICE ||
                        block.type == Material.PACKED_ICE ||
                        block.type == Material.ICE ||
                        block.type == Material.SNOW_BLOCK
                    ) {
                        block.world.spawnParticle(
                            Particle.BLOCK,
                            block.location.clone().add(0.5, 0.5, 0.5),
                            6,
                            0.3,
                            0.3,
                            0.3,
                            0.04,
                            block.blockData,
                        )
                        block.type = convertedCell.previous
                    }
                }
                sessions.remove(uuid)
            },
            MELT_AFTER_TICKS,
        )
    }
}
