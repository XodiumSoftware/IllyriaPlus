package org.xodium.illyriaplus.data

/**
 * Metadata for a custom painting variant.
 *
 * @property name The registry/resource name of the painting (e.g. `alpha`).
 * @property width The width of the painting in blocks.
 * @property height The height of the painting in blocks.
 */
internal data class PaintingData(
    val name: String,
    val width: Int,
    val height: Int,
)
