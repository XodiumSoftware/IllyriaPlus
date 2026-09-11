package org.xodium.illyriabridge.bridges

import io.netty.buffer.Unpooled
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.DiscardedPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeMap
import net.minecraft.world.item.crafting.RecipeSerializer
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.xodium.illyriabridge.IllyriaBridge.Companion.instance
import org.xodium.illyriabridge.payloads.FabricRecipeSyncPayload
import kotlin.time.measureTime

/**
 * Manages recipe synchronization for Fabric clients connecting to the server.
 * Listens for player join events and sends appropriate recipe data to Fabric clients.
 */
internal object FabricRecipeBridge : BridgeInterface {
    private const val RECIPE_CHANNEL = "fabric:recipe_sync"

    override fun register(): Long =
        super.register() +
            measureTime {
                instance.server.messenger.registerOutgoingPluginChannel(instance, RECIPE_CHANNEL)
            }.inWholeMilliseconds

    /**
     * Handles player join events by checking the client brand and sending
     * Fabric-compatible recipe sync payloads if the client is running Fabric.
     *
     * @param event The player join event
     */
    @EventHandler
    fun on(event: PlayerJoinEvent) {
        val originalPlayer = event.player
        val brand = originalPlayer.clientBrandName ?: return

        if (!brand.equals("fabric", ignoreCase = true)) return

        val player = (originalPlayer as CraftPlayer).handle
        val server = player.level().server
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess())

        sendFabricPayload(player, server.recipeManager, buffer)
    }

    /**
     * Sends a Fabric recipe sync payload to the specified player.
     * Groups recipes by their serializer type and encodes them for Fabric clients.
     *
     * @param player The server player to send the payload to
     * @param recipeManager The recipe manager containing all server recipes
     * @param buffer The buffer to encode recipe data into
     */
    private fun sendFabricPayload(
        player: ServerPlayer,
        recipeManager: RecipeManager,
        buffer: RegistryFriendlyByteBuf,
    ) {
        val entries = groupRecipesBySerializer(recipeManager.recipes)
        val payload = FabricRecipeSyncPayload(entries)

        FabricRecipeSyncPayload.CODEC.encode(buffer, payload)

        val bytes = ByteArray(buffer.writerIndex())

        buffer.getBytes(0, bytes)

        sendPayload(player, Identifier.fromNamespaceAndPath("fabric", "recipe_sync"), bytes)
    }

    /**
     * Groups recipes by their serializer type.
     *
     * @param recipes The recipes to group
     * @return A list of entries, each containing a serializer and its recipes
     */
    private fun groupRecipesBySerializer(recipes: RecipeMap): List<FabricRecipeSyncPayload.Entry> {
        val seen = HashSet<RecipeSerializer<*>>()
        val entries = ArrayList<FabricRecipeSyncPayload.Entry>()

        for (serializer in BuiltInRegistries.RECIPE_SERIALIZER) {
            if (!seen.add(serializer)) continue

            val matchingRecipes = recipes.values().filter { it.value().serializer === serializer }

            if (matchingRecipes.isNotEmpty()) entries.add(FabricRecipeSyncPayload.Entry(serializer, matchingRecipes))
        }

        return entries
    }

    /**
     * Sends a custom payload packet to the specified player.
     *
     * @param player The server player to send the packet to
     * @param id The identifier for the payload type
     * @param bytes The raw payload bytes
     */
    private fun sendPayload(
        player: ServerPlayer,
        id: Identifier,
        bytes: ByteArray,
    ) {
        player.connection.send(ClientboundCustomPayloadPacket(DiscardedPayload(id, bytes)))
    }
}
