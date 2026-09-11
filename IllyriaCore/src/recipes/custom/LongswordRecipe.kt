package org.xodium.illyriacore.recipes.custom

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.items.LongswordItem
import org.xodium.illyriacore.recipes.RecipeInterface

/** Represents the recipe for upgrading a Netherite Sword into a Longsword. */
internal object LongswordRecipe : RecipeInterface {
    override val recipes =
        setOf(
            SmithingTransformRecipe(
                NamespacedKey(instance, "longsword_smithing_transform_recipe"),
                LongswordItem(),
                RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                RecipeChoice.MaterialChoice(Material.NETHERITE_SWORD),
                RecipeChoice.MaterialChoice(Material.ECHO_SHARD),
            ),
        )
}
