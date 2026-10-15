package org.xodium.illyriabending.abilities.earth.active

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

/**
 * Collapse: vanish the ground out from under a target ahead of the caster.
 *
 * The caster focuses on the patch of earth below a target a few blocks ahead and rips
 * it downward, opening a shallow pit. Anyone standing on the site drops in, taking
 * a small amount of impact damage from the jolt. After [REFORM_AFTER_TICKS], loose
 * earth surges back in to refill the hole.
 *
 * Pit depth, radius, and reform delay scale with level.
 */
internal object Collapse : AbilityInterface {
    override val cooldownMs: Long = 7000

    /** Horizontal distance ahead of the caster where the pit opens. */
    private const val SPAWN_DISTANCE = 4.0

    /** Maximum vertical search depth when locating the target ground surface. */
    private const val MAX_DEPTH = 8

    /** Base horizontal radius of the pit in blocks (before level scaling). */
    private const val RADIUS = 1

    /** Additional radius every two levels. */
    private const val RADIUS_PER_TWO_LEVELS = 1

    /** Base pit depth in blocks (before level scaling). */
    private const val DEPTH = 2

    /** Additional pit depth every two levels. */
    private const val DEPTH_PER_TWO_LEVELS = 1

    /** Damage in hearts dealt to entities caught in the collapse. */
    private const val DAMAGE = 1.0

    /** Additional damage per level. */
    private const val DAMAGE_PER_LEVEL = 0.25

    /** How long the pit stays open before refilling, in ticks. */
    private const val REFORM_AFTER_TICKS = 120L

    /** Additional reform delay in ticks per level. */
    private const val REFORM_PER_LEVEL = 30L

    override fun activate(
        player: Player,
        level: Int,
    ) {
        val world = player.world
        val forward =
            player
                .location
                .direction
                .clone()
                .setY(0)
                .normalize()
        val target =
            player
                .location
                .clone()
                .add(forward.multiply(SPAWN_DISTANCE))
        val ground = findGround(target) ?: return

        val radius = RADIUS + (level - 1) / 2 * RADIUS_PER_TWO_LEVELS
        val depth = DEPTH + (level - 1) / 2 * DEPTH_PER_TWO_LEVELS
        val damage = DAMAGE + (level - 1) * DAMAGE_PER_LEVEL
        val reformAfter = REFORM_AFTER_TICKS + (level - 1) * REFORM_PER_LEVEL

        val removed = carvePit(ground, radius, depth)
        if (removed.isEmpty()) return

        world.playSound(ground, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.7f)
        world.playSound(ground, Sound.BLOCK_STONE_BREAK, 0.9f, 0.9f)
        damageTrapped(player, ground, radius, depth, damage)
        scheduleReform(removed, reformAfter)
    }

    /**
     * Walks downward from [origin] until it finds a solid block; returns the location of
     * the first air block above it, or null if nothing is reachable.
     */
    private fun findGround(origin: Location): Location? {
        val world = origin.world ?: return null
        val base = origin.block
        if (!base.isPassable && !base.isLiquid) {
            return base.location.clone().add(0.0, 1.0, 0.0)
        }
        for (i in 0 until MAX_DEPTH) {
            val below = world.getBlockAt(base.x, base.y - i, base.z)
            if (!below.isPassable && !below.isLiquid) {
                return world.getBlockAt(base.x, base.y - i + 1, base.z).location
            }
        }
        return null
    }

    /**
     * Carves out a centered pit at [seat] by replacing earth-like blocks with air,
     * down to [depth]. Returns the blocks that were removed (paired with their original
     * [Material]) so they can later be reformed.
     */
    private fun carvePit(
        seat: Location,
        radius: Int,
        depth: Int,
    ): List<Pair<Block, Material>> {
        val world = seat.world ?: return emptyList()
        val removed = mutableListOf<Pair<Block, Material>>()

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                // Circular mask: ignore corners outside the radius.
                if (x * x + z * z > radius * radius + 1) continue
                for (y in 0 until depth) {
                    val block =
                        world.getBlockAt(
                            seat.blockX + x,
                            seat.blockY - y,
                            seat.blockZ + z,
                        )
                    // Only displace bendable earthen blocks.
                    if (!isEarthLike(block.type)) continue
                    removed.add(block to block.type)
                    block.type = Material.AIR
                }
            }
        }

        world.spawnParticle(
            Particle.BLOCK,
            seat.clone().add(0.5, 0.5, 0.5),
            40,
            radius + 0.5,
            0.5,
            radius + 0.5,
            0.08,
            Material.DIRT.createBlockData(),
        )
        return removed
    }

    /** Applies impact damage to entities caught in the collapse area. */
    private fun damageTrapped(
        player: Player,
        seat: Location,
        radius: Int,
        depth: Int,
        damage: Double,
    ) {
        val world = seat.world ?: return
        world
            .getNearbyEntities(seat, radius + 1.0, depth + 2.0, radius + 1.0)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { it.location.y >= seat.y - depth - 0.5 && it.location.y <= seat.y + 1.0 }
            .forEach { entity ->
                entity.damage(damage, player)
                entity.velocity = entity.velocity.add(Vector(0.0, -0.3, 0.0))
            }
    }

    /**
     * Returns true when [material] is a soft or bendable earth block suitable for
     * Collapse. Harder blocks like obsidian and bedrock are too rigid to rip out.
     */
    private fun isEarthLike(material: Material): Boolean =
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
            -> true

            else -> false
        }

    /**
     * Schedules the removed blocks to grow back after [delay] ticks. Cells that have
     * since been replaced by something else are left untouched.
     */
    private fun scheduleReform(
        removed: List<Pair<Block, Material>>,
        delay: Long,
    ) {
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                removed.forEach { (block, originalMaterial) ->
                    if (block.type.isAir) {
                        block.type = originalMaterial
                        block.world.spawnParticle(
                            Particle.BLOCK,
                            block.location.clone().add(0.5, 0.5, 0.5),
                            6,
                            0.3,
                            0.3,
                            0.3,
                            0.04,
                            originalMaterial.createBlockData(),
                        )
                    }
                }
                removed
                    .firstOrNull()
                    ?.first
                    ?.let { first ->
                        first.world.playSound(first.location, Sound.BLOCK_GRAVEL_PLACE, 0.8f, 0.8f)
                    }
            },
            delay,
        )
    }
}
