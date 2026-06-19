package org.xodium.illyriaplus.paintings

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.bukkit.Art
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.Utils.toRegistryKeyFragment

/** Represents a contract for painting variants within the system. */
@Suppress("UnstableApiUsage")
internal interface PaintingInterface {
    companion object {
        /** Shared namespace/author constant for Yapetto painting variants. */
        const val YAPETTO = "yapetto"
    }

    /**
     * The unique typed key identifying this painting variant in the registry.
     *
     * Class names must end with `Painting` (e.g. `AlphaPainting`) so the derived
     * registry key matches the intended variant key.
     *
     * @see io.papermc.paper.registry.TypedKey
     * @see io.papermc.paper.registry.RegistryKey.PAINTING_VARIANT
     */
    val key: TypedKey<Art>
        get() =
            TypedKey.create(
                RegistryKey.PAINTING_VARIANT,
                Key.key(IllyriaPlus.ID, javaClass.toRegistryKeyFragment("Painting")),
            )

    /**
     * The asset key fragment derived from the class name, with the `Painting`
     * suffix removed and converted to snake_case.
     */
    val assetKey: String get() = javaClass.toRegistryKeyFragment("Painting")

    /**
     * The display title derived from the class name, with the `Painting` suffix
     * removed and converted to proper case.
     */
    val title: String get() = javaClass.toRegistryKeyFragment("Painting").snakeToProperCase()

    /**
     * Configures the properties of the painting variant using the provided builder.
     *
     * @param builder The builder used to define the painting variant properties.
     * @return The builder for method chaining.
     */
    fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder = builder

    /**
     * Retrieves the painting variant from the registry.
     *
     * @return The [Art] instance corresponding to the key.
     * @throws NoSuchElementException if the painting variant is not found in the registry.
     */
    fun get(): Art = RegistryAccess.registryAccess().getRegistry(RegistryKey.PAINTING_VARIANT).getOrThrow(key)
}
