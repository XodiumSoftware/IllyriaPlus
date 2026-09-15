package org.xodium.illyriacore.variants

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.WolfVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.bukkit.entity.Wolf
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyriacore.Utils.toRegistryKeyFragment

/** Represents a contract for wolf variants within the system. */
internal interface VariantInterface {
    /**
     * The unique typed key identifies this variant in the registry.
     *
     * @see io.papermc.paper.registry.TypedKey
     * @see io.papermc.paper.registry.RegistryKey.WOLF_VARIANT
     */
    val key: TypedKey<Wolf.Variant>
        get() =
            TypedKey.create(
                RegistryKey.WOLF_VARIANT,
                Key.key(IllyriaCore.ID, javaClass.toRegistryKeyFragment<Wolf.Variant>()),
            )

    /**
     * Configures the properties of the wolf variant using the provided builder.
     *
     * @param builder The builder used to define the wolf variant properties.
     * @return The builder for method chaining.
     */
    fun invoke(builder: WolfVariantRegistryEntry.Builder): WolfVariantRegistryEntry.Builder = builder

    /**
     * Retrieves the wolf variant from the registry.
     *
     * @return The [Wolf.Variant] instance corresponding to the key.
     * @throws NoSuchElementException if the wolf variant is not found in the registry.
     */
    fun get(): Wolf.Variant = RegistryAccess.registryAccess().getRegistry(RegistryKey.WOLF_VARIANT).getOrThrow(key)
}
