package org.xodium.illyriacore.enums

import org.bukkit.Material

/** Represents the different types of bending available. */
internal enum class BendingTypeEnum(
    val color: String,
    val material: Material,
) {
    AIR("white", Material.WHITE_WOOL),
    EARTH("brown", Material.BROWN_WOOL),
    FIRE("red", Material.RED_WOOL),
    WATER("blue", Material.BLUE_WOOL),
}
