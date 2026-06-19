package org.xodium.illyriaplus.data

import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface

/**
 * Holds the metadata for a painting variant.
 *
 * @property name The registry key fragment (snake_case) identifying this painting variant.
 * @property size The width and height of the painting in blocks, represented as `Pair(width, height)`.
 * @property author The namespace/author key for the painting asset.
 * @property title The display title derived from [name], converted to proper case.
 */
internal data class PaintingData(
    override val name: String,
    override val size: Pair<Int, Int>,
    override val author: String,
    override val title: String = name.snakeToProperCase(),
) : PaintingInterface
