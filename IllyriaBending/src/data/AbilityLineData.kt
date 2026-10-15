package org.xodium.illyriabending.data

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.xodium.illyrialib.Utils.MM

/**
 * A line of text with its score value.
 *
 * @property text The MiniMessage formatted text.
 * @property score The score value (higher = higher on sidebar).
 */
internal data class AbilityLineData(
    val text: String,
    val score: Int,
) {
    /**
     * Calculates the score value for sidebar display.
     *
     * @param index The line index.
     * @param total The total number of lines.
     * @return The score value (higher = higher on sidebar).
     */
    fun scoreValue(
        index: Int,
        total: Int,
    ): Int = total - index

    /**
     * Converts the MiniMessage text to plain text for scoreboard entry names.
     *
     * @return Plain text representation.
     */
    fun toPlainText(): String = PlainTextComponentSerializer.plainText().serialize(MM.deserialize(text))
}
