package org.xodium.illyriaplus.data

import org.bukkit.Material

/**
 * Categories for grouping features in the FAQ GUI.
 *
 * @param label The display label for the tab.
 * @param material The [Material] used as the tab icon.
 * @param char The character identifier for the tab.
 */
internal enum class FaqTab(
    val label: String,
    val material: Material,
    val char: Char,
) {
    PLAYER("<mango>Player Mechanics</gradient>", Material.CARVED_PUMPKIN, 'P'),
    WORLD("<mango>World Mechanics</gradient>", Material.GRASS_BLOCK, 'W'),
    ENTITY("<mango>Entity Mechanics</gradient>", Material.WOLF_SPAWN_EGG, 'E'),
    SERVER("<mango>Server Mechanics</gradient>", Material.COMPASS, 'S'),
    RECIPES("<mango>Recipes</gradient>", Material.PAPER, 'R'),
}

// TODO: Move player-admin into Mechanics for better organization.
// TODO: Create Enchantments and add into it SPELLS, UTILITY, VANILLA.
