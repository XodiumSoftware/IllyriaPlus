package org.xodium.illyriaplus.recipes.custom

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ShapedRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.items.Trowel
import org.xodium.illyriaplus.recipes.RecipeInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents the crafting recipe for the Trowel custom item. */
internal object TrowelRecipe : RecipeInterface {
    override val recipes =
        setOf(
            ShapedRecipe(
                NamespacedKey(instance, "trowel_shaped_recipe"),
                Trowel(),
            ).apply {
                shape(" I ", "S  ")
                setIngredient('I', Material.IRON_INGOT)
                setIngredient('S', Material.STICK)
            },
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Trowel())
                .setName(MM.deserialize("<mango>Trowel Recipe</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize("<yellow>[ ] [I]</yellow>"),
                    MM.deserialize("<yellow>[S] [ ]</yellow>"),
                    MM.deserialize(""),
                    MM.deserialize("<dark_gray>I = Iron Ingot</dark_gray>"),
                    MM.deserialize("<dark_gray>S = Stick</dark_gray>"),
                ),
        )
}
