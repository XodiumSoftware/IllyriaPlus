package org.xodium.illyriabridge.bridges

import com.google.common.io.ByteStreams
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerRegisterChannelEvent
import org.xodium.illyriabridge.IllyriaBridge.Companion.instance
import org.xodium.illyriabridge.bridges.XaeroMapBridge.idPath
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.random.Random
import kotlin.time.measureTime

/**
 * Manages Xaero's WorldMap and Minimap plugin channel synchronization.
 * Ensures Fabric clients receive the correct server-level world ID for map consistency.
 */
internal object XaeroMapBridge : BridgeInterface {
    private const val WORLDMAP_CHANNEL = "xaeroworldmap:main"
    private const val MINIMAP_CHANNEL = "xaerominimap:main"

    private val idPath by lazy { instance.dataFolder.toPath().resolve("xaeromap.id") }
    private val worldId: Int by lazy { loadId() }

    override fun register(): Long =
        super.register() +
            measureTime {
                instance.server.messenger.registerOutgoingPluginChannel(instance, WORLDMAP_CHANNEL)
                instance.server.messenger.registerOutgoingPluginChannel(instance, MINIMAP_CHANNEL)
                worldId
            }.inWholeMilliseconds

    /**
     * Handles player channel registration events by sending the server world ID
     * when a player registers a Xaero map channel.
     *
     * @param event The player register channel event
     */
    @EventHandler
    fun on(event: PlayerRegisterChannelEvent) {
        when (val channel = event.channel) {
            WORLDMAP_CHANNEL, MINIMAP_CHANNEL -> sendPlayerWorldId(event.player, channel)
            else -> return
        }
    }

    /**
     * Handles player world change events by re-sending the server world ID
     * for both WorldMap and Minimap channels.
     *
     * @param event The player changed world event
     */
    @EventHandler
    fun on(event: PlayerChangedWorldEvent) {
        event.player.apply {
            sendPlayerWorldId(this, WORLDMAP_CHANNEL)
            sendPlayerWorldId(this, MINIMAP_CHANNEL)
        }
    }

    /**
     * Sends the server world ID to the specified player via the given plugin channel.
     *
     * @param player The player to send the world ID to
     * @param channel The plugin channel to send the message on
     */
    private fun sendPlayerWorldId(
        player: Player,
        channel: String,
    ) {
        player.sendPluginMessage(
            instance,
            channel,
            ByteStreams
                .newDataOutput()
                .apply {
                    writeByte(0)
                    writeInt(worldId)
                }.toByteArray(),
        )
    }

    /**
     * Loads the server world ID from disk, creating it if necessary.
     * If [idPath] exists, its content is parsed as an integer and returned.
     * Otherwise, a new random ID is generated, persisted, and returned.
     *
     * @return The loaded or created world ID, or `0` on failure
     */
    private fun loadId(): Int =
        runCatching {
            when {
                idPath.exists() -> {
                    idPath.readText().trim().toInt()
                }

                else -> {
                    Random.nextInt().also {
                        idPath.parent.createDirectories()
                        idPath.writeText(it.toString())
                    }
                }
            }
        }.onFailure { instance.logger.severe("Error loading $idPath: ${it.message}") }.getOrDefault(0)
}
