package org.xodium.illyriaplus.damagetypes

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.DamageTypeRegistryEntry
import net.kyori.adventure.key.Key
import org.bukkit.damage.DamageType
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils.toRegistryKeyFragment

/** Represents a contract for custom damage types within the system. */
internal interface DamageTypeInterface {
    /**
     * The unique typed key identifying this damage type in the registry.
     *
     * @see io.papermc.paper.registry.TypedKey
     * @see io.papermc.paper.registry.RegistryKey.DAMAGE_TYPE
     */
    val key: TypedKey<DamageType>
        get() =
            TypedKey.create(
                RegistryKey.DAMAGE_TYPE,
                Key.key(IllyriaPlus.ID, javaClass.toRegistryKeyFragment<DamageType>()),
            )

    /**
     * Configures the properties of the damage type using the provided builder.
     *
     * @param builder The builder used to define the damage type properties.
     * @return The builder for method chaining.
     */
    fun invoke(builder: DamageTypeRegistryEntry.Builder): DamageTypeRegistryEntry.Builder = builder

    /**
     * Retrieves the damage type from the registry.
     *
     * @return The [DamageType] instance corresponding to the key.
     * @throws NoSuchElementException if the damage type is not found in the registry.
     */
    fun get(): DamageType = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).getOrThrow(key)
}
