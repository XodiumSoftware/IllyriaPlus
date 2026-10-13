package org.xodium.illyriacore.mechanics.player

import io.papermc.paper.connection.PlayerConfigurationConnection
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import io.papermc.paper.event.player.PlayerCustomClickEvent
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.xodium.illyriacore.dialogs.BendingDialog
import org.xodium.illyriacore.enums.BendingTypeEnum
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyriacore.pdcs.PlayerPDC.bendingType
import org.xodium.illyrialib.Utils.MM
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Represents a mechanic handling player bending selection within the system.
 *
 * Blocks new players in the configuration phase until they choose a [BendingTypeEnum]
 * via the registry-registered [BendingDialog]. Players who already have a stored type
 * skip the dialog entirely.
 */
internal object BendingMechanic : MechanicInterface {
    /** Timeout (in minutes) before a player is disconnected for not choosing a bending type. */
    private const val SELECTION_TIMEOUT_MINUTES = 1L

    /** Pending responses for players currently in the configuration phase. */
    private val awaitingSelection = ConcurrentHashMap<UUID, CompletableFuture<BendingTypeEnum?>>()

    /** Selections made during configuration, persisted to PDC on [PlayerJoinEvent]. */
    private val pendingPersist = ConcurrentHashMap<UUID, BendingTypeEnum>()

    @EventHandler
    fun on(event: AsyncPlayerConnectionConfigureEvent) {
        val connection = event.connection
        val profileId = connection.profile.id ?: return

        val dialog: Dialog =
            RegistryAccess
                .registryAccess()
                .getRegistry(RegistryKey.DIALOG)
                .get(BendingDialog.DIALOG_KEY) ?: return

        val response = CompletableFuture<BendingTypeEnum?>()
        response.completeOnTimeout(null, SELECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        awaitingSelection[profileId] = response

        val audience = connection.audience
        audience.showDialog(dialog)

        val selection = response.join()

        audience.closeDialog()
        awaitingSelection.remove(profileId)

        if (selection == null) {
            connection.disconnect(MM.deserialize("<red>You must choose a bending type to join.</red>"))
            return
        }

        pendingPersist[profileId] = selection
    }

    @EventHandler
    fun on(event: PlayerCustomClickEvent) {
        val connection = event.commonConnection as? PlayerConfigurationConnection ?: return
        val profileId = connection.profile.id ?: return

        val type =
            BendingTypeEnum.entries.firstOrNull {
                event.identifier == BendingDialog.clickKey(it)
            } ?: return

        awaitingSelection[profileId]?.complete(type)
    }

    @EventHandler
    fun on(event: PlayerJoinEvent) {
        pendingPersist.remove(event.player.uniqueId)?.let { event.player.bendingType = it }
    }

    @EventHandler
    fun on(event: PlayerQuitEvent) {
        awaitingSelection.remove(event.player.uniqueId)
        pendingPersist.remove(event.player.uniqueId)
    }
}
