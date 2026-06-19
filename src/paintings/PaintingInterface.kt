package org.xodium.illyriaplus.paintings

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.bukkit.Art
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase

/** Represents a single registerable painting variant within the system. */
@Suppress("UnstableApiUsage")
internal interface PaintingInterface {
    /** The registry key fragment (snake_case) identifying this painting variant. */
    val name: String

    /** The width and height of the painting in blocks, represented as `Pair(width, height)`. */
    val size: Pair<Int, Int>

    /** The namespace/author key for the painting asset. */
    val author: String

    /** The display title derived from [name], converted to proper case. */
    val title: String get() = name.snakeToProperCase()

    /** The asset key fragment used to locate the painting texture. */
    val assetKey: String get() = name

    /**
     * The unique typed key identifying this painting variant in the registry.
     *
     * @see io.papermc.paper.registry.TypedKey
     * @see io.papermc.paper.registry.RegistryKey.PAINTING_VARIANT
     */
    val key: TypedKey<Art>
        get() =
            TypedKey.create(
                RegistryKey.PAINTING_VARIANT,
                Key.key(IllyriaPlus.ID, name),
            )

    /**
     * Configures the properties of the painting variant using the provided builder.
     *
     * @param builder The builder used to define the painting variant properties.
     * @return The builder for method chaining.
     */
    fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(author, assetKey))
            .width(size.first)
            .height(size.second)
            .title(MM.deserialize(title))
            .author(MM.deserialize(author))

    /**
     * Retrieves the painting variant from the registry.
     *
     * @return The [Art] instance corresponding to the key.
     * @throws NoSuchElementException if the painting variant is not found in the registry.
     */
    fun get(): Art = RegistryAccess.registryAccess().getRegistry(RegistryKey.PAINTING_VARIANT).getOrThrow(key)
}
