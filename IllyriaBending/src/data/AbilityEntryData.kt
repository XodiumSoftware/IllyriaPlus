package org.xodium.illyriabending.data

/**
 * Represents an ability in the scoreboard list.
 *
 * @property name The ability display name.
 * @property isPassive Whether this is a passive ability (shown in different color).
 */
internal data class AbilityEntryData(
    val name: String,
    val isPassive: Boolean = false,
)
