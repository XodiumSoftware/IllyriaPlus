package org.xodium.illyriaplus.recipes.vanilla

import org.xodium.illyriaplus.recipes.RecipeInterface
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

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

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.NETHER_WART)
                .setName(MM.deserialize("<mango>Nether Wart Block</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Craft</yellow> <firewatch>></gradient> " +
                            "<white>Nether wart blocks can be crafted into 9 nether wart</white>",
                    ),
                ),
        )
}
