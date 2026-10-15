package org.xodium.illyriabending.abilities.earth.active

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.enums.ActivationTypeEnum

/**
 * LavaFlux: briefly transmute a patch of ground into roiling magma.
 *
 * The caster stirs the earth a few blocks ahead, melting stone, cobblestone, and
 * packed dirt into lava. Entities caught in the flux are burned and ignited while
 * the melt holds; once [REFORM_AFTER_TICKS] has passed, the lava cools back into its
 * original material as dark steam rises from the cracks.
 *
 * Flux radius and reform delay scale with level.
 */
internal object LavaFlux : AbilityInterface {
    override val cooldownMs: Long = 12000
    override val activationType = ActivationTypeEnum.INSTANT

    /** Horizontal distance ahead of the caster where the flux is centered. */
    private const val SPAWN_DISTANCE = 4.0

    /** Maximum vertical search depth when locating the target ground surface. */
    private const val MAX_DEPTH = 6

    /** Base horizontal radius of the flux site in blocks (before level scaling). */
    private const val RADIUS = 1

    /** Additional radius every two levels. */
    private const val RADIUS_PER_TWO_LEVELS = 1

    /** Damage per tick applied to entities standing in the lava before it reforms. */
    private const val DAMAGE_PER_TICK = 0.4

    /** Fire ticks applied to entities touching the flux. */
    private const val FIRE_TICKS = 60

    /** How long the flux stays molten before cooling, in ticks. */
    private const val REFORM_AFTER_TICKS = 80L

    /** Additional reform delay in ticks per level. */
    private const val REFORM_PER_LEVEL = 15L

    /** Ticks between damaging pulses while the flux is active. */
    private const val PULSE_TICKS = 10L

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

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
        val reformAfter = REFORM_AFTER_TICKS + (level - 1) * REFORM_PER_LEVEL

        val transmuted = transmute(ground, radius)
        if (transmuted.isEmpty()) return

        world.playSound(ground, Sound.BLOCK_LAVA_AMBIENT, 1.2f, 0.7f)
        world.playSound(ground, Sound.BLOCK_LAVA_POP, 1.0f, 0.8f)
        world.spawnParticle(
            Particle.LAVA,
            ground.clone().add(0.5, 0.5, 0.5),
            30,
            radius + 0.5,
            0.4,
            radius + 0.5,
            0.06,
        )

        // Periodically scorch anything standing in the flux.
        var pulses = (reformAfter / PULSE_TICKS).coerceAtLeast(1)
        val pulseTaskHolder = arrayOfNulls<BukkitTask>(1)
        pulseTaskHolder[0] =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable {
                    if (pulses-- <= 0) {
                        pulseTaskHolder[0]?.cancel()
                        return@Runnable
                    }
                    damageTrapped(player, ground, radius)
                },
                PULSE_TICKS,
                PULSE_TICKS,
            )

        scheduleReform(transmuted, reformAfter)
        complete(player)
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
     * Converts [radius]-circular stone/dirt/gravel/cobble tiles around [seat] into lava,
     * returning the affected blocks paired with their original material.
     */
    private fun transmute(
        seat: Location,
        radius: Int,
    ): List<Pair<Block, Material>> {
        val world = seat.world ?: return emptyList()
        val transmuted = mutableListOf<Pair<Block, Material>>()

        // Melt the ground layer just below the seat, so it visibly turns to lava.
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (x * x + z * z > radius * radius + 1) continue
                val block =
                    world.getBlockAt(
                        seat.blockX + x,
                        seat.blockY - 1,
                        seat.blockZ + z,
                    )
                if (!isMeltable(block.type)) continue
                transmuted += block to block.type
                block.type = Material.LAVA
            }
        }
        return transmuted
    }

    /** Applies burn damage and ignition to entities standing over the flux. */
    private fun damageTrapped(
        player: Player,
        seat: Location,
        radius: Int,
    ) {
        val world = seat.world ?: return
        world
            .getNearbyEntities(seat, radius + 1.0, 2.0, radius + 1.0)
            .filterIsInstance<LivingEntity>()
            .filter { it != player }
            .filter { it.location.y >= seat.y - 1.0 && it.location.y <= seat.y + 1.5 }
            .forEach { entity ->
                entity.damage(DAMAGE_PER_TICK, player)
                entity.fireTicks = maxOf(entity.fireTicks, FIRE_TICKS)
                // Pin them in the melt a little.
                entity.velocity = entity.velocity.multiply(0.5).add(Vector(0.0, -0.05, 0.0))
            }
    }

    /**
     * Returns true when [material] is an earthen block that LavaFlux can melt to lava.
     */
    private fun isMeltable(material: Material): Boolean =
        when (material) {
            Material.STONE,
            Material.COBBLESTONE,
            Material.MOSSY_COBBLESTONE,
            Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE,
            Material.TUFF,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE,
            Material.CALCITE,
            Material.DRIPSTONE_BLOCK,
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.COARSE_DIRT,
            Material.PODZOL,
            Material.ROOTED_DIRT,
            Material.MUD,
            Material.MUDDY_MANGROVE_ROOTS,
            Material.GRAVEL,
            Material.TERRACOTTA,
            -> true

            else -> false
        }

    /**
     * Schedules all transmuted blocks to cool back to their original material after
     * [delay] ticks. Cells that have since been replaced are left untouched.
     */
    private fun scheduleReform(
        transmuted: List<Pair<Block, Material>>,
        delay: Long,
    ) {
        instance.server.scheduler.runTaskLater(
            instance,
            Runnable {
                transmuted.forEach { (block, originalMaterial) ->
                    if (block.type == Material.LAVA) {
                        block.type = originalMaterial
                        block.world.spawnParticle(
                            Particle.LARGE_SMOKE,
                            block.location.clone().add(0.5, 0.5, 0.5),
                            10,
                            0.3,
                            0.2,
                            0.3,
                            0.04,
                        )
                        block.world.playSound(block.location, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.2f)
                    }
                }
            },
            delay,
        )
    }
}
