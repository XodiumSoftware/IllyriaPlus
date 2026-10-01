package org.xodium.illyriaseasons

import dev.wyck.renderer.packet.data.VirtualBiome
import dev.wyck.renderer.updater.BiomeUpdater
import org.bukkit.scheduler.BukkitTask

/** Watches world time and swaps the active Wyck biome palette when the season changes. */
internal object SeasonTracker {
    private const val TICKS_PER_DAY: Long = 24000L
    private const val CHECK_INTERVAL: Long = TICKS_PER_DAY

    private var task: BukkitTask? = null
    private var lastSeasonDay: Long = -1L
    private var activeVirtualBiome: VirtualBiome? = null

    /** Starts the season tracker task. */
    fun start() {
        val plugin = IllyriaSeasons.instance
        task =
            plugin.server.scheduler.runTaskTimer(
                plugin,
                Runnable {
                    val world = plugin.server.worlds.firstOrNull() ?: return@Runnable
                    val currentSeasonDay = (world.fullTime / TICKS_PER_DAY) % SeasonState.DAYS_PER_YEAR
                    if (currentSeasonDay != lastSeasonDay && lastSeasonDay >= 0) {
                        swapBiome(world)
                        lastSeasonDay = currentSeasonDay
                    } else if (lastSeasonDay < 0) {
                        lastSeasonDay = currentSeasonDay
                        swapBiome(world)
                    }
                },
                0L,
                CHECK_INTERVAL,
            )
    }

    /** Stops the tracker task. */
    fun stop() {
        task?.cancel()
        task = null
        activeVirtualBiome = null
        lastSeasonDay = -1L
    }

    /**
     * Swaps the active virtual biome to the current season's palette.
     *
     * @param world The world whose season to apply.
     */
    private fun swapBiome(world: org.bukkit.World) {
        val season = SeasonState.currentSeason(world)
        val handler = IllyriaSeasons.instance.packetHandler
        val updater = BiomeUpdater.of(IllyriaSeasons.instance)

        activeVirtualBiome?.let { handler.dismissBiome(it) }
        val virtualBiome =
            VirtualBiome
                .builder()
                .biome(SeasonBiomes.of(season))
                .build()
        handler.appendBiome(virtualBiome)
        activeVirtualBiome = virtualBiome

        IllyriaSeasons.instance.server.onlinePlayers.forEach {
            updater.updateChunksForPlayer(it)
        }
    }
}
