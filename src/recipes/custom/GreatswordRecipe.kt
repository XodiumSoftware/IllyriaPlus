package org.xodium.illyriaplus.recipes.custom

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.items.GreatswordItem
import org.xodium.illyriaplus.recipes.RecipeInterface

/** Represents the recipe for upgrading a Netherite Sword into a Greatsword. */
internal object GreatswordRecipe : RecipeInterface {
    override val recipes =
        setOf(
            SmithingTransformRecipe(
                NamespacedKey(instance, "greatsword_smithing_transform_recipe"),
                GreatswordItem(),
                RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                RecipeChoice.MaterialChoice(Material.NETHERITE_SWORD),
                RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT),
            ),
        )
}
