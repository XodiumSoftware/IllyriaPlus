package org.xodium.illyriaplus.data

import org.xodium.illyriaplus.IllyriaPlus

/**
 * Metadata for a custom painting variant.
 *
 * @property key The registry key fragment (e.g. `alpha`).
 * @property size The painting size as a width/height pair.
 * @property title The translation key used for the painting's title.
 * @property author The translation key used for the painting's author.
 */
internal data class PaintingData(
    val key: String,
    val size: Pair<Int, Int>,
    val title: String = "painting.${IllyriaPlus.ID}.$key.title",
    val author: String = "painting.${IllyriaPlus.ID}.$key.author",
)
