package org.xodium.illyriaseasons

/**
 * Represents one of the four seasons.
 *
 * Unlike real-world seasons, IllyriaSeasons uses a Minecraft-scaled calendar where each season
 * is measured in **Minecraft days** (20 minutes real time each). This makes seasons cycle at
 * a pace appropriate for gameplay rather than real-world time.
 *
 * Default: 30 Minecraft days per season (each season = 10 hours of gameplay).
 *
 * @property lengthInDays the duration of this season in Minecraft days
 */
internal enum class SeasonState(
    val lengthInDays: Int,
) {
    SPRING(30),
    SUMMER(30),
    AUTUMN(30),
    WINTER(30),
    ;

    /** The season that follows this one. */
    fun next(): SeasonState = entries[(ordinal + 1) % entries.size]

    /** The season that precedes this one. */
    fun previous(): SeasonState = entries[(ordinal + entries.size - 1) % entries.size]

    companion object {
        /** Total Minecraft days in the seasonal year. */
        const val DAYS_PER_YEAR: Int = 120
    }
}
