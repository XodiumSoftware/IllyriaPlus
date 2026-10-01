package org.xodium.illyriaseasons

import dev.wyck.renderer.packet.data.VirtualBiome
import dev.wyck.renderer.updater.BiomeUpdater
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/** Core seasonal state machine tracking the current position in the seasonal year. */
internal object SeasonSystem {
    private const val DATA_FILE = "data.yml"
    private const val KEY_DAY = "day"

    /** Current day within the seasonal year (1-based, 1–120). */
    var day: Int = 1
        private set

    /** Returns the current season. */
    fun currentSeason(): SeasonState = SeasonState.entries[((day - 1) / 30) % SeasonState.entries.size]

    /** Returns the current day within the current season (1-based, 1–30). */
    fun dayInSeason(): Int = ((day - 1) % 30) + 1

    /** Loads the persisted seasonal state from disk. */
    fun load() {
        val file = File(IllyriaSeasons.instance.dataFolder, DATA_FILE)
        if (!file.exists()) return
        day = YamlConfiguration.loadConfiguration(file).getInt(KEY_DAY, 1)
        onSeasonChanged(currentSeason())
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

    /** Advances the seasonal year by one Minecraft day. Triggers biome switch on season change. */
    fun advance() {
        val previousSeason = currentSeason()
        day = (day % SeasonState.DAYS_PER_YEAR) + 1
        if (day == 1 || dayInSeason() == 1) {
            onSeasonChanged(currentSeason(), previousSeason)
        }
    }

    /** Advances to the start of the next season. */
    fun advanceSeason() {
        val daysUntilNextSeason = currentSeason().lengthInDays - dayInSeason() + 1
        day += daysUntilNextSeason
        onSeasonChanged(currentSeason())
    }

    /**
     * Sets the season day directly.
     *
     * @param day The day within the seasonal year to set (1-based, 1–120).
     */
    fun setDay(day: Int) {
        this.day = day.coerceIn(1, SeasonState.DAYS_PER_YEAR)
        onSeasonChanged(currentSeason())
        save()
    }

    /**
     * Swaps the active Wyck virtual biome to the given season's palette and refreshes chunks
     * for all online players so clients render the correct colors immediately.
     */
    private fun onSeasonChanged(
        season: SeasonState,
        previous: SeasonState? = null,
    ) {
        val handler = IllyriaSeasons.instance.packetHandler
        val updater = BiomeUpdater.of(IllyriaSeasons.instance)

        // Remove previous season's virtual biome(s) and inject the current one.
        previous?.let {
            handler.removeBiome(SeasonBiomes.of(it).resourceKey())
        }
        handler.appendBiome(
            VirtualBiome
                .builder()
                .biome(SeasonBiomes.of(season))
                .build(),
        )

        // Resend chunks so colors update immediately.
        IllyriaSeasons
            .instance
            .server
            .onlinePlayers
            .forEach { updater.updateChunksForPlayer(it) }

        IllyriaSeasons.instance.logger.info(
            "Season: ${previous?.name?.lowercase()} → ${season.name.lowercase()} " +
                "(day $day/${SeasonState.DAYS_PER_YEAR})",
        )
    }
}
