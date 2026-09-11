package org.xodium.illyriacore.recipes.custom

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.items.GreatswordItem
import org.xodium.illyriacore.recipes.RecipeInterface

/** Represents the recipe for upgrading a Netherite Sword into a Greatsword. */
internal object GreatswordRecipe : RecipeInterface {
    override val recipes =
        setOf(
            SmithingTransformRecipe(
                NamespacedKey(instance, "greatsword_smithing_transform_recipe"),
                GreatswordItem(),
                RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                RecipeChoice.MaterialChoice(Material.NETHERITE_SWORD),
                RecipeChoice.MaterialChoice(Material.NETHER_STAR),
            ),
        )
}
