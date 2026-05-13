@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Color
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.CommandUtils.playerExecuted
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.interfaces.MechanicInterface

/** Represents a mechanic handling locator functionality within the system. */
internal object LocatorMechanic : MechanicInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("locator")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .then(
                        Commands
                            .argument("color", ArgumentTypes.namedColor())
                            .playerExecuted { player, ctx ->
                                player.locator(ctx.getArgument("color", NamedTextColor::class.java))
                            },
                    ).then(
                        Commands
                            .argument("hex", ArgumentTypes.hexColor())
                            .playerExecuted { player, ctx ->
                                player.locator(ctx.getArgument("hex", TextColor::class.java))
                            },
                    ).then(
                        Commands
                            .literal("reset")
                            .playerExecuted { player, _ -> player.locator() },
                    ),
                "Allows players to personalise their locator bar",
                listOf("lc"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.locator".lowercase(),
                "Allows use of the locator command",
                PermissionDefault.TRUE,
            ),
        )

    /**
     * Sets waypoint color.
     *
     * @param color Optional color.
     */
    private fun Player.locator(color: TextColor? = null) {
        waypointColor = color?.let { Color.fromRGB(it.value()) }
        sendActionBar(Component.text("Locator color changed!", color))
    }
}
