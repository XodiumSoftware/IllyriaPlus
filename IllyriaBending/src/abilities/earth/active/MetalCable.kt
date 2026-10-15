package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum

/**
 * MetalCable: whip a chain of iron up to a ledge and reel yourself to it.
 *
 * The caster snaps a flexible metal cable toward the block they're looking at. If the
 * cable finds a solid anchor within range, the caster is yanked toward it with a
 * powerful burst; the cable cracks back into its pouch with a metallic hiss. Airborne
 * landings get a small slow-fall so the caster doesn't crater on arrival.
 *
 * Cable range and reel-in speed scale with level.
 */
internal object MetalCable : AbilityInterface {
    override val cooldownMs: Long = 4500
    override val activationType = ActivationTypeEnum.INSTANT

    /** Maximum cable reach in blocks (before level scaling). */
    private const val RANGE = 20.0

    /** Additional range per level. */
    private const val RANGE_PER_LEVEL = 3.0

    /** Reel velocity magnitude applied toward the anchor (before level scaling). */
    private const val REEL_SPEED = 1.1

    /** Additional reel speed per level. */
    private const val REEL_PER_LEVEL = 0.12

    /** Raycast step used when sweeping for an anchor block. */
    private const val STEP = 0.4

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val world = player.world
        val origin = player.eyeLocation.clone()
        val direction = origin.direction.clone().normalize()

        val range = RANGE + (level - 1) * RANGE_PER_LEVEL
        val reelSpeed = REEL_SPEED + (level - 1) * REEL_PER_LEVEL

        world.playSound(origin, Sound.BLOCK_CHAIN_PLACE, 0.9f, 1.3f)

        val anchor = traceAnchor(origin, direction, range)
        if (anchor == null) {
            // The cable swings out and retracts harmlessly.
            world.playSound(origin, Sound.BLOCK_CHAIN_BREAK, 0.7f, 1.0f)
            return
        }

        renderCable(origin, anchor)
        world.playSound(anchor, Sound.BLOCK_CHAIN_HIT, 1.0f, 1.2f)

        // Yank the caster toward the anchor.
        val pull =
            anchor
                .clone()
                .subtract(origin)
                .toVector()
                .normalize()
                .multiply(reelSpeed)
        player.velocity = pull

        world.spawnParticle(
            Particle.CRIT,
            anchor.clone().add(0.5, 0.5, 0.5),
            12,
            0.2,
            0.2,
            0.2,
            0.05,
        )
        complete(player)
    }

    /**
     * Marches a ray from [origin] along [direction] until it hits a solid block;
     * returns the anchor location, or null if nothing solid was found within range.
     */
    private fun traceAnchor(
        origin: Location,
        direction: Vector,
        range: Double,
    ): Location? {
        val world = origin.world ?: return null
        var d = STEP
        while (d <= range) {
            val point = origin.clone().add(direction.clone().multiply(d))
            val block = point.block
            // Anchor must be something solid enough to hold weight.
            if (!block.isPassable && !block.isLiquid && !block.type.isAir) {
                // Only metal-friendly anchors: ore, metal blocks, stone, and glass.
                if (isAnchorable(block.type)) {
                    return block.location.clone()
                }
                return null
            }
            d += STEP
        }
        return null
    }

    /**
     * Returns true when [material] is a suitable anchor for the cable: metals, ores,
     * stone, and glass. Other solids don't take the hook firmly enough.
     */
    private fun isAnchorable(material: Material): Boolean =
        when (material) {
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.EMERALD_BLOCK,
            Material.LAPIS_BLOCK,
            Material.REDSTONE_BLOCK,
            Material.NETHERITE_BLOCK,
            Material.COPPER_BLOCK,
            Material.IRON_CHAIN,
            Material.IRON_BARS,
            Material.IRON_DOOR,
            Material.IRON_TRAPDOOR,
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL,
            Material.HOPPER,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.STONE,
            Material.COBBLESTONE,
            Material.MOSSY_COBBLESTONE,
            Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE,
            Material.GLASS,
            Material.TINTED_GLASS,
            Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN,
            Material.BEDROCK,
            -> true

            else -> false
        }

    /**
     * Draws a spiraling cable of chain-colored particles between two points.
     */
    private fun renderCable(
        origin: Location,
        anchor: Location,
    ) {
        val world = origin.world ?: return
        val totalDist = origin.distance(anchor)
        if (totalDist <= 0.01) return
        val dir =
            anchor
                .clone()
                .subtract(origin)
                .toVector()
                .normalize()

        val ironData = Material.IRON_BLOCK.createBlockData()
        var t = 0.0
        var twist = 0.0
        while (t <= totalDist) {
            val base = origin.clone().add(dir.clone().multiply(t))
            twist += 0.5
            val ox = kotlin.math.cos(twist) * 0.1
            val oz = kotlin.math.sin(twist) * 0.1
            world.spawnParticle(Particle.BLOCK, base.clone().add(ox, 0.0, oz), 1, 0.02, 0.02, 0.02, 0.0, ironData)
            t += 0.4
        }
    }
}
