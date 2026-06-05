package org.xodium.illyriaplus.mechanics.server

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Material
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.Command.playerExecuted
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.guis.FaqGui
import org.xodium.illyriaplus.mechanics.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

internal object FaqMechanic : MechanicInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("faq")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> FaqGui.open(player) },
                "Opens the FAQ Gui",
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.faq".lowercase(),
                "Allows use of the /faq command",
                PermissionDefault.TRUE,
            ),
        )

    override val faqTab = FaqTab.SERVER_MECHANIC

    override val faqItem: Item =
        Item.simple(
            ItemBuilder(Material.BOOK)
                .setName(MM.deserialize("<mango>Server FAQ</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize("<yellow>/faq</yellow> <firewatch>></gradient> <white>Opens the FAQ GUI</white>"),
                    MM.deserialize(""),
                    MM.deserialize("<white>Categories:</white>"),
                    MM.deserialize("  <yellow>•</yellow> Player Mechanics"),
                    MM.deserialize("  <yellow>•</yellow> World Mechanics"),
                    MM.deserialize("  <yellow>•</yellow> Entity Mechanics"),
                    MM.deserialize("  <yellow>•</yellow> Server Mechanics"),
                    MM.deserialize("  <yellow>•</yellow> Recipes"),
                ),
        )
}
