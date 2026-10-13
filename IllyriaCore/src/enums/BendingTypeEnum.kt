package org.xodium.illyriacore.enums

import org.bukkit.Material

/** Represents the different types of bending available. */
internal enum class BendingTypeEnum(
    val color: String,
    val material: Material,
) {
    AIR("white", Material.WIND_CHARGE),
    EARTH("brown", Material.MUD),
    FIRE("red", Material.FIRE_CHARGE),
    WATER("blue", Material.HEART_OF_THE_SEA),
}
