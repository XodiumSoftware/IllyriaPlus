package org.xodium.illyriaplus.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents an object handling wood-log recipe implementation within the system. */
internal object WoodLogRecipe : RecipeInterface {
    private val woodToLog =
        mapOf(
            // Regular Logs
            Material.OAK_WOOD to Material.OAK_LOG,
            Material.SPRUCE_WOOD to Material.SPRUCE_LOG,
            Material.BIRCH_WOOD to Material.BIRCH_LOG,
            Material.JUNGLE_WOOD to Material.JUNGLE_LOG,
            Material.ACACIA_WOOD to Material.ACACIA_LOG,
            Material.DARK_OAK_WOOD to Material.DARK_OAK_LOG,
            Material.MANGROVE_WOOD to Material.MANGROVE_LOG,
            Material.CHERRY_WOOD to Material.CHERRY_LOG,
            Material.PALE_OAK_WOOD to Material.PALE_OAK_LOG,
            Material.CRIMSON_HYPHAE to Material.CRIMSON_STEM,
            Material.WARPED_HYPHAE to Material.WARPED_STEM,
            // Stripped Logs
            Material.STRIPPED_OAK_WOOD to Material.STRIPPED_OAK_LOG,
            Material.STRIPPED_SPRUCE_WOOD to Material.STRIPPED_SPRUCE_LOG,
            Material.STRIPPED_BIRCH_WOOD to Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_JUNGLE_WOOD to Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_ACACIA_WOOD to Material.STRIPPED_ACACIA_LOG,
            Material.STRIPPED_DARK_OAK_WOOD to Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_MANGROVE_WOOD to Material.STRIPPED_MANGROVE_LOG,
            Material.STRIPPED_CHERRY_WOOD to Material.STRIPPED_CHERRY_LOG,
            Material.STRIPPED_PALE_OAK_WOOD to Material.STRIPPED_PALE_OAK_LOG,
            Material.STRIPPED_CRIMSON_HYPHAE to Material.STRIPPED_CRIMSON_STEM,
            Material.STRIPPED_WARPED_HYPHAE to Material.STRIPPED_WARPED_STEM,
        )

    override val recipes =
        woodToLog
            .map { (wood, log) ->
                ShapelessRecipe(
                    NamespacedKey(instance, "${wood.key.key}_to_${log.key.key}_shapeless_recipe"),
                    ItemStack.of(log, 4),
                ).apply { addIngredient(wood) }
            }.toSet()

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.OAK_LOG)
                .setName(MM.deserialize("<mango>Wood Log Recipes</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Convert</yellow> <firewatch>></gradient> " +
                            "<white>Wood blocks can be converted back to logs in a crafting table</white>",
                    ),
                ),
        )
}
