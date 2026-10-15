package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * EarthTunnel: bore a horizontal shaft straight ahead through earth and stone.
 *
 * The caster sweeps their focus hand forward and the ground obligingly parts — a
 * cylinder of air unrolls through dirt, gravel, and stone for [LENGTH] blocks, wide
 * enough to walk through. Blocks that cannot be bent (obsidian, bedrock, ores) stop
 * the shaft early; un-displaced cells simply stand aside.
 *
 * Shaft length scales with level.
 */
internal object EarthTunnel : AbilityInterface {
    override val cooldownMs: Long = 8500
    override val activationType = ActivationType.INSTANT
    override val hasSession = true

    /** Total tunnel length in blocks (before level scaling). */
    private const val LENGTH = 8

    /** Additional length in blocks per level. */
    private const val LENGTH_PER_LEVEL = 1

    /** Tunnel radius in blocks (circular cross-section half-extent). */
    private const val RADIUS = 1

    /** Ticks between successive per-segment carves, for a rolling dig animation. */
    private const val SEGMENT_INTERVAL_TICKS = 1L

    /** Per-player dig state for active sessions. */
    private val digs = ConcurrentHashMap<UUID, DigData>()

    /** Holds state for a single EarthTunnel dig. */
    private class DigData(
        val player: Player,
        val length: Int,
        val forward: org.bukkit.util.Vector,
        val startBlock: Block,
        val world: World,
        var segment: Int = 1,
    )

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val uuid = player.uniqueId
        if (SessionManager.has(this, uuid)) return

        val world = player.world
        val forward =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()

        val length = LENGTH + (level - 1) * LENGTH_PER_LEVEL
        val startBlock = player.location.block

        world.playSound(player.location, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.8f)

        digs[uuid] = DigData(player, length, forward, startBlock, world)
        SessionManager.start(this, uuid, SEGMENT_INTERVAL_TICKS)
    }

    override fun tickSession(uuid: UUID) {
        val data = digs[uuid] ?: return

        if (data.segment > data.length) {
            digs.remove(uuid)
            SessionManager.remove(this, uuid)
            complete(data.player)
            return
        }

        val center =
            data
                .startBlock
                .location
                .clone()
                .add(data.forward.clone().multiply(data.segment.toDouble()))
        carve(data.world, center)
        data.world.playSound(center, Sound.BLOCK_STONE_BREAK, 0.7f, 0.9f)
        data.segment++
    }

    /**
     * Clears a circular cross-section of bendable earth and stone at [center], leaving
     * everything non-bendable standing so the tunnel frames naturally.
     */
    private fun carve(
        world: World,
        center: Location,
    ) {
        val dirtData = Material.DIRT.createBlockData()
        for (x in -RADIUS..RADIUS) {
            for (y in -RADIUS..RADIUS) {
                for (z in -RADIUS..RADIUS) {
                    if (x * x + y * y + z * z > RADIUS * RADIUS + 1) continue
                    val block: Block =
                        world.getBlockAt(
                            center.blockX + x,
                            center.blockY + y,
                            center.blockZ + z,
                        )
                    if (!isBendable(block.type)) continue
                    world.spawnParticle(
                        Particle.BLOCK,
                        block.location.clone().add(0.5, 0.5, 0.5),
                        8,
                        0.3,
                        0.3,
                        0.3,
                        0.06,
                        dirtData,
                    )
                    block.type = Material.AIR
                }
            }
        }
    }

    /**
     * Returns true when [material] is a bendable earthlike block that EarthTunnel can
     * clear. Harder materials, valuable ores, and indestructibles do not yield.
     */
    private fun isBendable(material: Material): Boolean =
        when (material) {
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.COARSE_DIRT,
            Material.PODZOL,
            Material.ROOTED_DIRT,
            Material.MUD,
            Material.MUDDY_MANGROVE_ROOTS,
            Material.STONE,
            Material.COBBLESTONE,
            Material.MOSSY_COBBLESTONE,
            Material.GRAVEL,
            Material.SAND,
            Material.RED_SAND,
            Material.SANDSTONE,
            Material.RED_SANDSTONE,
            Material.CLAY,
            Material.TERRACOTTA,
            Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE,
            Material.TUFF,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE,
            Material.CALCITE,
            Material.DRIPSTONE_BLOCK,
            Material.NETHERRACK,
            Material.SOUL_SAND,
            Material.SOUL_SOIL,
            Material.MYCELIUM,
            Material.SNOW_BLOCK,
            Material.POWDER_SNOW,
            Material.DIRT_PATH,
            Material.FARMLAND,
            -> true

            else -> false
        }
}
