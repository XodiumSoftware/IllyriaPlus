package org.xodium.illyriacore.enums

import org.bukkit.Material

/** Represents the different types of bending available. */
internal enum class BendingTypeEnum(
    val color: String,
    val material: Material,
) {
    AIR("white", Material.WHITE_CONCRETE),
    EARTH("brown", Material.BROWN_CONCRETE),
    FIRE("red", Material.RED_CONCRETE),
    WATER("blue", Material.BLUE_CONCRETE),
}
