package org.xodium.illyriacore.mechanics.world

import dev.wyck.biome.ClimateSettings
import dev.wyck.biome.CustomBiome
import dev.wyck.biome.TemperatureModifier
import dev.wyck.keys.ResourceKey
import dev.wyck.renderer.packet.PacketHandler
import dev.wyck.renderer.packet.data.VirtualBiome
import dev.wyck.renderer.updater.BiomeUpdater
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Material
import org.bukkit.World
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.Schedule.schedule
import org.xodium.illyriacore.dialogs.SeasonDialog
import org.xodium.illyriacore.enums.SeasonStateEnum
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyriacore.mechanics.server.TabListMechanic
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
    private lateinit var packetHandler: PacketHandler

    private val biomes: Map<SeasonStateEnum, CustomBiome> by lazy {
        mapOf(
            SeasonStateEnum.SPRING to
                CustomBiome
                    .builder()
                    .resourceKey(ResourceKey.of(IllyriaCore.ID, SeasonStateEnum.SPRING.name.lowercase()))
                    .climateSettings(ClimateSettings.of(true, 0.7f, TemperatureModifier.NONE, 0.5f))
                    .register(),
            SeasonStateEnum.SUMMER to
                CustomBiome
                    .builder()
                    .resourceKey(ResourceKey.of(IllyriaCore.ID, SeasonStateEnum.SUMMER.name.lowercase()))
                    .climateSettings(ClimateSettings.of(false, 0.8f, TemperatureModifier.NONE, 0.0f))
                    .register(),
            SeasonStateEnum.AUTUMN to
                CustomBiome
                    .builder()
                    .resourceKey(ResourceKey.of(IllyriaCore.ID, SeasonStateEnum.AUTUMN.name.lowercase()))
                    .climateSettings(ClimateSettings.of(true, 0.6f, TemperatureModifier.NONE, 0.6f))
                    .register(),
            SeasonStateEnum.WINTER to
                CustomBiome
                    .builder()
                    .resourceKey(ResourceKey.of(IllyriaCore.ID, SeasonStateEnum.WINTER.name.lowercase()))
                    .climateSettings(ClimateSettings.of(true, 0.0f, TemperatureModifier.NONE, 0.4f))
                    .register(),
        )
    }

    override fun register(): Long =
        super.register() +
            measureTime {
                packetHandler = PacketHandler.of(instance).register()
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
        activeVirtualBiome?.let { packetHandler.dismissBiome(it) }
        activeVirtualBiome = null
        lastSeasonDay = -1L
        packetHandler.unregister()
    }

    /**
     * Swaps the active virtual biome to the current season's climate settings.
     *
     * @param world The world whose season to apply.
     */
    internal fun swapBiome(world: World) {
        val season = SeasonStateEnum.currentSeason(world)
        val updater = BiomeUpdater.of(instance)

        activeVirtualBiome?.let { packetHandler.dismissBiome(it) }
        val builder =
            VirtualBiome
                .builder()
                .biome(biomes.getValue(season))
        if (season == SeasonStateEnum.WINTER) {
            builder.replacement(Material.WATER, Material.ICE)
        }
        val virtualBiome = builder.build()
        packetHandler.appendBiome(virtualBiome)
        activeVirtualBiome = virtualBiome

        instance.server.onlinePlayers.forEach {
            updater.updateChunksForPlayer(it)
            TabListMechanic.tablist(it)
        }
    }
}
