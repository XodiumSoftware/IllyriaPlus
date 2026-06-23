package org.xodium.illyriaplus.data

import org.xodium.illyriaplus.Utils.snakeToProperCase

/**
 * Holds the metadata for a single painting variant.
 *
 * @property name The registry key fragment (snake_case) identifying this painting variant.
 * @property size The width and height of the painting in blocks, represented as `Pair(width, height)`.
 * @property author The namespace/author key for the painting asset.
 * @property title The display title derived from [name], converted to proper case.
 */
internal data class PaintingData(
    val name: String,
    val size: Pair<Int, Int>,
    val author: String,
    val title: String = name.snakeToProperCase(),
)
