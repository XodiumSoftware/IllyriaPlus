package org.xodium.illyriaseasons

import dev.wyck.biome.ClimateSettings
import dev.wyck.biome.CustomBiome
import dev.wyck.biome.TemperatureModifier
import dev.wyck.keys.ResourceKey

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
            .resourceKey(ResourceKey.of(IllyriaSeasons.NAMESPACE, "spring"))
            .foliageColor("#7CFC00")
            .dryFoliageColor("#6B8E23")
            .grassColor("#32CD32")
            .waterColor("#4169E1")
            .climateSettings(ClimateSettings.of(true, 0.7f, TemperatureModifier.NONE, 0.4f))
            .build()
    }

    val SUMMER: CustomBiome by lazy {
        CustomBiome
            .builder()
            .resourceKey(ResourceKey.of(IllyriaSeasons.NAMESPACE, "summer"))
            .climateSettings(ClimateSettings.of(true, 0.8f, TemperatureModifier.NONE, 0.5f))
            .build()
    }

    val AUTUMN: CustomBiome by lazy {
        CustomBiome
            .builder()
            .resourceKey(ResourceKey.of(IllyriaSeasons.NAMESPACE, "autumn"))
            .foliageColor("#D2691E")
            .dryFoliageColor("#8B4513")
            .grassColor("#CD853F")
            .waterColor("#4682B4")
            .climateSettings(ClimateSettings.of(true, 0.6f, TemperatureModifier.NONE, 0.5f))
            .build()
    }

    val WINTER: CustomBiome by lazy {
        CustomBiome
            .builder()
            .resourceKey(ResourceKey.of(IllyriaSeasons.NAMESPACE, "winter"))
            .foliageColor("#F0F0F0")
            .dryFoliageColor("#E0E0E0")
            .grassColor("#C0E8C0")
            .waterColor("#B0C4DE")
            .climateSettings(ClimateSettings.of(true, 0.2f, TemperatureModifier.NONE, 0.3f))
            .build()
    }

    val biomes: List<CustomBiome> by lazy {
        listOf(SPRING, SUMMER, AUTUMN, WINTER)
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
