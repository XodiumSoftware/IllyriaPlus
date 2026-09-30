package org.xodium.illyriabridge.bridges

import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.xodium.illyriabridge.IllyriaBridge.Companion.instance
import java.util.UUID

/**
 * Allows non-op players using the Armor Statues mod to edit armor stands
 * via client-sent vanilla commands (`/data merge entity`, `/attribute`).
 *
 * The mod sends commands through the chat pipeline; without the mod installed
 * on the server, these commands require OP. This bridge intercepts and
 * re-dispatches them with elevated permissions, but only when targeting
 * a nearby armor stand.
 *
 * Clients must have `overrideClientPermissionsCheck = true` in their
 * Armor Statues mod config for the mod to send commands without OP.
 */
internal object ArmorStatuesBridge : BridgeInterface {
    private const val DATA_MERGE_PREFIX = "data merge entity "
    private const val ATTRIBUTE_PREFIX = "attribute "
    private const val MAX_DISTANCE_SQUARED = 9.0
    private const val SCALING_ATTRIBUTE = "minecraft:generic.scale"

    @EventHandler(priority = EventPriority.LOWEST)
    fun on(event: PlayerCommandPreprocessEvent) {
        val message = event.message.removePrefix("/")

        when {
            message.startsWith(DATA_MERGE_PREFIX) -> handleDataMerge(event, message)
            message.startsWith(ATTRIBUTE_PREFIX) -> handleAttribute(event, message)
        }
    }

    /**
     * Handles `data merge entity <uuid> <nbt>` commands from Armor Statues.
     * Re-dispatches the command as console so the armor stand is modified without OP.
     *
     * @param event The preprocess event to cancel on success
     * @param message The raw command string (without leading slash)
     */
    private fun handleDataMerge(
        event: PlayerCommandPreprocessEvent,
        message: String,
    ) {
        val armorStand =
            findNearbyArmorStand(event.player, extractTargetUuid(message, DATA_MERGE_PREFIX) ?: return)
                ?: return

        event.isCancelled = true
        instance.server.dispatchCommand(
            instance.server.consoleSender,
            "execute as ${armorStand.uniqueId} run $message",
        )
    }

    /**
     * Handles `attribute <uuid> minecraft:generic.scale ...` commands from Armor Statues.
     * Re-dispatches the command as console so the armor stand scale is modified without OP.
     *
     * @param event The preprocess event to cancel on success
     * @param message The raw command string (without leading slash)
     */
    private fun handleAttribute(
        event: PlayerCommandPreprocessEvent,
        message: String,
    ) {
        if (!message.contains(SCALING_ATTRIBUTE)) return
        val armorStand =
            findNearbyArmorStand(event.player, extractTargetUuid(message, ATTRIBUTE_PREFIX) ?: return)
                ?: return

        event.isCancelled = true
        instance.server.dispatchCommand(
            instance.server.consoleSender,
            "execute as ${armorStand.uniqueId} run $message",
        )
    }

    /**
     * Extracts the target entity UUID from the command string.
     *
     * @param message The full command string
     * @param prefix The command prefix to strip
     * @return The UUID string, or null if parsing fails
     */
    private fun extractTargetUuid(
        message: String,
        prefix: String,
    ): String? {
        val rest = message.removePrefix(prefix)
        val endIndex = rest.indexOf(' ')
        return if (endIndex > 0) rest.substring(0, endIndex) else null
    }

    /**
     * Finds a nearby armor stand by its UUID that is within range of the given player.
     *
     * @param player The interacting player
     * @param targetUuid The UUID string of the target entity
     * @return The armor stand, or null if not found, wrong type, or out of range
     */
    private fun findNearbyArmorStand(
        player: Player,
        targetUuid: String,
    ): ArmorStand? {
        val entityUuid =
            try {
                UUID.fromString(targetUuid)
            } catch (_: IllegalArgumentException) {
                return null
            }
        val entity = instance.server.getEntity(entityUuid) ?: return null

        if (entity !is ArmorStand) return null
        if (entity.world != player.world) return null
        if (player.location.distanceSquared(entity.location) > MAX_DISTANCE_SQUARED) return null

        return entity
    }
}
