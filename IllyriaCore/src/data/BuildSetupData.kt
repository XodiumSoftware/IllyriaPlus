package org.xodium.illyriaplus.data

import org.bukkit.Material
import org.bukkit.Tag

/**
 * Represents a placement rule allowing specific materials to be built against a target block.
 *
 * @property materials The set of [Material] entries that can be placed.
 * @property target The [Tag] of blocks the [materials] can be placed against.
 * @property offset The relative offset from the placed block to the target block.
 */
internal data class BuildSetupData(
    val materials: Set<Material>,
    val target: Tag<Material>,
    val offset: Triple<Int, Int, Int> = Triple(0, -1, 0),
) {
    companion object {
        /**
         * Flattens a collection of build setups into a map keyed by each material.
         *
         * @return A map from each [Material] to its owning [BuildSetupData].
         */
        fun Collection<BuildSetupData>.toMaterialMap(): Map<Material, BuildSetupData> =
            flatMap { setup -> setup.materials.map { it to setup } }.toMap()
    }
}
