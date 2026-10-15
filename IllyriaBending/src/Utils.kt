package org.xodium.illyriabending

/**
 * Converts an integer to its Roman numeral representation.
 *
 * Only values in `1..5` are mapped; anything outside that range falls back to the number's
 * decimal string.
 *
 * @return The Roman numeral for this value, or its decimal string if out of range.
 */
internal fun Int.toRoman(): String =
    when (this) {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        4 -> "IV"
        5 -> "V"
        else -> toString()
    }
