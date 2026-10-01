package org.xodium.illyriaseasons

import dev.wyck.renderer.packet.PacketHandler
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/** Core seasonal state machine tracking the current position in the seasonal year. */
internal object SeasonSystem {
    private const val DATA_FILE = "data.yml"
    private const val KEY_DAY = "day"

    /** Current day within the seasonal year (1-based, 1–120). */
    var day: Int = 1
        private set

    /** Returns a [PacketHandler] for injecting virtual seasonal biomes. Not yet wired. */
    @Suppress("unused")
    fun packetHandler(): PacketHandler = PacketHandler.of(IllyriaSeasons.instance)

    /**
     * Returns the current season.
     *
     * @return The current [SeasonState].
     */
    fun currentSeason(): SeasonState = SeasonState.entries[((day - 1) / 30) % SeasonState.entries.size]

    /**
     * Returns the current day within the current season (1-based, 1–30).
     *
     * @return Day within the season.
     */
    fun dayInSeason(): Int = ((day - 1) % 30) + 1

    /** Loads the persisted seasonal state from disk. */
    fun load() {
        val file = File(IllyriaSeasons.instance.dataFolder, DATA_FILE)
        if (!file.exists()) return
        day = YamlConfiguration.loadConfiguration(file).getInt(KEY_DAY, 1)
    }

    /** Saves the current seasonal state to disk. */
    fun save() {
        val file = File(IllyriaSeasons.instance.dataFolder, DATA_FILE)
        file.parentFile?.mkdirs()
        YamlConfiguration()
            .apply {
                set(KEY_DAY, day)
            }.save(file)
    }

    /** Advances the seasonal year by one Minecraft day. */
    fun advance() {
        day = (day % SeasonState.DAYS_PER_YEAR) + 1
        if (day == 1 || dayInSeason() == 1) {
            IllyriaSeasons.instance.logger.info(
                "Advanced to ${currentSeason().name.lowercase()} " +
                    "(day ${dayInSeason()}/${currentSeason().lengthInDays}, " +
                    "day $day/${SeasonState.DAYS_PER_YEAR})",
            )
        }
    }

    /** Advances to the start of the next season. */
    fun advanceSeason() {
        val daysUntilNextSeason = currentSeason().lengthInDays - dayInSeason() + 1
        day += daysUntilNextSeason
    }

    /**
     * Sets the season day directly.
     *
     * @param day The day within the seasonal year to set (1-based, 1–120).
     */
    fun setDay(day: Int) {
        this.day = day.coerceIn(1, SeasonState.DAYS_PER_YEAR)
        save()
    }
}
