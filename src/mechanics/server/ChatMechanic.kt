package org.xodium.illyriaplus.mechanics.server

import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils.CommandUtils.executesCatching
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.PlayerUtils.face
import org.xodium.illyriaplus.Utils.prefix
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.interfaces.MechanicInterface

/** Represents a mechanic handling chat formatting within the system. */
internal object ChatMechanic : MechanicInterface {
    private val JOIN_BANNER_TEXT: List<String> =
        listOf(
            "<gradient:#FFA751:#FFE259>]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|" +
                "[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[</gradient>",
            "<image><gradient:#FFE259:#FFA751>⯈</gradient>",
            "<image><gradient:#FFE259:#FFA751>⯈</gradient>",
            "<image><gradient:#FFE259:#FFA751>⯈</gradient> " +
                "<gradient:#CB2D3E:#EF473A>Welcome</gradient> <player> " +
                "<click:suggest_command:'/nickname '>" +
                "<hover:show_text:'<gradient:#FFE259:#FFA751>Set your nickname!</gradient>'>" +
                "<white><sprite:items:item/name_tag></white></hover></click> " +
                "<click:suggest_command:'/locator '>" +
                "<hover:show_text:'<gradient:#FFE259:#FFA751>Change your locator color!</gradient>'>" +
                "<white><sprite:items:item/compass_00></white></hover></click>",
            "<image><gradient:#FFE259:#FFA751>⯈</gradient>",
            "<image><gradient:#FFE259:#FFA751>⯈</gradient> " +
                "<gradient:#CB2D3E:#EF473A>Check out</gradient><gray>:</gray>",
            "<image><gradient:#FFE259:#FFA751>⯈</gradient> <gray>✦</gray> " +
                "<click:run_command:'/rules'><gradient:#13547a:#80d0c7>/rules</gradient></click:run_command>",
            "<image><gradient:#FFE259:#FFA751>⯈</gradient> <gray>✦</gray> " +
                "<click:open_url:'https://vanillaplus.xodium.org'>" +
                "<gradient:#13547a:#80d0c7>wiki</gradient></click:open_url>",
            "<image><gradient:#FFE259:#FFA751>⯈</gradient>",
            "<gradient:#FFA751:#FFE259>]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|" +
                "[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[</gradient>",
        )

    private const val CHAT_FORMAT: String =
        "<player_head> <player> <reset><gradient:#FFE259:#FFA751>›</gradient> <message>"
    private const val WHISPER_TO_FORMAT: String =
        "<gradient:#1488CC:#2B32B2>You</gradient> <gradient:#FFE259:#FFA751>➛</gradient> " +
            "<player> <reset><gradient:#FFE259:#FFA751>›</gradient> <message>"
    private const val WHISPER_FROM_FORMAT: String =
        "<player> <reset><gradient:#FFE259:#FFA751>➛</gradient> " +
            "<gradient:#1488CC:#2B32B2>You</gradient> <gradient:#FFE259:#FFA751>›</gradient> <message>"
    private const val DELETE_SYMBOL: String = "<dark_gray>[<dark_red><b>X</b></dark_red><dark_gray>]"
    private const val CLICK_TO_WHISPER_MSG: String = "<gradient:#FFE259:#FFA751>Click to Whisper</gradient>"
    private const val CLICK_TO_DELETE_MSG: String = "<gradient:#FFE259:#FFA751>Click to delete your message</gradient>"

    private val PLAYER_IS_NOT_ONLINE_MSG: String =
        "${IllyriaPlus.instance.prefix} <gradient:#CB2D3E:#EF473A>Player is not Online!</gradient>"

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("whisper")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .then(
                        Commands
                            .argument("target", ArgumentTypes.player())
                            .then(
                                Commands
                                    .argument("message", StringArgumentType.greedyString())
                                    .executesCatching {
                                        if (it.source.sender !is Player) {
                                            IllyriaPlus.instance.logger.warning(
                                                "Command can only be executed by a Player!",
                                            )
                                        }

                                        val sender = it.source.sender as Player
                                        val targetResolver =
                                            it.getArgument("target", PlayerSelectorArgumentResolver::class.java)
                                        val target =
                                            targetResolver.resolve(it.source).singleOrNull()
                                                ?: return@executesCatching sender.sendMessage(
                                                    MM.deserialize(PLAYER_IS_NOT_ONLINE_MSG),
                                                )
                                        val message = it.getArgument("message", String().javaClass)

                                        whisper(sender, target, message)
                                    },
                            ),
                    ),
                "This command allows you to whisper to players",
                listOf("w", "msg", "tell", "tellraw"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${IllyriaPlus.instance.javaClass.simpleName}.whisper".lowercase(),
                "Allows use of the whisper command",
                PermissionDefault.TRUE,
            ),
        )

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: AsyncChatEvent) = asyncChat(event)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) = joinBanner(event.player)

    /**
     * Handles asynchronous chat events.
     *
     * @param event The [AsyncChatEvent] to be processed.
     */
    private fun asyncChat(event: AsyncChatEvent) {
        event.renderer(ChatRenderer.defaultRenderer())
        event.renderer { player, displayName, message, audience ->
            var base =
                MM.deserialize(
                    CHAT_FORMAT,
                    Placeholder.component("player_head", MM.deserialize("<head:${player.uniqueId}>")),
                    Placeholder.component(
                        "player",
                        displayName
                            .clickEvent(ClickEvent.suggestCommand("/w ${player.name} "))
                            .hoverEvent(
                                HoverEvent.showText(MM.deserialize(CLICK_TO_WHISPER_MSG)),
                            ),
                    ),
                    Placeholder.component("message", message),
                )

            if (audience == player) base = base.appendSpace().append(createDeleteCross(event.signedMessage()))
            base
        }
    }

    /**
     * Handles the whisper command.
     *
     * @param sender The player who sent the command.
     * @param target The player to whom the message is being sent.
     * @param message The message to be sent.
     */
    private fun whisper(
        sender: Player,
        target: Player,
        message: String,
    ) {
        sender.sendMessage(
            MM.deserialize(
                WHISPER_TO_FORMAT,
                Placeholder.component(
                    "player",
                    target
                        .displayName()
                        .clickEvent(ClickEvent.suggestCommand("/w ${target.name} "))
                        .hoverEvent(HoverEvent.showText(MM.deserialize(CLICK_TO_WHISPER_MSG))),
                ),
                Placeholder.component("message", MM.deserialize(message)),
            ),
        )

        target.sendMessage(
            MM.deserialize(
                WHISPER_FROM_FORMAT,
                Placeholder.component(
                    "player",
                    sender
                        .displayName()
                        .clickEvent(ClickEvent.suggestCommand("/w ${sender.name} "))
                        .hoverEvent(HoverEvent.showText(MM.deserialize(CLICK_TO_WHISPER_MSG))),
                ),
                Placeholder.component("message", MM.deserialize(message)),
            ),
        )
    }

    /**
     * Sends the welcome banner to the player on join.
     *
     * @param player The player who joined.
     */
    private fun joinBanner(player: Player) {
        var imageIndex = 0

        player.sendMessage(
            MM.deserialize(
                Regex("<image>").replace(JOIN_BANNER_TEXT.joinToString("\n")) { "<image${++imageIndex}>" },
                Placeholder.component("player", player.displayName()),
                *player
                    .face()
                    .lines()
                    .mapIndexed { i, line -> Placeholder.component("image${i + 1}", MM.deserialize(line)) }
                    .toTypedArray(),
            ),
        )
    }

    /**
     * Creates to delete cross-component for message deletion.
     *
     * @param signedMessage The signed message to be deleted.
     * @return A [net.kyori.adventure.text.Component] representing the delete cross with hover text and click action.
     */
    private fun createDeleteCross(signedMessage: SignedMessage): Component =
        MM
            .deserialize(DELETE_SYMBOL)
            .hoverEvent(MM.deserialize(CLICK_TO_DELETE_MSG))
            .clickEvent(ClickEvent.callback { IllyriaPlus.instance.server.deleteMessage(signedMessage) })
}
