package org.xodium.illyriabending.abilities.earth.passive

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.xodium.illyriabending.abilities.AbilityInterface
import org.xodium.illyriabending.abilities.ActivationType

/**
 * FerroControl: tug every stray scrap of metal toward the caster like a lodestone.
 *
 * Passively, the metalbender's aura reaches out for iron, gold, and other forged
 * metals left on the battlefield. On activation, all dropped metal items within
 * range are plucked out of the ground and flung gently toward the caster so they
 * can be swept into the inventory on the way past.
 *
 * Magnet radius and tug speed scale with level.
 */
internal object FerroControl : AbilityInterface {
    override val cooldownMs: Long = 5000
    override val activationType = ActivationType.INSTANT

    /** Base pickup radius in blocks (before level scaling). */
    private const val RADIUS = 8.0

    /** Additional radius per level. */
    private const val RADIUS_PER_LEVEL = 2.0

    /** Tug speed applied to each metal item. */
    private const val TUG_SPEED = 0.9

    /** Vertical nudge applied so items hop off the ground before flying. */
    private const val LIFT = 0.25

    override fun activate(
        player: Player,
        level: Int,
    ) {
        if (isOnCooldown(player)) return

        val world = player.world
        val center = player.location.clone()
        val radius = RADIUS + (level - 1) * RADIUS_PER_LEVEL

        var attracted = 0
        world
            .getNearbyEntities(center, radius, radius * 0.75, radius)
            .filterIsInstance<Item>()
            .filter { isMetal(it.itemStack.type) }
            .forEach { item ->
                val direction =
                    center
                        .clone()
                        .add(0.0, 1.0, 0.0)
                        .toVector()
                        .subtract(item.location.toVector())
                        .let { if (it.lengthSquared() < 1.0e-4) Vector(0.0, 0.5, 0.0) else it.normalize() }
                item.velocity =
                    direction
                        .multiply(TUG_SPEED)
                        .add(Vector(0.0, LIFT, 0.0))
                world.spawnParticle(
                    Particle.CRIT,
                    item.location.clone().add(0.0, 0.2, 0.0),
                    6,
                    0.15,
                    0.15,
                    0.15,
                    0.04,
                )
                attracted++
            }

        if (attracted > 0) {
            world.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.9f, 1.5f)
            world.playSound(center, Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.8f, 1.3f)
        }
        complete(player)
    }

    /**
     * Returns true when [material] is a forged metal or metalbearing item that the
     * metalbender can tug. Excludes raw or stone-like materials and weapons that
     * would be awkward to fling.
     */
    private fun isMetal(material: Material): Boolean =
        when (material) {
            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.COPPER_INGOT,
            Material.NETHERITE_INGOT,
            Material.IRON_NUGGET,
            Material.GOLD_NUGGET,
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.COPPER_BLOCK,
            Material.NETHERITE_BLOCK,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.RAW_IRON,
            Material.RAW_GOLD,
            Material.RAW_COPPER,
            Material.RAW_IRON_BLOCK,
            Material.RAW_GOLD_BLOCK,
            Material.RAW_COPPER_BLOCK,
            Material.IRON_CHAIN,
            Material.IRON_BARS,
            Material.IRON_DOOR,
            Material.IRON_TRAPDOOR,
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL,
            Material.HOPPER,
            Material.CAULDRON,
            Material.MINECART,
            Material.BUCKET,
            Material.LAVA_BUCKET,
            Material.WATER_BUCKET,
            Material.IRON_SWORD,
            Material.IRON_AXE,
            Material.IRON_PICKAXE,
            Material.IRON_SHOVEL,
            Material.IRON_HOE,
            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS,
            Material.GOLDEN_SWORD,
            Material.GOLDEN_AXE,
            Material.GOLDEN_PICKAXE,
            Material.GOLDEN_SHOVEL,
            Material.GOLDEN_HOE,
            Material.GOLDEN_HELMET,
            Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_LEGGINGS,
            Material.GOLDEN_BOOTS,
            Material.NETHERITE_SWORD,
            Material.NETHERITE_AXE,
            Material.NETHERITE_PICKAXE,
            Material.NETHERITE_SHOVEL,
            Material.NETHERITE_HOE,
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS,
            Material.SHEARS,
            Material.FLINT_AND_STEEL,
            Material.COMPASS,
            Material.CLOCK,
            Material.RECOVERY_COMPASS,
            Material.TRIPWIRE_HOOK,
            Material.CROSSBOW,
            Material.BELL,
            Material.LANTERN,
            Material.SOUL_LANTERN,
            Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Material.SHIELD,
            -> true

            else -> false
        }
}
