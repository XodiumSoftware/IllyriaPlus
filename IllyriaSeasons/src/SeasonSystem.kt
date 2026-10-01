package org.xodium.illyriaseasons

import dev.wyck.renderer.packet.PacketHandler
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.LocalDate

/** Core seasonal state machine tracking the current position in the seasonal year. */
internal object SeasonSystem {
    private const val DATA_FILE = "data.yml"
    private const val KEY_DAY_OF_YEAR = "day-of-year"

    /** Start day-of-year (1-based); defaults to today's real-world date. */
    private var dayOffset: Int = LocalDate.now().dayOfYear

    /** Returns a [PacketHandler] for injecting virtual seasonal biomes. Not yet wired. */
    @Suppress("unused")
    fun packetHandler(): PacketHandler = PacketHandler.of(IllyriaSeasons.instance)

    /**
     * Returns the current day-of-year (1–365) within the seasonal year.
     *
     * @return current day-of-year
     */
    fun dayOfYear(): Int = ((dayOffset - 1).mod(SeasonState.DAYS_PER_YEAR)) + 1

    /** Advances the seasonal year to the start of the next season. */
    fun advance() {
        val season = SeasonState.of(dayOfYear())
        val endOfSeason = season.lengthInDays - (SeasonState.progress(dayOfYear()) * season.lengthInDays).toInt()
        dayOffset += endOfSeason
        IllyriaSeasons.instance.logger.info(
            "Advanced to ${
                SeasonState.of(dayOfYear()).name.lowercase()
            } (day ${dayOfYear()}/${SeasonState.DAYS_PER_YEAR})",
        )
    }

    /** Loads the persisted seasonal state from disk. */
    fun load() {
        val file = File(IllyriaSeasons.instance.dataFolder, DATA_FILE)
        if (!file.exists()) return
        dayOffset =
            YamlConfiguration
                .loadConfiguration(file)
                .getInt(KEY_DAY_OF_YEAR, LocalDate.now().dayOfYear)
    }

    /** Saves the current seasonal state to disk. */
    fun save() {
        val file = File(IllyriaSeasons.instance.dataFolder, DATA_FILE)
        file.parentFile?.mkdirs()
        YamlConfiguration()
            .apply {
                set(KEY_DAY_OF_YEAR, dayOfYear())
            }.save(file)
    }

    /**
     * Sets the seasonal day-of-year directly.
     *
     * @param day the day of year to set (1-based, 1–365)
     */
    fun setDay(day: Int) {
        dayOffset = day.coerceIn(1, SeasonState.DAYS_PER_YEAR)
        save()
    }
}
