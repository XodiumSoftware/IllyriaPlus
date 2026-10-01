package org.xodium.illyriaseasons

/**
 * Represents one of the four real-world seasons.
 *
 * Aligned to the northern hemisphere meteorological calendar (non-leap year, 365 days):
 * - [SPRING]: March 1 – May 31 (92 days)
 * - [SUMMER]: June 1 – August 31 (92 days)
 * - [AUTUMN]: September 1 – November 30 (91 days)
 * - [WINTER]: December 1 – February 28 (90 days)
 *
 * @property lengthInDays the duration of this season in days
 * @property startDayOfYear the day of year this season starts on (1-based)
 */
internal enum class SeasonState(
    val lengthInDays: Int,
    val startDayOfYear: Int,
) {
    SPRING(lengthInDays = 92, startDayOfYear = 60),
    SUMMER(lengthInDays = 92, startDayOfYear = 152),
    AUTUMN(lengthInDays = 91, startDayOfYear = 244),
    WINTER(lengthInDays = 90, startDayOfYear = 335),
    ;

    /** The season that follows this one. */
    fun next(): SeasonState = entries[(ordinal + 1) % entries.size]

    /** The season that precedes this one. */
    fun previous(): SeasonState = entries[(ordinal + entries.size - 1) % entries.size]

    companion object {
        /** Total days in the (non-leap) seasonal year. */
        const val DAYS_PER_YEAR: Int = 365

        /**
         * Returns the season containing the given day of year.
         *
         * @param dayOfYear the day of year (1-based, 1–365)
         * @return the season containing the given day
         */
        fun of(dayOfYear: Int): SeasonState {
            val day = ((dayOfYear - 1).mod(DAYS_PER_YEAR)) + 1
            return entries.single { season ->
                val endDay = season.startDayOfYear + season.lengthInDays
                if (endDay > DAYS_PER_YEAR) {
                    day >= season.startDayOfYear || day < endDay - DAYS_PER_YEAR
                } else {
                    day in season.startDayOfYear until endDay
                }
            }
        }

        /**
         * Returns how far the given day of year has progressed through its season.
         *
         * @param dayOfYear the day of year (1-based, 1–365)
         * @return progress through the current season, in the range `[0.0, 1.0)`
         */
        fun progress(dayOfYear: Int): Double {
            val season = of(dayOfYear)
            val day = ((dayOfYear - 1).mod(DAYS_PER_YEAR)) + 1
            val offset = (day - season.startDayOfYear).mod(DAYS_PER_YEAR)
            return offset.toDouble() / season.lengthInDays
        }
    }
}
