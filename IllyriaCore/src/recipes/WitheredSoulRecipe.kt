package org.xodium.illyriacore.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingRecipe
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.items.WitheredSoulItem

/**
 * Represents a smithing recipe allowing any netherite item to be infused with a
 * Withered Soul.
 */
internal object WitheredSoulRecipe : RecipeInterface {
    override val recipes =
        setOf(
            SmithingRecipe(
                NamespacedKey(instance, "withered_soul_smithing_recipe"),
                ItemStack.of(Material.NETHERITE_SWORD),
                RecipeChoice.PredicateChoice(
                    { stack ->
                        stack.type in Tag.ITEMS_NETHERITE_TOOL_MATERIALS ||
                            stack.type in Tag.ITEMS_REPAIRS_NETHERITE_ARMOR
                    },
                    ItemStack.of(Material.NETHERITE_SWORD),
                ),
                RecipeChoice.exactChoice(WitheredSoulItem()),
                true,
            ),
        )
}
