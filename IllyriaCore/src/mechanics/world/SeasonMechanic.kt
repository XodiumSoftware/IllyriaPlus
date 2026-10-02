package org.xodium.illyriacore.mechanics.world

import dev.wyck.renderer.packet.PacketHandler
import dev.wyck.renderer.packet.data.VirtualBiome
import dev.wyck.renderer.updater.BiomeUpdater
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.World
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.Schedule.schedule
import org.xodium.illyriacore.data.SeasonBiomes
import org.xodium.illyriacore.data.SeasonState
import org.xodium.illyriacore.dialogs.SeasonDialog
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.Utils.Command.requiresPlayer
import org.xodium.illyrialib.data.CommandData
import kotlin.time.measureTime

/** Represents a mechanic handling seasons within the system. */
internal object SeasonMechanic : MechanicInterface {
    private const val TICKS_PER_DAY: Long = 24000L
    private const val CHECK_INTERVAL: Long = TICKS_PER_DAY

    override val cmds: Collection<CommandData> =
        listOf(
            CommandData(
                Commands
                    .literal("seasons")
                    .requiresPlayer { isOp }
                    .playerExecuted { player, _ -> player.showDialog(SeasonDialog(player)) },
                "Inspects and controls the current season.",
            ),
        )

    private var task: BukkitTask? = null
    private var lastSeasonDay: Long = -1L
    private var activeVirtualBiome: VirtualBiome? = null

    override fun register(): Long =
        super.register() +
            measureTime {
                task =
                    schedule(period = CHECK_INTERVAL) {
                        val world = instance.server.worlds.firstOrNull() ?: return@schedule
                        val currentSeasonDay = (world.fullTime / TICKS_PER_DAY) % SeasonState.DAYS_PER_YEAR
                        if (currentSeasonDay != lastSeasonDay && lastSeasonDay >= 0) {
                            swapBiome(world)
                            lastSeasonDay = currentSeasonDay
                        } else if (lastSeasonDay < 0) {
                            lastSeasonDay = currentSeasonDay
                            swapBiome(world)
                        }
                    }
            }.inWholeMilliseconds

    override fun onDisable() {
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
    private fun swapBiome(world: World) {
        val season = SeasonState.currentSeason(world)
        val handler = PacketHandler.of(instance)
        val updater = BiomeUpdater.of(instance)

        activeVirtualBiome?.let { handler.dismissBiome(it) }
        val virtualBiome =
            VirtualBiome
                .builder()
                .biome(SeasonBiomes.of(season))
                .build()
        handler.appendBiome(virtualBiome)
        activeVirtualBiome = virtualBiome

        instance.server.onlinePlayers.forEach { updater.updateChunksForPlayer(it) }
    }
}
