package org.xodium.illyriaplus.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.CampfireRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.SmokingRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents an object handling rotten-flesh recipe implementation within the system. */
internal object RottenFleshRecipe : RecipeInterface {
    override val recipes =
        setOf(
            FurnaceRecipe(
                NamespacedKey(instance, "rotten_flesh_furnace_recipe"),
                ItemStack.of(Material.LEATHER),
                Material.ROTTEN_FLESH,
                0.1f,
                200,
            ),
            SmokingRecipe(
                NamespacedKey(instance, "rotten_flesh_smoking_recipe"),
                ItemStack.of(Material.LEATHER),
                Material.ROTTEN_FLESH,
                0.1f,
                100,
            ),
            CampfireRecipe(
                NamespacedKey(instance, "rotten_flesh_campfire_recipe"),
                ItemStack.of(Material.LEATHER),
                Material.ROTTEN_FLESH,
                0.05f,
                600,
            ),
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.LEATHER)
                .setName(MM.deserialize("<mango>Rotten Flesh Recipes</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Smelt</yellow> <firewatch>></gradient> " +
                            "<white>Rotten flesh can be smelted into leather</white>",
                    ),
                ),
        )
}
