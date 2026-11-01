package org.xodium.illyriacore.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.items.flask.FlaskItem

/** Recipes for the Illyria Flask and its upgrades. */
internal object FlaskRecipe : RecipeInterface {
    override val recipes =
        setOf(
            ShapedRecipe(
                NamespacedKey(instance, "illyria_flask_shaped_recipe"),
                FlaskItem(),
            ).apply {
                shape(" A ", "BCB", " B ")
                setIngredient('A', Material.BLAZE_POWDER)
                setIngredient('B', Material.NETHER_BRICK)
                setIngredient('C', Material.GLASS_BOTTLE)
            },
            ShapedRecipe(
                NamespacedKey(instance, "illyria_flask_charge_upgrade_shaped_recipe"),
                FlaskItem(),
            ).apply {
                shape(" A ", " B ", "   ")
                setIngredient('A', Material.ECHO_SHARD)
                setIngredient('B', RecipeChoice.exactChoice(FlaskItem()))
            },
            ShapedRecipe(
                NamespacedKey(instance, "illyria_flask_potency_upgrade_shaped_recipe"),
                FlaskItem(),
            ).apply {
                shape(" A ", " B ", "   ")
                setIngredient('A', Material.NETHER_STAR)
                setIngredient('B', RecipeChoice.exactChoice(FlaskItem()))
            },
        )
}
