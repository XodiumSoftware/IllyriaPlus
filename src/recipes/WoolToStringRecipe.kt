package org.xodium.illyriaplus.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapelessRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents an object handling wool-to-string recipe implementation within the system. */
internal object WoolToStringRecipe : RecipeInterface {
    override val recipes =
        setOf(
            ShapelessRecipe(
                NamespacedKey(instance, "wool_to_string_shapeless_recipe"),
                ItemStack.of(Material.STRING, 4),
            ).apply {
                addIngredient(RecipeChoice.MaterialChoice(Tag.WOOL))
            },
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.STRING)
                .setName(MM.deserialize("<mango>Wool to String</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Craft</yellow> <firewatch>></gradient> " +
                            "<white>Any wool can be crafted into 4 strings</white>",
                    ),
                ),
        )
}
