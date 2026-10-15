package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
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

    /** Total tunnel length in blocks (before level scaling). */
    private const val LENGTH = 8

    /** Additional length in blocks per level. */
    private const val LENGTH_PER_LEVEL = 1

    /** Tunnel radius in blocks (circular cross-section half-extent). */
    private const val RADIUS = 1

    /** Ticks between successive per-segment carves, for a rolling dig animation. */
    private const val SEGMENT_INTERVAL_TICKS = 1L

    /** Active tunnel digs keyed by player UUID (currently used as a re-entry guard). */
    private val sessions = ConcurrentHashMap<UUID, BukkitTask>()

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val uuid = player.uniqueId
        if (sessions.containsKey(uuid)) return

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

        // Carve one block-length per tick, progressing outward.
        var segment = 1
        val taskHolder = arrayOfNulls<BukkitTask>(1)
        taskHolder[0] =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable {
                    if (segment > length) {
                        taskHolder[0]?.cancel()
                        sessions.remove(uuid)
                        return@Runnable
                    }
                    val center =
                        startBlock
                            .location
                            .clone()
                            .add(forward.clone().multiply(segment.toDouble()))
                    carve(world, center)
                    world.playSound(center, Sound.BLOCK_STONE_BREAK, 0.7f, 0.9f)
                    segment++
                },
                0L,
                SEGMENT_INTERVAL_TICKS,
            )
        taskHolder[0]?.let { sessions[uuid] = it }
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
