package org.xodium.illyriasouls.recipes

import org.bukkit.inventory.Recipe
import org.xodium.illyriasouls.IllyriaSouls
import kotlin.time.measureTime

/** Represents a contract for recipes within IllyriaSouls. */
internal interface RecipeInterface {
    /**
     * Retrieves the set of recipes to be registered.
     *
     * @return A [Collection] of [Recipe] instances.
     */
    val recipes: Collection<Recipe> get() = emptySet()

    /**
     * Registers all recipes returned by [recipes] with the server.
     *
     * @return The time taken to register the recipes in milliseconds.
     */
    fun register(): Long =
        measureTime {
            recipes.forEach { IllyriaSouls.instance.server.addRecipe(it) }
        }.inWholeMilliseconds
}
