package org.xodium.illyriaplus.data

import org.xodium.illyriaplus.Utils.snakeToProperCase

internal data class PaintingData(
    val name: String,
    val size: Pair<Int, Int>,
    val author: String,
    val title: String = name.snakeToProperCase(),
)
