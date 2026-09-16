package org.xodium.illyriabridge.bridges

import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import org.xodium.illyriabridge.IllyriaBridge.Companion.instance
import kotlin.time.measureTime

/**
 * Manages Jade (What Am I Looking At) plugin channel synchronization.
 * Replies to the Jade client handshake so the overlay enables its server-connected mode.
 *
 * An empty handshake is sent (no server config, providers, or shearable blocks), so Jade
 * renders tooltips with its full client-side information without sending data requests.
 */
internal object JadeBridge : BridgeInterface, PluginMessageListener {
    private const val CLIENT_HANDSHAKE_CHANNEL = "jade:client_handshake"
    private const val SERVER_HANDSHAKE_CHANNEL = "jade:server_handshake"

    /** Empty server handshake body: four VarInt-zero collection sizes (config, blocks, providers). */
    private val EMPTY_HANDSHAKE = ByteArray(4)

    override fun register(): Long =
        super.register() +
            measureTime {
                instance.server.messenger.registerIncomingPluginChannel(instance, CLIENT_HANDSHAKE_CHANNEL, this)
                instance.server.messenger.registerOutgoingPluginChannel(instance, SERVER_HANDSHAKE_CHANNEL)
            }.inWholeMilliseconds

    override fun onPluginMessageReceived(
        channel: String,
        player: Player,
        message: ByteArray,
    ) {
        if (channel == CLIENT_HANDSHAKE_CHANNEL) {
            player.sendPluginMessage(instance, SERVER_HANDSHAKE_CHANNEL, EMPTY_HANDSHAKE)
        }
    }
}
