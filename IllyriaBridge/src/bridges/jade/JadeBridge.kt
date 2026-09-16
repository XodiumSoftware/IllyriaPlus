package org.xodium.illyriabridge.bridges.jade

import io.netty.buffer.Unpooled
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.DiscardedPayload
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener
import org.xodium.illyriabridge.IllyriaBridge.Companion.instance
import org.xodium.illyriabridge.bridges.BridgeInterface
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.time.measureTime

/**
 * Manages Jade (What Am I Looking At) plugin channel synchronization.
 * Replies to the client handshake to enable server-connected mode and routes block
 * data requests to the registered [JadeProvider]s.
 *
 * All server→client payloads are sent as raw [ClientboundCustomPayloadPacket]s, because
 * Bukkit's `sendPluginMessage` silently drops messages for Fabric clients that never
 * registered the channel through the legacy `minecraft:register` handshake.
 */
internal object JadeBridge : BridgeInterface, PluginMessageListener {
    private const val CLIENT_HANDSHAKE_CHANNEL = "jade:client_handshake"
    private const val SERVER_HANDSHAKE_CHANNEL = "jade:server_handshake"
    private const val REQUEST_BLOCK_CHANNEL = "jade:request_block"
    private const val RECEIVE_DATA_CHANNEL = "jade:receive_data"

    /** The block data providers advertised in the handshake; request provider indices refer to this list. */
    internal val providers =
        listOf<JadeProvider>(
            JadeHopperLock,
        )

    /** The provider keys, in handshake order. */
    private val providerKeys = providers.map { it.key }

    override fun register(): Long =
        super.register() +
            measureTime {
                instance.server.messenger.registerIncomingPluginChannel(instance, CLIENT_HANDSHAKE_CHANNEL, this)
                instance.server.messenger.registerIncomingPluginChannel(instance, REQUEST_BLOCK_CHANNEL, this)
                instance.server.messenger.registerOutgoingPluginChannel(instance, SERVER_HANDSHAKE_CHANNEL)
                instance.server.messenger.registerOutgoingPluginChannel(instance, RECEIVE_DATA_CHANNEL)
            }.inWholeMilliseconds

    override fun onPluginMessageReceived(
        channel: String,
        player: Player,
        message: ByteArray,
    ) {
        when (channel) {
            CLIENT_HANDSHAKE_CHANNEL -> {
                instance.logger.info("[Jade] Handshake from ${player.name}, replying")
                send(player, SERVER_HANDSHAKE_CHANNEL, handshake(player))
            }

            REQUEST_BLOCK_CHANNEL -> handleBlockRequest(player, message)
        }
    }

    /**
     * Builds the server handshake: empty config map, empty shearable blocks,
     * the supported block providers, and empty entity providers.
     *
     * @param player The player the handshake is for
     * @return The encoded handshake payload
     */
    private fun handshake(player: Player): ByteArray {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(player))

        buf.writeVarInt(0) // serverConfig
        buf.writeVarInt(0) // shearableBlocks
        buf.writeVarInt(providerKeys.size)
        providerKeys.forEach { buf.writeUtf(it) }
        buf.writeVarInt(0) // entityProviderIds

        return buf.toByteArray()
    }

    /**
     * Handles a block data request by decoding the target position and requested provider indices,
     * letting each matching provider write into the response NBT, and replying with receive_data.
     *
     * @param player The requesting player
     * @param message The raw request payload
     */
    private fun handleBlockRequest(
        player: Player,
        message: ByteArray,
    ) {
        val buf = RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(message), registryAccess(player))

        buf.readBoolean() // showDetails
        val pos = buf.readBlockHitResult().blockPos // BlockHitResult
        ItemStack.OPTIONAL_STREAM_CODEC.decode(buf) // serversideRep
        if (buf.isReadable && buf.getByte(buf.readerIndex()).toInt() and 0xFF == 0x0A) {
            ByteBufCodecs.COMPOUND_TAG.decode(buf) // accessor data
        }

        val indices = (0 until buf.readVarInt()).map { buf.readVarInt() }
        val matched = indices.mapNotNull { providers.getOrNull(it) }.distinct()
        if (matched.isEmpty()) return

        val block = player.world.getBlockAt(pos.x, pos.y, pos.z)
        val tag = CompoundTag()
        tag.putInt("x", pos.x)
        tag.putInt("y", pos.y)
        tag.putInt("z", pos.z)
        tag.putString("BlockId", block.type.key.toString())

        var wrote = false
        matched.forEach { wrote = it.write(block, tag) || wrote }

        if (wrote) send(player, RECEIVE_DATA_CHANNEL, encodeNbt(tag))
    }

    /**
     * Encodes a CompoundTag in the unnamed network format (id byte + payload + TAG_End).
     *
     * @param tag The tag to encode
     * @return The encoded bytes
     */
    internal fun encodeNbt(tag: CompoundTag): ByteArray {
        val out = ByteArrayOutputStream()
        NbtIo.writeAnyTag(tag, DataOutputStream(out))
        return out.toByteArray()
    }

    /**
     * Sends a raw custom payload packet on the given channel, bypassing the Bukkit
     * listening-channel gate that drops messages for Fabric clients.
     *
     * @param player The player to send the payload to
     * @param channel The namespaced channel identifier (e.g. "jade:server_handshake")
     * @param bytes The raw payload bytes
     */
    internal fun send(
        player: Player,
        channel: String,
        bytes: ByteArray,
    ) {
        val (namespace, path) = channel.split(':', limit = 2)
        (player as CraftPlayer).handle.connection.send(
            ClientboundCustomPayloadPacket(DiscardedPayload(Identifier.fromNamespaceAndPath(namespace, path), bytes)),
        )
    }

    /**
     * Resolves a registry access for codec encoding/decoding from a player or the server.
     *
     * @param player The player, or null to use the server's access
     * @return The registry access
     */
    internal fun registryAccess(player: Player?) =
        player?.let { (it as CraftPlayer).handle.level().registryAccess() }
            ?: (org.bukkit.Bukkit.getServer() as CraftServer).handle.server.registryAccess()

    /**
     * Reads this buffer's readable bytes into a new byte array, starting at index 0.
     * The buffer's reader index is left unchanged.
     *
     * @return A byte array containing the buffer's readable bytes
     */
    internal fun RegistryFriendlyByteBuf.toByteArray(): ByteArray {
        val bytes = ByteArray(readableBytes())
        getBytes(0, bytes)
        return bytes
    }

    /**
     * Encodes a single boolean into Jade's codec format and stores it as a byte array under [key].
     *
     * @param value The boolean to encode
     * @return The 1-byte payload
     */
    internal fun boolPayload(value: Boolean): ByteArray {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess(null))
        buf.writeBoolean(value)
        return buf.toByteArray()
    }
}
