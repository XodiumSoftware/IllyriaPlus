package org.xodium.illyriaplus.recipes.vanilla

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.recipes.RecipeInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents an object handling ice breakdown recipes within the system. */
internal object IceBreakdownRecipe : RecipeInterface {
    override val recipes =
        setOf(
            ShapelessRecipe(
                NamespacedKey(instance, "blue_ice_breakdown_shapeless_recipe"),
                ItemStack.of(Material.PACKED_ICE, 9),
            ).apply {
                addIngredient(Material.BLUE_ICE)
            },
            ShapelessRecipe(
                NamespacedKey(instance, "packed_ice_breakdown_shapeless_recipe"),
                ItemStack.of(Material.ICE, 9),
            ).apply {
                addIngredient(Material.PACKED_ICE)
            },
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.BLUE_ICE)
                .setName(MM.deserialize("<mango>Ice Breakdown</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Craft</yellow> <firewatch>></gradient> " +
                            "<white>Blue ice breaks into 9 packed ice; packed ice breaks into 9 ice</white>",
                    ),
                ),
        )
}
