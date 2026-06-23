package org.xodium.illyriaplus.recipes.vanilla

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.StonecuttingRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.recipes.RecipeInterface

/** Represents an object handling painting recipe implementation within the system. */
internal object PaintingRecipe : RecipeInterface {
    override val recipes =
        buildSet {
            val paintingRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.PAINTING_VARIANT)

            paintingRegistry.forEach { variant ->
                val variantKey = paintingRegistry.getKey(variant) ?: return@forEach

                add(
                    StonecuttingRecipe(
                        NamespacedKey(instance, "painting_${variantKey.value().replace(':', '_')}_stonecutting_recipe"),
                        @Suppress("UnstableApiUsage")
                        ItemStack.of(Material.PAINTING).apply { setData(DataComponentTypes.PAINTING_VARIANT, variant) },
                        Material.PAINTING,
                    ),
                )
            }
        }
}
