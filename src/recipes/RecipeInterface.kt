package org.xodium.illyriaplus.recipes

import io.papermc.paper.potion.PotionMix
import org.bukkit.inventory.Recipe
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.data.FaqTab
import xyz.xenondevs.invui.item.Item
import kotlin.time.measureTime

/** Represents a contract for recipes within the system. */
internal interface RecipeInterface {
    /**
     * Retrieves the set of recipes to be registered.
     *
     * @return A [Collection] of [org.bukkit.inventory.Recipe] instances.
     */
    val recipes: Collection<Recipe> get() = emptySet()

    /**
     * Retrieves the set of potion brewing recipes to be registered.
     *
     * @return A [Collection] of [io.papermc.paper.potion.PotionMix] instances.
     */
    val potions: Collection<PotionMix> get() = emptySet()

    /**
     * Retrieves the FAQ tab.
     *
     * @return A [org.xodium.illyriaplus.data.FaqTab] instance.
     */
    val faqTab: FaqTab get() = FaqTab.RECIPES

    /**
     * Retrieves the FAQ display item.
     *
     * @return An [xyz.xenondevs.invui.item.Item] instance.
     */
    val faqItem: Item

    /**
     * Registers all recipes returned by [recipes] with the server.
     *
     * @return The time taken to register the recipes in milliseconds.
     */
    fun register(): Long =
        measureTime {
            recipes.forEach { IllyriaPlus.instance.server.addRecipe(it) }
            potions.forEach {
                IllyriaPlus.instance.server.potionBrewer
                    .addPotionMix(it)
            }
        }.inWholeMilliseconds
}
