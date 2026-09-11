package org.xodium.illyriaplus.data

import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.joml.Vector3f

/**
 * Represents the display configuration for a plant placed into a pot.
 *
 * Used by the potting mechanic to scale, offset, and optionally flip a
 * [org.bukkit.entity.BlockDisplay] so the plant fits naturally inside a pot.
 *
 * @property blockData The block state rendered inside the pot, or `null` to use
 *                     the held item's default block state.
 * @property scale The scale applied to the displayed block.
 * @property translation The translation offset applied to the displayed block.
 * @property upsideDown Whether the plant hangs upside down (e.g. spore blossom).
 */
internal data class PlantData(
    val blockData: BlockData? = null,
    val scale: Vector3f = Vector3f(0.35f, 0.6f, 0.35f),
    val translation: Vector3f = Vector3f(-0.18f, 0.24f, -0.18f),
    val upsideDown: Boolean = false,
) {
    companion object {
        /** All supported plants, keyed by their held item material. */
        val PLANTS: Map<Material, PlantData> =
            mapOf(
                Material.BEETROOT to PlantData(blockData = Material.BEETROOTS.createBlockData("[age=3]")),
                Material.CARROT to PlantData(blockData = Material.CARROTS.createBlockData("[age=7]")),
                Material.CHERRY_LEAVES to leaves(),
                Material.CORNFLOWER to PlantData(),
                Material.DANDELION to PlantData(),
                Material.FERN to PlantData(),
                Material.HANGING_ROOTS to hanging(1.24f),
                Material.LILY_OF_THE_VALLEY to PlantData(),
                Material.OAK_LEAVES to leaves(),
                Material.OAK_SAPLING to PlantData(),
                Material.POPPY to PlantData(),
                Material.POTATO to PlantData(blockData = Material.POTATOES.createBlockData("[age=7]")),
                Material.SHORT_GRASS to wide(),
                Material.SPORE_BLOSSOM to hanging(1.37f),
                Material.SPRUCE_SAPLING to PlantData(),
                Material.SUGAR_CANE to wide(),
                Material.TALL_GRASS to wide(),
                Material.WHEAT to
                    PlantData(
                        blockData = Material.WHEAT.createBlockData("[age=7]"),
                        scale = Vector3f(0.35f, 0.4f, 0.35f),
                    ),
            )

        /**
         * Creates a configuration for wide plants like tall grass and sugar cane.
         *
         * @return A [PlantData] with a wider scale and offset.
         */
        private fun wide(): PlantData =
            PlantData(
                scale = Vector3f(0.4f, 0.9f, 0.4f),
                translation = Vector3f(-0.2f, 0.24f, -0.2f),
            )

        /**
         * Creates a configuration for leaf blocks.
         *
         * @return A [PlantData] roughly filling the pot's width.
         */
        private fun leaves(): PlantData =
            PlantData(
                scale = Vector3f(0.5f, 0.9f, 0.5f),
                translation = Vector3f(-0.25f, 0.24f, -0.25f),
            )

        /**
         * Creates a configuration for plants rendered hanging upside down.
         *
         * @param baseY The Y offset of the plant, accounting for its flipped height.
         * @return A flipped, full-scale [PlantData].
         */
        private fun hanging(baseY: Float): PlantData =
            PlantData(
                scale = Vector3f(1f, 1f, 1f),
                translation = Vector3f(-0.5f, baseY, 0.5f),
                upsideDown = true,
            )
    }
}
