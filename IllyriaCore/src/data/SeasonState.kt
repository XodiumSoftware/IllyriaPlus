package org.xodium.illyriacore.data

import org.bukkit.World

/** Minecraft days per season. */
private const val DAYS_PER_SEASON: Int = 30

/**
 * Represents one of the four Minecraft calendar seasons.
 *
 * Seasons cycle automatically based on world age: each season is [DAYS_PER_SEASON] Minecraft
 * days (30 × 24,000 ticks = 30 × 20 minutes of real time). The state is purely derived from
 * `world.fullTime` — no persistence needed.
 *
 * @property lengthInDays the duration of this season in Minecraft days
 */
internal enum class SeasonState(
    val lengthInDays: Int,
) {
    SPRING(DAYS_PER_SEASON),
    SUMMER(DAYS_PER_SEASON),
    AUTUMN(DAYS_PER_SEASON),
    WINTER(DAYS_PER_SEASON),
    ;

    /** The season that follows this one. */
    fun next(): SeasonState = entries[(ordinal + 1) % entries.size]

    /** The season that precedes this one. */
    fun previous(): SeasonState = entries[(ordinal + entries.size - 1) % entries.size]

    companion object {
        /** Total Minecraft days in the seasonal year (4 × 30 days). */
        const val DAYS_PER_YEAR: Int = DAYS_PER_SEASON * 4

        /** Ticks in one Minecraft day (20 real minutes). */
        private const val TICKS_PER_DAY: Long = 24000L

        /** Season starting offset in ticks (to avoid day 0 = spring). */
        private const val SEASONAL_OFFSET: Long = 0L

        /**
         * Returns the current season for the given world.
         *
         * @param world The world to check.
         * @return The current [SeasonState].
         */
        fun currentSeason(world: World): SeasonState {
            val totalDays = world.fullTime / TICKS_PER_DAY
            return entries[((totalDays + SEASONAL_OFFSET / TICKS_PER_DAY) % DAYS_PER_YEAR / DAYS_PER_SEASON).toInt()]
        }

        /**
         * Returns the day within the current season for the given world (1-based).
         *
         * @param world The world to check.
         * @return Current day in the season (1–[DAYS_PER_SEASON]).
         */
        fun dayInSeason(world: World): Int {
            val totalDays = world.fullTime / TICKS_PER_DAY
            return ((totalDays + SEASONAL_OFFSET / TICKS_PER_DAY) % DAYS_PER_SEASON).toInt() + 1
        }

        /**
         * Returns the total days elapsed in the seasonal year (0–119).
         *
         * @param world The world to check.
         * @return Total seasonal days elapsed.
         */
        fun seasonalDay(world: World): Long = (world.fullTime / TICKS_PER_DAY + SEASONAL_OFFSET) % DAYS_PER_YEAR

        /**
         * Advances the world to the start of the next season.
         *
         * @param world The world to advance.
         * @return The new day of year (0-based) after skipping.
         */
        fun advanceSeason(world: World): Long {
            val currentDay = seasonalDay(world)
            val daysToNextSeason = DAYS_PER_SEASON - (currentDay % DAYS_PER_SEASON)
            val targetTicks = (currentDay + daysToNextSeason) * TICKS_PER_DAY
            world.fullTime = targetTicks
            return seasonalDay(world)
        }
    }
}
