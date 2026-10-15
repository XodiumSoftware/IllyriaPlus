package org.xodium.illyriabending.abilities.earth.passive

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Openable
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface

/**
 * Locksmithing: pry open barred metal fittings with a flick of the wrist.
 *
 * Passively, a seasoned metalbender can worry at the iron pins in doors, trapdoors,
 * and similar fastenings until they tumble open. On activation, the caster twists the
 * first openable metal block within their line of sight — iron doors and trapdoors
 * clang open or shut, without needing a key or a redstone pulse.
 *
 * Range scales with level.
 */
internal object Locksmithing : AbilityInterface {
    override val cooldownMs: Long = 2500

    /** Maximum line-of-sight range in blocks (before level scaling). */
    private const val RANGE = 6.0

    /** Additional range per level. */
    private const val RANGE_PER_LEVEL = 1.0

    /** Raycast step when sweeping along the gaze. */
    private const val STEP = 0.4

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val world = player.world
        val origin = player.eyeLocation.clone()
        val direction = origin.direction.clone().normalize()
        val range = RANGE + (level - 1) * RANGE_PER_LEVEL

        val target =
            traceOpenable(origin, direction, range) ?: run {
                // Nothing metal to pick this time.
                world.playSound(origin, Sound.BLOCK_CHAIN_FALL, 0.6f, 1.4f)
                return
            }

        toggle(target)
        renderUnlock(target.location)
    }

    /**
     * Marches along the gaze ray until it finds a metal block we can pick, returning it,
     * or null if nothing bendable was in reach.
     */
    private fun traceOpenable(
        origin: Location,
        direction: Vector,
        range: Double,
    ): Block? {
        val world = origin.world ?: return null
        var d = STEP
        while (d <= range) {
            val point = origin.clone().add(direction.clone().multiply(d))
            val block = point.block
            if (isLockable(block.type)) return block
            if (!block.isPassable && !block.isLiquid && !block.type.isAir) return null
            d += STEP
        }
        return null
    }

    /**
     * Toggles the openable state of a metal block: opens it if shut, closes it if open.
     * Blocks whose [Block.getBlockData] isn't [Openable] are ignored.
     */
    private fun toggle(block: Block) {
        val data = block.blockData
        if (data !is Openable) return
        data.isOpen = !data.isOpen
        block.blockData = data

        val world = block.world
        world.playSound(
            block.location,
            if (data.isOpen) Sound.BLOCK_IRON_DOOR_OPEN else Sound.BLOCK_IRON_DOOR_CLOSE,
            1.0f,
            1.0f,
        )
        world.playSound(block.location, Sound.BLOCK_CHAIN_HIT, 0.7f, 1.3f)
    }

    /** Renders a spray of iron filings around the block as the pins give. */
    private fun renderUnlock(center: Location) {
        val world = center.world ?: return
        world.spawnParticle(
            Particle.CRIT,
            center.clone().add(0.5, 0.5, 0.5),
            18,
            0.3,
            0.5,
            0.3,
            0.06,
        )
        world.spawnParticle(
            Particle.BLOCK,
            center.clone().add(0.5, 0.5, 0.5),
            12,
            0.3,
            0.5,
            0.3,
            0.05,
            Material.IRON_BLOCK.createBlockData(),
        )
    }

    /**
     * Returns true when [material] is a metal fastening that Locksmithing can pick —
     * iron doors, trapdoors, and any other openable iron fitting.
     */
    private fun isLockable(material: Material): Boolean =
        when (material) {
            Material.IRON_DOOR,
            Material.IRON_TRAPDOOR,
            Material.COPPER_DOOR,
            Material.EXPOSED_COPPER_DOOR,
            Material.WEATHERED_COPPER_DOOR,
            Material.OXIDIZED_COPPER_DOOR,
            Material.WAXED_COPPER_DOOR,
            Material.WAXED_EXPOSED_COPPER_DOOR,
            Material.WAXED_WEATHERED_COPPER_DOOR,
            Material.WAXED_OXIDIZED_COPPER_DOOR,
            Material.COPPER_TRAPDOOR,
            Material.EXPOSED_COPPER_TRAPDOOR,
            Material.WEATHERED_COPPER_TRAPDOOR,
            Material.OXIDIZED_COPPER_TRAPDOOR,
            Material.WAXED_COPPER_TRAPDOOR,
            Material.WAXED_EXPOSED_COPPER_TRAPDOOR,
            Material.WAXED_WEATHERED_COPPER_TRAPDOOR,
            Material.WAXED_OXIDIZED_COPPER_TRAPDOOR,
            -> true

            else -> false
        }
}
