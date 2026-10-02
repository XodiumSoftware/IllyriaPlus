package org.xodium.illyriacore.data

import dev.wyck.biome.ClimateSettings
import dev.wyck.biome.CustomBiome
import dev.wyck.biome.TemperatureModifier
import dev.wyck.keys.ResourceKey
import dev.wyck.renderer.packet.data.BlockReplacement
import org.bukkit.Material

/**
 * Defines seasonal biome palettes for Wyck's packet-based biome rendering.
 *
 * Each season is a virtual biome (phony biome) that exists only in packets — the underlying
 * world data is never modified. The four seasons map to distinct visual palettes
 * applied client-side via Wyck's [dev.wyck.renderer.packet.PacketHandler].
 */
internal object SeasonBiomes {
    val SPRING: CustomBiome by lazy {
        CustomBiome
            .builder()
            .resourceKey(ResourceKey.of(IllyriaSeasons.ID, "spring"))
            .foliageColor("#7CFC00")
            .dryFoliageColor("#6B8E23")
            .grassColor("#32CD32")
            .waterColor("#4169E1")
            .climateSettings(ClimateSettings.of(true, 0.7f, TemperatureModifier.NONE, 0.4f))
            .blockReplacements(
                BlockReplacement.of(Material.OAK_LEAVES, Material.CHERRY_LEAVES),
                BlockReplacement.of(Material.BIRCH_LEAVES, Material.CHERRY_LEAVES),
            ).build()
    }

    val SUMMER: CustomBiome by lazy {
        CustomBiome
            .builder()
            .resourceKey(ResourceKey.of(IllyriaSeasons.ID, "summer"))
            .climateSettings(ClimateSettings.of(true, 0.8f, TemperatureModifier.NONE, 0.5f))
            .build()
    }

    val AUTUMN: CustomBiome by lazy {
        CustomBiome
            .builder()
            .resourceKey(ResourceKey.of(IllyriaSeasons.ID, "autumn"))
            .foliageColor("#D2691E")
            .dryFoliageColor("#8B4513")
            .grassColor("#CD853F")
            .waterColor("#4682B4")
            .climateSettings(ClimateSettings.of(true, 0.6f, TemperatureModifier.NONE, 0.5f))
            .blockReplacements(
                BlockReplacement.of(Material.OAK_LEAVES, Material.ORANGE_POPLAR_LEAVES),
                BlockReplacement.of(Material.BIRCH_LEAVES, Material.YELLOW_POPLAR_LEAVES),
                BlockReplacement.of(Material.SPRUCE_LEAVES, Material.RED_POPLAR_LEAVES),
            ).build()
    }

    val WINTER: CustomBiome by lazy {
        CustomBiome
            .builder()
            .resourceKey(ResourceKey.of(IllyriaSeasons.ID, "winter"))
            .foliageColor("#F0F0F0")
            .dryFoliageColor("#E0E0E0")
            .grassColor("#C0E8C0")
            .waterColor("#B0C4DE")
            .climateSettings(ClimateSettings.of(true, 0.2f, TemperatureModifier.NONE, 0.3f))
            .blockReplacements(
                BlockReplacement.of(Material.GRASS_BLOCK, Material.SNOW_BLOCK),
                BlockReplacement.of(Material.SHORT_GRASS, Material.SNOW),
                BlockReplacement.of(Material.DIRT, Material.SNOW_BLOCK),
                BlockReplacement.of(Material.DIRT_PATH, Material.SNOW_BLOCK),
            ).build()
    }

    val biomes: List<CustomBiome> by lazy {
        listOf(SPRING, SUMMER, AUTUMN, WINTER)
    }

    /** Registers all four seasonal biomes with Wyck's biome registry. */
    fun register() {
        biomes.forEach { it.register() }
    }

    /** Maps [SeasonState] to its corresponding [CustomBiome]. */
    fun of(season: SeasonState): CustomBiome =
        when (season) {
            SeasonState.SPRING -> SPRING
            SeasonState.SUMMER -> SUMMER
            SeasonState.AUTUMN -> AUTUMN
            SeasonState.WINTER -> WINTER
        }
}
