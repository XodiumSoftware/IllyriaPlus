package org.xodium.illyriacore.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.xodium.illyriacore.dialogs.NicknameDialog
import org.xodium.illyriacore.dialogs.NicknameDialog.nickname
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.data.CommandData

/** Represents a mechanic handling player nicknames within the system. */
internal object NicknameMechanic : MechanicInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("nickname")
                    .playerExecuted { player, _ -> player.showDialog(NicknameDialog(player)) },
                "Opens the nickname dialog",
                listOf("nick"),
            ),
        )

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) {
        val player = event.player
        player.nickname()
        nametag(player)
    }

    @EventHandler
    fun on(event: PlayerQuitEvent) {
        event.player.passengers
            .filterIsInstance<TextDisplay>()
            .forEach { it.remove() }
    }

    /**
     * Spawns a floating text display above the player's head showing their nickname.
     *
     * The display is mounted as a passenger so it follows the player automatically.
     * It hides when the player sneaks and removes the vanilla nameplate to avoid duplication.
     *
     * @param player The player to create the nametag for.
     */
    private fun nametag(player: Player) {
        val display =
            player.world.spawnEntity(
                player.location.add(0.0, 2.2, 0.0),
                EntityType.TEXT_DISPLAY,
            ) as TextDisplay
        display.text(player.displayName())
        display.billboard = Display.Billboard.CENTER
        display.isVisibleByDefault = !player.isSneaking
        player.addPassenger(display)
        player.customName(null)
    }
}
