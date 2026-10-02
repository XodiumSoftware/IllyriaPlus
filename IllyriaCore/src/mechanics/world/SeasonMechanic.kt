package org.xodium.illyriacore.mechanics.world

import dev.wyck.biome.ClimateSettings
import dev.wyck.biome.CustomBiome
import dev.wyck.biome.TemperatureModifier
import dev.wyck.keys.ResourceKey
import dev.wyck.renderer.packet.PacketHandler

import dev.wyck.renderer.packet.data.VirtualBiome
import dev.wyck.renderer.updater.BiomeUpdater
import io.papermc.paper.command.brigadier.Commands

import org.bukkit.World
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.Schedule.schedule
import org.xodium.illyriacore.dialogs.SeasonDialog
import org.xodium.illyriacore.enums.SeasonStateEnum
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

    private var lastSeasonDay: Long = -1L
    private var activeVirtualBiome: VirtualBiome? = null

    private val biomes: Map<SeasonStateEnum, CustomBiome> by lazy {
        mapOf(
            SeasonStateEnum.SPRING to
                CustomBiome
                    .builder()
                    .resourceKey(ResourceKey.of(IllyriaCore.ID, SeasonStateEnum.SPRING.name.lowercase()))
                    .foliageColor("#7CFC00")
                    .dryFoliageColor("#6B8E23")
                    .grassColor("#32CD32")
                    .waterColor("#4169E1")
                    .climateSettings(ClimateSettings.of(true, 0.7f, TemperatureModifier.NONE, 0.4f))
                    .build(),
            SeasonStateEnum.SUMMER to
                CustomBiome
                    .builder()
                    .resourceKey(ResourceKey.of(IllyriaCore.ID, SeasonStateEnum.SUMMER.name.lowercase()))
                    .climateSettings(ClimateSettings.of(true, 0.8f, TemperatureModifier.NONE, 0.5f))
                    .build(),
            SeasonStateEnum.AUTUMN to
                CustomBiome
                    .builder()
                    .resourceKey(ResourceKey.of(IllyriaCore.ID, SeasonStateEnum.AUTUMN.name.lowercase()))
                    .foliageColor("#D2691E")
                    .dryFoliageColor("#8B4513")
                    .grassColor("#CD853F")
                    .waterColor("#4682B4")
                    .climateSettings(ClimateSettings.of(true, 0.6f, TemperatureModifier.NONE, 0.5f))
                    .build(),
            SeasonStateEnum.WINTER to
                CustomBiome
                    .builder()
                    .resourceKey(ResourceKey.of(IllyriaCore.ID, SeasonStateEnum.WINTER.name.lowercase()))
                    .foliageColor("#F0F0F0")
                    .dryFoliageColor("#E0E0E0")
                    .grassColor("#C0E8C0")
                    .waterColor("#B0C4DE")
                    .climateSettings(ClimateSettings.of(true, 0.2f, TemperatureModifier.NONE, 0.3f))
                    .build(),
        )
    }

    override fun register(): Long =
        super.register() +
            measureTime {
                schedule(period = CHECK_INTERVAL) {
                    val world = instance.server.worlds.firstOrNull() ?: return@schedule
                    val currentSeasonDay = (world.fullTime / TICKS_PER_DAY) % SeasonStateEnum.DAYS_PER_YEAR
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
        activeVirtualBiome?.let { PacketHandler.of(instance).dismissBiome(it) }
        activeVirtualBiome = null
        lastSeasonDay = -1L
    }

    /**
     * Swaps the active virtual biome to the current season's palette.
     *
     * @param world The world whose season to apply.
     */
    internal fun swapBiome(world: World) {
        val season = SeasonStateEnum.currentSeason(world)
        val handler = PacketHandler.of(instance)
        val updater = BiomeUpdater.of(instance)

        activeVirtualBiome?.let { handler.dismissBiome(it) }
        val virtualBiome =
            VirtualBiome
                .builder()
                .biome(biomes.getValue(season))
                .build()
        handler.appendBiome(virtualBiome)
        activeVirtualBiome = virtualBiome

        instance.server.onlinePlayers.forEach { updater.updateChunksForPlayer(it) }
    }
}
