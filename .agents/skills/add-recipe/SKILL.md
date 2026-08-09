---
name: add-recipe
description: Scaffolds a new RecipeInterface object under recipes/vanilla and registers it in the main plugin class.
---

# Add a Recipe

Use this skill when the user wants to add a new custom recipe to the project.

## Before Writing Code

1. Ask the user:
   - What is the recipe name?
   - What recipe type: shaped, shapeless, blasting, furnace, smoking, stonecutting, or brewing (potion mix)?
   - What are the ingredients and result?
   - What should the `NamespacedKey` be? Suggest `{descriptive_name}_{recipe_type}` if the user is unsure.

## Creating the Recipe File

1. Create `src/recipes/vanilla/<Name>Recipe.kt` using the template below.
2. The object must be `internal object <Name>Recipe : RecipeInterface`.
3. Override `val recipes: Collection<Recipe>` for crafting/smelting recipes, or `val potions: Collection<PotionMix>` for brewing recipes.
4. Use `ItemStack.of(Material.<...>)` for results.
5. Name each `NamespacedKey` with the pattern `{descriptive_name}_{recipe_type}_recipe`.

## Wiring

1. Open the main plugin class (e.g., `src/Plugin.kt`).
2. Import the new recipe.
3. Add it alphabetically to the `recipes` list in `onEnable()`.

## Documentation

1. Update `ARCHITECTURE.md` recipe list.
2. Add a concise KDoc comment to the object describing what it adds.
3. If the change is significant, run `./gradlew dokkaGenerateHtml`.

## Template

```kotlin
package <project.package>.recipes.vanilla

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.<RecipeType>
import <project.package>.Plugin.Companion.instance
import <project.package>.recipes.RecipeInterface

/** Represents an object handling <description> within the system. */
internal object <Name>Recipe : RecipeInterface {
    override val recipes =
        setOf(
            <RecipeType>(
                NamespacedKey(instance, "<descriptive_name>_<recipe_type>_recipe"),
                ItemStack.of(Material.<RESULT>),
                // ingredients
            ),
        )
}
```

Replace `<project.package>` and `Plugin` with the actual project package and main plugin class name.

After finishing, summarize the files changed and ask the user if they want to commit.
