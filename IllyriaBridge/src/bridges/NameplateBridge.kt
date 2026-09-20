package org.xodium.illyriabridge.bridges

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.adventure.PaperAdventure
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.xodium.illyriabridge.IllyriaBridge.Companion.instance
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Rewrites outgoing player nametag metadata to use Bukkit display names. */
internal object NameplateBridge : BridgeInterface {
    private const val HANDLER_NAME = "illyria_nametag_bridge"
    private const val CUSTOM_NAME_ID = 2
    private const val CUSTOM_NAME_VISIBLE_ID = 3

    private val injectedPlayers = ConcurrentHashMap.newKeySet<UUID>()

    override fun register(): Long =
        super.register().also {
            instance.server.onlinePlayers.forEach {
                inject(it)
                refreshViewer(it)
            }
        }

    @EventHandler
    fun on(event: PlayerJoinEvent) {
        inject(event.player)
        refreshViewer(event.player)
    }

    @EventHandler
    fun on(event: PlayerChangedWorldEvent) {
        refreshViewer(event.player)
    }

    @EventHandler
    fun on(event: PlayerQuitEvent) {
        injectedPlayers.remove(event.player.uniqueId)
    }

    /** Injects the packet handler into the player's channel if it is not already present. */
    private fun inject(player: Player) {
        if (!injectedPlayers.add(player.uniqueId)) return

        val channel =
            (player as CraftPlayer)
                .handle
                .connection
                .connection
                .channel
        if (channel.pipeline().get(HANDLER_NAME) != null) return

        channel.pipeline().addBefore("packet_handler", HANDLER_NAME, Handler())
    }

    /** Sends display-name nametag metadata for every other online player to the given viewer. */
    private fun refreshViewer(viewer: Player) {
        val connection = (viewer as CraftPlayer).handle.connection

        instance.server.onlinePlayers
            .asSequence()
            .filter { it.uniqueId != viewer.uniqueId }
            .filter { it.world == viewer.world }
            .forEach { target ->
                connection.send(ClientboundSetEntityDataPacket(target.entityId, createCustomNameMetadata(target)))
            }
    }

    /** Creates metadata entries for the custom name and its visibility. */
    private fun createCustomNameMetadata(player: Player): List<SynchedEntityData.DataValue<*>> =
        listOf(
            SynchedEntityData.DataValue(
                CUSTOM_NAME_ID,
                EntityDataSerializers.OPTIONAL_COMPONENT,
                Optional.of(PaperAdventure.asVanilla(player.displayName())),
            ),
            SynchedEntityData.DataValue(
                CUSTOM_NAME_VISIBLE_ID,
                EntityDataSerializers.BOOLEAN,
                true,
            ),
        )

    /** Handles outgoing packets and rewrites player nametag metadata. */
    private class Handler : ChannelDuplexHandler() {
        override fun write(
            ctx: ChannelHandlerContext,
            msg: Any,
            promise: ChannelPromise,
        ) {
            super.write(ctx, rewrite(msg), promise)
        }

        /** Rewrites packets containing player metadata to replace nametags with display names. */
        private fun rewrite(msg: Any): Any =
            when (msg) {
                is ClientboundSetEntityDataPacket -> rewriteMetadataPacket(msg)
                else -> msg
            }

        /** Rewrites metadata packets for players to replace custom names with display names. */
        private fun rewriteMetadataPacket(packet: ClientboundSetEntityDataPacket): Any {
            val player = findPlayer(packet.id) ?: return packet
            val playerMetadata = createCustomNameMetadata(player)
            val filteredMetadata = filterNametagData(packet.packedItems)

            return ClientboundSetEntityDataPacket(packet.id, filteredMetadata + playerMetadata)
        }

        /** Removes vanilla nametag metadata entries from the list. */
        private fun filterNametagData(
            items: List<SynchedEntityData.DataValue<*>>,
        ): List<SynchedEntityData.DataValue<*>> =
            items.filterNot { it.id == CUSTOM_NAME_ID || it.id == CUSTOM_NAME_VISIBLE_ID }

        /** Finds the Bukkit player matching the given entity ID. */
        private fun findPlayer(entityId: Int): Player? =
            instance.server.onlinePlayers.firstOrNull { it.entityId == entityId }
    }
}
