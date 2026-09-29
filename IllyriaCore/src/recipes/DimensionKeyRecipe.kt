package org.xodium.illyriacore.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ShapedRecipe
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.items.IncendiumKeyItem
import org.xodium.illyriacore.items.NullscapeKeyItem

/** Represents recipes for custom dimension key items. */
internal object DimensionKeyRecipe : RecipeInterface {
    override val recipes =
        setOf(
            ShapedRecipe(
                NamespacedKey(instance, "incendium_key_shaped_recipe"),
                IncendiumKeyItem(),
            ).apply {
                shape(" F ", "FDF", " R ")
                setIngredient('F', Material.FLINT_AND_STEEL)
                setIngredient('D', Material.DIAMOND)
                setIngredient('R', Material.REDSTONE)
            },
            ShapedRecipe(
                NamespacedKey(instance, "nullscape_key_shaped_recipe"),
                NullscapeKeyItem(),
            ).apply {
                shape(" A ", "AEA", " O ")
                setIngredient('A', Material.AMETHYST_SHARD)
                setIngredient('E', Material.ENDER_PEARL)
                setIngredient('O', Material.OBSIDIAN)
            },
        )
}
