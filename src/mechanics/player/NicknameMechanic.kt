@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.xodium.illyriaplus.mechanics.player

import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.CommandUtils.playerExecuted
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.interfaces.MechanicInterface
import org.xodium.illyriaplus.mechanics.server.TabListMechanic.tablist
import org.xodium.illyriaplus.pdcs.PlayerPDC.nickname

/** Represents a mechanic handling player nicknames within the system. */
internal object NicknameMechanic : MechanicInterface {
    const val UPDATE_NICKNAME_MSG: String = "<firewatch>Nickname has been updated to: <nickname></gradient>"

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("nickname")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> player.nickname("") }
                    .then(
                        Commands
                            .argument("name", StringArgumentType.greedyString())
                            .playerExecuted { player, ctx ->
                                player.nickname(StringArgumentType.getString(ctx, "name"))
                                player.playerListName(player.displayName())
                                tablist(player)
                            },
                    ),
                "Allows players to set or remove their nickname",
                listOf("nick"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.nickname".lowercase(),
                "Allows use of the nickname command",
                PermissionDefault.TRUE,
            ),
        )

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) {
        handleJoin(event)
    }

    /**
     * Applies the player's stored nickname on join.
     *
     * @param event The PlayerJoinEvent triggered when a player joins.
     */
    private fun handleJoin(event: PlayerJoinEvent) {
        event.player.nickname()
    }

    /** Applies the player's stored nickname to their display name. */
    private fun Player.nickname() = displayName(MM.deserialize(nickname))

    /**
     * Sets the player's nickname to the given name, applies it, and sends a confirmation.
     *
     * @param name The new nickname. Blank or empty clears the nickname.
     */
    private fun Player.nickname(name: String) {
        nickname = name
        nickname()
        sendActionBar(MM.deserialize(UPDATE_NICKNAME_MSG, Placeholder.component("nickname", displayName())))
    }
}
