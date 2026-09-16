package org.xodium.illyriabridge.bridges

import com.google.common.io.ByteStreams
import io.papermc.paper.event.world.WorldGameRuleChangeEvent
import org.bukkit.GameRules
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRegisterChannelEvent
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriabridge.IllyriaBridge.Companion.instance
import java.util.UUID
import kotlin.time.measureTime

/**
 * Manages AppleSkin plugin channel synchronization.
 * Syncs saturation, exhaustion, and natural regeneration state to clients.
 */
internal object AppleSkinBridge : BridgeInterface {
    private const val SATURATION_CHANNEL = "appleskin:saturation"
    private const val EXHAUSTION_CHANNEL = "appleskin:exhaustion"
    private const val NATURAL_REGENERATION_CHANNEL = "appleskin:natural_regeneration"

    private const val MINIMUM_EXHAUSTION_CHANGE_THRESHOLD = 0.01f

    private val syncTasks = mutableMapOf<UUID, BukkitTask>()

    override fun register(): Long =
        super.register() +
            measureTime {
                instance.server.messenger.registerOutgoingPluginChannel(instance, SATURATION_CHANNEL)
                instance.server.messenger.registerOutgoingPluginChannel(instance, EXHAUSTION_CHANNEL)
                instance.server.messenger.registerOutgoingPluginChannel(instance, NATURAL_REGENERATION_CHANNEL)
            }.inWholeMilliseconds

    @EventHandler
    fun on(event: PlayerRegisterChannelEvent) {
        val player = event.player

        when (event.channel) {
            SATURATION_CHANNEL -> startSyncTask(player)
            NATURAL_REGENERATION_CHANNEL -> sendNaturalRegenState(player)
        }
    }

    @EventHandler
    fun on(event: PlayerChangedWorldEvent) {
        if (event.player.listeningPluginChannels.contains(NATURAL_REGENERATION_CHANNEL)) {
            sendNaturalRegenState(event.player)
        }
    }

    @EventHandler
    fun on(event: PlayerQuitEvent) {
        syncTasks.remove(event.player.uniqueId)?.cancel()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun on(event: WorldGameRuleChangeEvent) {
        if (event.gameRule != GameRules.NATURAL_HEALTH_REGENERATION) return

        event.world.players.forEach {
            if (it.listeningPluginChannels.contains(NATURAL_REGENERATION_CHANNEL)) {
                sendNaturalRegenState(it, event.value.toBoolean())
            }
        }
    }

    /**
     * Starts a per-tick task that syncs saturation and exhaustion to the player.
     * Any existing task for the player is cancelled first. The task cancels itself
     * when the player goes offline.
     *
     * @param player The player to start syncing for
     */
    private fun startSyncTask(player: Player) {
        syncTasks[player.uniqueId]?.cancel()

        var previousSaturation = -1f
        var previousExhaustion = -1f

        lateinit var task: BukkitTask

        task =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable {
                    if (!player.isOnline) {
                        task.cancel()
                        syncTasks.remove(player.uniqueId)
                        return@Runnable
                    }

                    val saturation = player.saturation
                    if (saturation != previousSaturation) {
                        player.sendPluginMessage(
                            instance,
                            SATURATION_CHANNEL,
                            ByteStreams.newDataOutput().apply { writeFloat(saturation) }.toByteArray(),
                        )
                        previousSaturation = saturation
                    }

                    val exhaustion = player.exhaustion
                    if (kotlin.math.abs(exhaustion - previousExhaustion) >= MINIMUM_EXHAUSTION_CHANGE_THRESHOLD) {
                        player.sendPluginMessage(
                            instance,
                            EXHAUSTION_CHANNEL,
                            ByteStreams.newDataOutput().apply { writeFloat(exhaustion) }.toByteArray(),
                        )
                        previousExhaustion = exhaustion
                    }
                },
                1L,
                1L,
            )

        syncTasks[player.uniqueId] = task
    }

    /**
     * Sends the current natural regeneration gamerule state of the player's world.
     *
     * @param player The player to send the state to
     */
    private fun sendNaturalRegenState(player: Player) {
        sendNaturalRegenState(player, player.world.getGameRuleValue(GameRules.NATURAL_HEALTH_REGENERATION))
    }

    /**
     * Sends the given natural regeneration state to the player.
     *
     * @param player The player to send the state to
     * @param state Whether natural regeneration is enabled
     */
    private fun sendNaturalRegenState(
        player: Player,
        state: Boolean,
    ) {
        player.sendPluginMessage(
            instance,
            NATURAL_REGENERATION_CHANNEL,
            ByteStreams.newDataOutput().apply { writeByte(if (state) 1 else 0) }.toByteArray(),
        )
    }
}
