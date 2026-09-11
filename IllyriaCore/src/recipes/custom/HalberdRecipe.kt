package org.xodium.illyriacore.recipes.custom

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.items.HalberdItem
import org.xodium.illyriacore.recipes.RecipeInterface

/** Represents the recipe for upgrading a Trident into a Halberd. */
internal object HalberdRecipe : RecipeInterface {
    override val recipes =
        setOf(
            SmithingTransformRecipe(
                NamespacedKey(instance, "halberd_smithing_transform_recipe"),
                HalberdItem(),
                RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                RecipeChoice.MaterialChoice(Material.NETHERITE_SPEAR),
                RecipeChoice.MaterialChoice(Material.NETHER_STAR),
            ),
        )
}
