package org.xodium.illyriaplus.data

import org.bukkit.Material

/** Categories for grouping mechanics in the FAQ GUI. */
internal enum class FaqCategory(
    val label: String,
    val material: Material,
    val char: Char,
) {
    PLAYER("<mango>Player Category</gradient>", Material.CARVED_PUMPKIN, 'P'),
    WORLD("<mango>World Category</gradient>", Material.GRASS_BLOCK, 'W'),
    ENTITY("<mango>Entity Category</gradient>", Material.WOLF_SPAWN_EGG, 'E'),
    SERVER("<mango>Server Category</gradient>", Material.COMPASS, 'S'),
    ADMIN("<mango>Admin Category</gradient>", Material.COMMAND_BLOCK, 'A'),
}
