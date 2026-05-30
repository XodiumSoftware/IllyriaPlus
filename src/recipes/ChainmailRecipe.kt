package org.xodium.illyriaplus.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents an object handling chainmail recipe implementation within the system. */
internal object ChainmailRecipe : RecipeInterface {
    override val recipes =
        setOf(
            ShapedRecipe(
                NamespacedKey(instance, "chainmail_helmet_shaped_recipe"),
                ItemStack.of(Material.CHAINMAIL_HELMET),
            ).apply {
                shape("AAA", "A A")
                setIngredient('A', Material.IRON_BARS)
            },
            ShapedRecipe(
                NamespacedKey(instance, "chainmail_chestplate_shaped_recipe"),
                ItemStack.of(Material.CHAINMAIL_CHESTPLATE),
            ).apply {
                shape("A A", "AAA", "AAA")
                setIngredient('A', Material.IRON_BARS)
            },
            ShapedRecipe(
                NamespacedKey(instance, "chainmail_leggings_shaped_recipe"),
                ItemStack.of(Material.CHAINMAIL_LEGGINGS),
            ).apply {
                shape("AAA", "A A", "A A")
                setIngredient('A', Material.IRON_BARS)
            },
            ShapedRecipe(
                NamespacedKey(instance, "chainmail_boots_shaped_recipe"),
                ItemStack.of(Material.CHAINMAIL_BOOTS),
            ).apply {
                shape("A A", "A A")
                setIngredient('A', Material.IRON_BARS)
            },
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.IRON_BARS)
                .setName(MM.deserialize("<mango>Chainmail Recipes</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Craft Chainmail</yellow> <firewatch>></gradient> " +
                            "<white>Chainmail armor can be crafted using iron bars</white>",
                    ),
                ),
        )
}
