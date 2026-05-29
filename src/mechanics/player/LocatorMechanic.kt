package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.CommandUtils.playerExecuted
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling locator functionality within the system. */
internal object LocatorMechanic : MechanicInterface {
    private const val CURRENT_LOCATOR_MSG = "<gray>Current Locator color: </gray><hex>■"
    private const val NEW_LOCATOR_MSG = "<gray>New Locator color: </gray><hex>■"
    private const val DEFAULT_LOCATOR_MSG = "<gray>Locator color: default/not custom set</gray>"
    private const val RESET_LOCATOR_MSG = "<gray>Locator color has been reset!</gray>"

    override val faqTab = FaqTab.PLAYER

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.COMPASS)
                .setName(MM.deserialize("<mango>Locator</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize("<gray>cmd:</gray> <yellow>/locator <color></yellow>"),
                    MM.deserialize(
                        "<yellow>Hex Colors</yellow> <firewatch>></gradient> <white>/locator #RRGGBB</white>",
                    ),
                    MM.deserialize("<yellow>Reset</yellow> <firewatch>></gradient> <white>/locator reset</white>"),
                ),
        )

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("locator")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> player.locator() }
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
                            .playerExecuted { player, _ -> player.locator(null) },
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

    /** Shows the current locator color. */
    private fun Player.locator() =
        sendActionBar(
            waypointColor?.let {
                MM.deserialize(CURRENT_LOCATOR_MSG, Placeholder.styling("hex", TextColor.color(it.asRGB())))
            }
                ?: MM.deserialize(DEFAULT_LOCATOR_MSG),
        )

    /**
     * Sets waypoint color.
     *
     * @param color The color to apply, or null to reset.
     */
    private fun Player.locator(color: TextColor?) {
        waypointColor = color?.let { Color.fromRGB(it.value()) }
        sendActionBar(
            color?.let {
                MM.deserialize(NEW_LOCATOR_MSG, Placeholder.styling("hex", it))
            }
                ?: MM.deserialize(RESET_LOCATOR_MSG),
        )
    }
}
