package org.xodium.illyriaplus.paintings

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.bukkit.Art
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.PaintingData

/** Represents a collection of registerable painting variants within the system. */
internal interface PaintingInterface {
    /** The complete list of painting variants in this collection. */
    val paintings: List<PaintingData>

    /**
     * The unique typed key identifying a painting variant in the registry.
     *
     * @param name The registry key fragment (snake_case) of the painting variant.
     * @see io.papermc.paper.registry.TypedKey
     * @see io.papermc.paper.registry.RegistryKey.PAINTING_VARIANT
     */
    fun key(name: String): TypedKey<Art> = TypedKey.create(RegistryKey.PAINTING_VARIANT, Key.key(IllyriaPlus.ID, name))

    /**
     * Configures the properties of a named painting variant using the provided builder.
     *
     * @param name The registry key fragment (snake_case) of the painting variant.
     * @param builder The builder used to define the painting variant properties.
     * @return The builder for method chaining.
     */
    fun invoke(
        name: String,
        builder: PaintingVariantRegistryEntry.Builder,
    ): PaintingVariantRegistryEntry.Builder =
        paintings.first { it.name == name }.let { (name, size, author, title) ->
            builder.apply {
                assetId(Key.key(IllyriaPlus.ID, name))
                width(size.first)
                height(size.second)
                title(MM.deserialize("<yellow>$title"))
                author(MM.deserialize("<gray>$author"))
            }
        }

    /**
     * Retrieves a painting variant from the registry.
     *
     * @param name The registry key fragment (snake_case) of the painting variant.
     * @return The [Art] instance corresponding to the key.
     * @throws NoSuchElementException if the painting variant is not found in the registry.
     */
    fun get(name: String): Art =
        RegistryAccess.registryAccess().getRegistry(RegistryKey.PAINTING_VARIANT).getOrThrow(key(name))
}
