package org.xodium.illyriaplus.recipes.custom

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.StonecuttingRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.data.PotData
import org.xodium.illyriaplus.items.PotItem
import org.xodium.illyriaplus.recipes.RecipeInterface

/** Represents an object handling custom flower pot stonecutting recipes within the system. */
internal object PotRecipe : RecipeInterface {
    override val recipes =
        PotData
            .VARIANTS
            .map { data ->
                StonecuttingRecipe(
                    NamespacedKey(instance, "pot_${data.key}_stonecutting_recipe"),
                    PotItem(data),
                    Material.FLOWER_POT,
                )
            }.toSet()
}
