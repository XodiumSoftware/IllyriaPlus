package org.xodium.illyriabending

/** Converts an integer to its Roman numeral representation. Supports values in `1..5`; anything outside that range falls back to the number's decimal string. */
internal fun Int.toRoman(): String =
    when (this) {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        4 -> "IV"
        5 -> "V"
        else -> toString()
    }
