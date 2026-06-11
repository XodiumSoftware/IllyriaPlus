package org.xodium.illyriaplus.recipes.vanilla

import org.xodium.illyriaplus.recipes.RecipeInterface
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents an object handling diamond armor/tool recycling via blast furnace. */
internal object DiamondRecycleRecipe : RecipeInterface {
    override val recipes =
        setOf(
            BlastingRecipe(
                NamespacedKey(instance, "diamond_recycle_blasting_recipe"),
                ItemStack.of(Material.DIAMOND),
                RecipeChoice.MaterialChoice(
                    Material.DIAMOND_AXE,
                    Material.DIAMOND_BOOTS,
                    Material.DIAMOND_CHESTPLATE,
                    Material.DIAMOND_HELMET,
                    Material.DIAMOND_HOE,
                    Material.DIAMOND_HORSE_ARMOR,
                    Material.DIAMOND_LEGGINGS,
                    Material.DIAMOND_NAUTILUS_ARMOR,
                    Material.DIAMOND_PICKAXE,
                    Material.DIAMOND_SHOVEL,
                    Material.DIAMOND_SPEAR,
                    Material.DIAMOND_SWORD,
                ),
                1.0f,
                100,
            ),
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.DIAMOND)
                .setName(MM.deserialize("<mango>Diamond Recycle Recipes</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Recycle Gear</yellow> <firewatch>></gradient> " +
                            "<white>Diamond armor and tools can be recycled into diamonds via blast furnace</white>",
                    ),
                ),
        )
}
