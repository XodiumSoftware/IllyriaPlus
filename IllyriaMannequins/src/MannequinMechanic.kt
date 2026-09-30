package org.xodium.illyriamannequins

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.owner

/** Manages the mannequin and its interactions. */
@Suppress("UnstableApiUsage")
internal object MannequinMechanic : Listener {
    /** Registers the mannequin command and event listeners. */
    fun register() {
        instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                Commands
                    .literal("mannequin")
                    .requires { it.sender is Player && it.sender.isOp }
                    .executes { ctx ->
                        spawnMannequin(ctx.source.sender as Player)
                        Command.SINGLE_SUCCESS
                    }.build(),
                "Spawn a Mannequin at your location",
            )
        }
        instance.server.pluginManager.registerEvents(this, instance)
        MannequinHeadTracking.register()
        MannequinFollowing.register()
        MannequinHostility.register()
        MannequinCombat.register()
    }

    @EventHandler
    fun on(event: PlayerInteractAtEntityEvent) {
        val mannequin = event.rightClicked as? Mannequin ?: return
        if (event.player.uniqueId != mannequin.owner) return
        event.isCancelled = true
        if (event.player.isSneaking) {
            MannequinEquipment.swapItem(event, mannequin)
            return
        }
        MannequinEditDialog.show(event.player, mannequin)
    }

    /**
     * Spawns a mannequin at the player's location.
     *
     * @param player The player to spawn the mannequin for.
     */
    private fun spawnMannequin(player: Player) =
        player.world.spawn(player.location, Mannequin::class.java) {
            it.owner = player.uniqueId
            it.setAI(true)
        }
}
