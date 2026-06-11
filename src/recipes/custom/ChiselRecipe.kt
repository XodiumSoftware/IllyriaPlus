package org.xodium.illyriaplus.recipes.custom

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ShapedRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.items.Chisel
import org.xodium.illyriaplus.recipes.RecipeInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents the crafting recipe for the Chisel custom item. */
internal object ChiselRecipe : RecipeInterface {
    override val recipes =
        setOf(
            ShapedRecipe(
                NamespacedKey(instance, "chisel_shaped_recipe"),
                Chisel(),
            ).apply {
                shape(" I ", " S ")
                setIngredient('I', Material.IRON_INGOT)
                setIngredient('S', Material.STICK)
            },
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Chisel())
                .setName(MM.deserialize("<mango>Chisel Recipe</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize("<yellow>[ ] [I]</yellow>"),
                    MM.deserialize("<yellow>[ ] [S]</yellow>"),
                    MM.deserialize(""),
                    MM.deserialize("<dark_gray>I = Iron Ingot</dark_gray>"),
                    MM.deserialize("<dark_gray>S = Stick</dark_gray>"),
                ),
        )
}
