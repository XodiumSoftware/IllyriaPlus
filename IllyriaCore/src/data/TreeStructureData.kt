package org.xodium.illyriaplus.data

import org.bukkit.structure.Structure
import org.bukkit.util.Vector

/**
 * Holds a loaded tree [Structure] and its pre-computed trunk centre offset.
 *
 * @property structure The loaded tree [Structure] to place when a sapling grows.
 * @property trunkOffset The [Vector] from the structure origin to the trunk centre block.
 *                        Used to align the trunk with the sapling location when the structure is placed.
 */
internal data class TreeStructureData(
    val structure: Structure,
    val trunkOffset: Vector,
)
