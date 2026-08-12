package org.xodium.illyriaplus.mechanics.player

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.Schedule.schedule
import org.xodium.illyriaplus.mechanics.MechanicInterface

/** Applies a carrying-weight system to players based on their inventory contents. */
internal object WeightMechanic : MechanicInterface {
    private const val CHECK_INTERVAL_TICKS = 20L
    private const val EFFECT_DURATION_TICKS = 40

    private const val LIGHT_THRESHOLD = 50
    private const val MEDIUM_THRESHOLD = 150
    private const val HEAVY_THRESHOLD = 300
    private const val OVERBURDENED_THRESHOLD = 500

    private val weights =
        sortedMapOf(
            // Air / negligible
            Material.AIR to 0.0,
            // Natural light items
            Material.FEATHER to 0.1,
            Material.WHEAT to 0.2,
            Material.WHEAT_SEEDS to 0.1,
            Material.CARROT to 0.2,
            Material.POTATO to 0.2,
            Material.BEETROOT to 0.2,
            Material.BEETROOT_SEEDS to 0.1,
            Material.MELON_SLICE to 0.3,
            Material.PUMPKIN to 1.0,
            Material.MELON to 1.0,
            Material.SUGAR_CANE to 0.2,
            Material.BAMBOO to 0.2,
            Material.KELP to 0.2,
            Material.LEAF_LITTER to 0.1,
            // Wood / organic
            Material.OAK_LOG to 2.0,
            Material.SPRUCE_LOG to 2.0,
            Material.BIRCH_LOG to 2.0,
            Material.JUNGLE_LOG to 2.0,
            Material.ACACIA_LOG to 2.0,
            Material.DARK_OAK_LOG to 2.0,
            Material.MANGROVE_LOG to 2.0,
            Material.CHERRY_LOG to 2.0,
            Material.PALE_OAK_LOG to 2.0,
            Material.OAK_PLANKS to 1.0,
            Material.SPRUCE_PLANKS to 1.0,
            Material.BIRCH_PLANKS to 1.0,
            Material.JUNGLE_PLANKS to 1.0,
            Material.ACACIA_PLANKS to 1.0,
            Material.DARK_OAK_PLANKS to 1.0,
            Material.MANGROVE_PLANKS to 1.0,
            Material.CHERRY_PLANKS to 1.0,
            Material.PALE_OAK_PLANKS to 1.0,
            Material.STICK to 0.2,
            Material.COAL to 1.0,
            Material.CHARCOAL to 1.0,
            // Stone / building
            Material.STONE to 3.0,
            Material.COBBLESTONE to 3.0,
            Material.STONE_BRICKS to 3.0,
            Material.CRACKED_STONE_BRICKS to 3.0,
            Material.MOSSY_STONE_BRICKS to 3.0,
            Material.DEEPSLATE to 4.0,
            Material.COBBLED_DEEPSLATE to 4.0,
            Material.DEEPSLATE_BRICKS to 4.0,
            Material.DEEPSLATE_TILES to 4.0,
            Material.TUFF to 3.0,
            Material.TUFF_BRICKS to 3.0,
            Material.POLISHED_TUFF to 3.0,
            Material.BRICKS to 3.0,
            Material.MUD_BRICKS to 3.0,
            Material.SANDSTONE to 3.0,
            Material.RED_SANDSTONE to 3.0,
            Material.PRISMARINE to 3.0,
            Material.PRISMARINE_BRICKS to 3.0,
            Material.DARK_PRISMARINE to 3.0,
            Material.NETHER_BRICKS to 3.0,
            Material.RED_NETHER_BRICKS to 3.0,
            Material.BLACKSTONE to 3.0,
            Material.POLISHED_BLACKSTONE to 3.0,
            Material.POLISHED_BLACKSTONE_BRICKS to 3.0,
            Material.END_STONE to 3.0,
            Material.END_STONE_BRICKS to 3.0,
            Material.PURPUR_BLOCK to 3.0,
            // Ores / metals
            Material.COAL_ORE to 4.0,
            Material.IRON_ORE to 5.0,
            Material.COPPER_ORE to 5.0,
            Material.GOLD_ORE to 6.0,
            Material.REDSTONE_ORE to 4.0,
            Material.LAPIS_ORE to 4.0,
            Material.DIAMOND_ORE to 6.0,
            Material.EMERALD_ORE to 5.0,
            Material.NETHER_GOLD_ORE to 5.0,
            Material.NETHER_QUARTZ_ORE to 4.0,
            Material.ANCIENT_DEBRIS to 8.0,
            Material.RAW_IRON to 4.0,
            Material.RAW_COPPER to 4.0,
            Material.RAW_GOLD to 5.0,
            Material.IRON_INGOT to 3.0,
            Material.COPPER_INGOT to 3.0,
            Material.GOLD_INGOT to 4.0,
            Material.REDSTONE to 0.5,
            Material.LAPIS_LAZULI to 0.5,
            Material.DIAMOND to 2.0,
            Material.EMERALD to 2.0,
            Material.NETHERITE_INGOT to 5.0,
            Material.NETHERITE_SCRAP to 4.0,
            Material.QUARTZ to 1.0,
            Material.AMETHYST_SHARD to 0.5,
            Material.ECHO_SHARD to 1.0,
            // Tools / weapons / armor
            Material.WOODEN_PICKAXE to 1.0,
            Material.WOODEN_AXE to 1.0,
            Material.WOODEN_SHOVEL to 0.5,
            Material.WOODEN_HOE to 0.5,
            Material.WOODEN_SWORD to 1.0,
            Material.STONE_PICKAXE to 2.0,
            Material.STONE_AXE to 2.0,
            Material.STONE_SHOVEL to 1.5,
            Material.STONE_HOE to 1.5,
            Material.STONE_SWORD to 2.0,
            Material.IRON_PICKAXE to 3.0,
            Material.IRON_AXE to 3.0,
            Material.IRON_SHOVEL to 2.0,
            Material.IRON_HOE to 2.0,
            Material.IRON_SWORD to 3.0,
            Material.IRON_HELMET to 3.0,
            Material.IRON_CHESTPLATE to 5.0,
            Material.IRON_LEGGINGS to 4.0,
            Material.IRON_BOOTS to 2.0,
            Material.GOLDEN_PICKAXE to 2.0,
            Material.GOLDEN_AXE to 2.0,
            Material.GOLDEN_SHOVEL to 1.5,
            Material.GOLDEN_HOE to 1.5,
            Material.GOLDEN_SWORD to 2.0,
            Material.GOLDEN_HELMET to 3.0,
            Material.GOLDEN_CHESTPLATE to 5.0,
            Material.GOLDEN_LEGGINGS to 4.0,
            Material.GOLDEN_BOOTS to 2.0,
            Material.DIAMOND_PICKAXE to 4.0,
            Material.DIAMOND_AXE to 4.0,
            Material.DIAMOND_SHOVEL to 3.0,
            Material.DIAMOND_HOE to 3.0,
            Material.DIAMOND_SWORD to 4.0,
            Material.DIAMOND_HELMET to 4.0,
            Material.DIAMOND_CHESTPLATE to 7.0,
            Material.DIAMOND_LEGGINGS to 6.0,
            Material.DIAMOND_BOOTS to 3.0,
            Material.NETHERITE_PICKAXE to 5.0,
            Material.NETHERITE_AXE to 5.0,
            Material.NETHERITE_SHOVEL to 4.0,
            Material.NETHERITE_HOE to 4.0,
            Material.NETHERITE_SWORD to 5.0,
            Material.NETHERITE_HELMET to 6.0,
            Material.NETHERITE_CHESTPLATE to 9.0,
            Material.NETHERITE_LEGGINGS to 7.0,
            Material.NETHERITE_BOOTS to 5.0,
            // Food / misc
            Material.BREAD to 0.5,
            Material.APPLE to 0.3,
            Material.COOKED_BEEF to 0.5,
            Material.COOKED_PORKCHOP to 0.5,
            Material.COOKED_CHICKEN to 0.3,
            Material.BEEF to 0.5,
            Material.PORKCHOP to 0.5,
            Material.CHICKEN to 0.3,
            Material.ROTTEN_FLESH to 0.5,
            Material.BONE to 0.5,
            Material.LEATHER to 1.0,
            Material.STRING to 0.2,
            Material.GUNPOWDER to 0.5,
            Material.FLINT to 0.5,
            Material.CLAY_BALL to 0.5,
            Material.BOWL to 0.2,
            Material.BUCKET to 2.0,
            Material.WATER_BUCKET to 3.0,
            Material.LAVA_BUCKET to 3.0,
            Material.MILK_BUCKET to 3.0,
            Material.POWDER_SNOW_BUCKET to 3.0,
            Material.PUFFERFISH_BUCKET to 2.0,
            Material.SALMON_BUCKET to 2.0,
            Material.COD_BUCKET to 2.0,
            Material.TROPICAL_FISH_BUCKET to 2.0,
            Material.AXOLOTL_BUCKET to 2.0,
            Material.TADPOLE_BUCKET to 2.0,
            // Heavy / special
            Material.ANVIL to 20.0,
            Material.CHIPPED_ANVIL to 20.0,
            Material.DAMAGED_ANVIL to 20.0,
            Material.OBSIDIAN to 10.0,
            Material.CRYING_OBSIDIAN to 10.0,
            Material.ENCHANTING_TABLE to 15.0,
            Material.ENDER_CHEST to 15.0,
            Material.CHEST to 5.0,
            Material.TRAPPED_CHEST to 5.0,
            Material.BARREL to 5.0,
            Material.HOPPER to 8.0,
            Material.DROPPER to 5.0,
            Material.DISPENSER to 5.0,
            Material.FURNACE to 8.0,
            Material.BLAST_FURNACE to 8.0,
            Material.SMOKER to 8.0,
            Material.LOOM to 5.0,
            Material.CARTOGRAPHY_TABLE to 5.0,
            Material.SMITHING_TABLE to 5.0,
            Material.FLETCHING_TABLE to 5.0,
            Material.GRINDSTONE to 5.0,
            Material.STONECUTTER to 8.0,
            Material.BREWING_STAND to 5.0,
            Material.CAULDRON to 8.0,
            Material.BELL to 8.0,
            Material.BEACON to 15.0,
            Material.JUKEBOX to 8.0,
            Material.NOTE_BLOCK to 5.0,
            Material.PISTON to 5.0,
            Material.STICKY_PISTON to 5.0,
            Material.DISPENSER to 5.0,
            Material.DROPPER to 5.0,
            Material.HAY_BLOCK to 3.0,
            Material.SPONGE to 2.0,
            Material.WET_SPONGE to 5.0,
            Material.BOOKSHELF to 5.0,
            Material.CHISELED_BOOKSHELF to 5.0,
            Material.LECTERN to 5.0,
            Material.CAMPFIRE to 4.0,
            Material.SOUL_CAMPFIRE to 4.0,
            Material.COMPOSTER to 4.0,
            Material.BEEHIVE to 5.0,
            Material.BEE_NEST to 5.0,
            Material.TNT to 4.0,
            Material.SLIME_BLOCK to 3.0,
            Material.HONEY_BLOCK to 3.0,
            Material.MAGMA_BLOCK to 5.0,
            Material.GLOWSTONE to 3.0,
            Material.SEA_LANTERN to 3.0,
            Material.REDSTONE_LAMP to 3.0,
            Material.JACK_O_LANTERN to 3.0,
            Material.SHULKER_BOX to 8.0,
            Material.WHITE_SHULKER_BOX to 8.0,
            Material.LIGHT_GRAY_SHULKER_BOX to 8.0,
            Material.GRAY_SHULKER_BOX to 8.0,
            Material.BLACK_SHULKER_BOX to 8.0,
            Material.BROWN_SHULKER_BOX to 8.0,
            Material.RED_SHULKER_BOX to 8.0,
            Material.ORANGE_SHULKER_BOX to 8.0,
            Material.YELLOW_SHULKER_BOX to 8.0,
            Material.LIME_SHULKER_BOX to 8.0,
            Material.GREEN_SHULKER_BOX to 8.0,
            Material.CYAN_SHULKER_BOX to 8.0,
            Material.LIGHT_BLUE_SHULKER_BOX to 8.0,
            Material.BLUE_SHULKER_BOX to 8.0,
            Material.PURPLE_SHULKER_BOX to 8.0,
            Material.MAGENTA_SHULKER_BOX to 8.0,
            Material.PINK_SHULKER_BOX to 8.0,
        )

    private val trackedPlayers = mutableSetOf<Player>()

    override fun register(): Long {
        schedule(period = CHECK_INTERVAL_TICKS) { trackedPlayers.toList().forEach { update(it) } }
        return super.register()
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: PlayerJoinEvent) {
        trackedPlayers.add(event.player)
        update(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: PlayerQuitEvent) {
        trackedPlayers.remove(event.player)
    }

    /** Calculates total carrying weight and applies tiered potion effects. */
    private fun update(player: Player) {
        if (!player.isOnline) {
            trackedPlayers.remove(player)
            return
        }

        val weight = calculateWeight(player)
        applyEffects(player, weight)
    }

    /** Sums weight across inventory contents, armour, and held items. */
    private fun calculateWeight(player: Player): Double {
        var total = 0.0

        player.inventory.contents.filterNotNull().forEach { total += weightOf(it.type) * it.amount }
        player.inventory.armorContents.filterNotNull().forEach {
            total += weightOf(it.type) * it.amount
        }
        player.inventory.extraContents.filterNotNull().forEach {
            total += weightOf(it.type) * it.amount
        }

        return total
    }

    /** Returns the weight of a single unit of the given material. */
    private fun weightOf(material: Material): Double = weights[material] ?: defaultWeight(material)

    /** Heuristic fallback weight for materials not explicitly mapped. */
    private fun defaultWeight(material: Material): Double =
        when {
            material.isBlock -> 2.0
            material.isEdible -> 0.5
            material.isFuel -> 1.0
            else -> 0.5
        }

    /** Applies movement and action penalties based on weight thresholds. */
    private fun applyEffects(
        player: Player,
        weight: Double,
    ) {
        if (weight < LIGHT_THRESHOLD) {
            removeWeightEffects(player)
            return
        }

        val effects = mutableListOf<PotionEffect>()

        when {
            weight >= OVERBURDENED_THRESHOLD -> {
                effects +=
                    PotionEffect(
                        PotionEffectType.SLOWNESS,
                        EFFECT_DURATION_TICKS,
                        3,
                        false,
                        true,
                        true,
                    )
                effects +=
                    PotionEffect(
                        PotionEffectType.JUMP_BOOST,
                        EFFECT_DURATION_TICKS,
                        -2,
                        false,
                        true,
                        true,
                    )
                effects +=
                    PotionEffect(
                        PotionEffectType.MINING_FATIGUE,
                        EFFECT_DURATION_TICKS,
                        2,
                        false,
                        true,
                        true,
                    )
                effects +=
                    PotionEffect(
                        PotionEffectType.WEAKNESS,
                        EFFECT_DURATION_TICKS,
                        1,
                        false,
                        true,
                        true,
                    )
            }
            weight >= HEAVY_THRESHOLD -> {
                effects +=
                    PotionEffect(
                        PotionEffectType.SLOWNESS,
                        EFFECT_DURATION_TICKS,
                        2,
                        false,
                        true,
                        true,
                    )
                effects +=
                    PotionEffect(
                        PotionEffectType.JUMP_BOOST,
                        EFFECT_DURATION_TICKS,
                        -1,
                        false,
                        true,
                        true,
                    )
                effects +=
                    PotionEffect(
                        PotionEffectType.MINING_FATIGUE,
                        EFFECT_DURATION_TICKS,
                        1,
                        false,
                        true,
                        true,
                    )
            }
            weight >= MEDIUM_THRESHOLD -> {
                effects +=
                    PotionEffect(
                        PotionEffectType.SLOWNESS,
                        EFFECT_DURATION_TICKS,
                        1,
                        false,
                        true,
                        true,
                    )
                effects +=
                    PotionEffect(
                        PotionEffectType.JUMP_BOOST,
                        EFFECT_DURATION_TICKS,
                        -1,
                        false,
                        true,
                        true,
                    )
            }
            else ->
                effects +=
                    PotionEffect(
                        PotionEffectType.SLOWNESS,
                        EFFECT_DURATION_TICKS,
                        0,
                        false,
                        true,
                        true,
                    )
        }

        effects.forEach { player.addPotionEffect(it) }
    }

    /** Removes the potion effects applied by this mechanic. */
    private fun removeWeightEffects(player: Player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.JUMP_BOOST)
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE)
        player.removePotionEffect(PotionEffectType.WEAKNESS)
    }
}
