package org.xodium.illyriaplus.recipes.vanilla

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.recipes.RecipeInterface

/** Represents an object handling nether-wart-block-to-nether-wart recipe implementation within the system. */
internal object NetherWartBlockRecipe : RecipeInterface {
    override val recipes =
        setOf(
            ShapelessRecipe(
                NamespacedKey(instance, "nether_wart_block_shapeless_recipe"),
                ItemStack.of(Material.NETHER_WART, 9),
            ).apply {
                addIngredient(Material.NETHER_WART_BLOCK)
            },
        )
}
