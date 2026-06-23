package org.xodium.illyriaplus.banners

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.BannerPatternRegistryEntry
import net.kyori.adventure.key.Key
import org.bukkit.block.banner.PatternType
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.data.BannerData

/** Represents a collection of registerable banner patterns within the system. */
@Suppress("UnstableApiUsage")
internal interface BannerInterface {
    /** The complete list of banner patterns in this collection. */
    val banners: List<BannerData>

    /**
     * The unique typed key identifying a banner pattern in the registry.
     *
     * @param name The registry key fragment (snake_case) of the banner pattern.
     * @see io.papermc.paper.registry.TypedKey
     * @see io.papermc.paper.registry.RegistryKey.BANNER_PATTERN
     */
    fun key(name: String): TypedKey<PatternType> =
        TypedKey.create(RegistryKey.BANNER_PATTERN, Key.key(IllyriaPlus.ID, name))

    /**
     * Configures the properties of a named banner pattern using the provided builder.
     *
     * @param name The registry key fragment (snake_case) of the banner pattern.
     * @param builder The builder used to define the banner pattern properties.
     * @return The builder for method chaining.
     */
    fun invoke(
        name: String,
        builder: BannerPatternRegistryEntry.Builder,
    ): BannerPatternRegistryEntry.Builder =
        banners.first { it.name == name }.let { banner ->
            builder.apply {
                assetId(Key.key(IllyriaPlus.ID, banner.name))
                translationKey(banner.translationKey)
            }
        }

    /**
     * Retrieves a banner pattern from the registry.
     *
     * @param name The registry key fragment (snake_case) of the banner pattern.
     * @return The [PatternType] instance corresponding to the key.
     * @throws NoSuchElementException if the banner pattern is not found in the registry.
     */
    fun get(name: String): PatternType =
        RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN).getOrThrow(key(name))
}
