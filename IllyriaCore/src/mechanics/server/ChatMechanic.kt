package org.xodium.illyriacore.mechanics.server

import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyrialib.data.CommandData

/** Represents a mechanic handling chat formatting within the system. */
internal object ChatMechanic : MechanicInterface {
    private const val CHAT_FORMAT = "<player_head> <player> <reset><mango>›</gradient> <message>"
    private const val WHISPER_TO_FORMAT =
        "<skyline>You</gradient> <mango>➛</gradient> <player> <reset><mango>›</gradient> <message>"
    private const val WHISPER_FROM_FORMAT =
        "<player> <reset><mango>➛</gradient> <skyline>You</gradient> <mango>›</gradient> <message>"
    private const val DELETE_SYMBOL = "<dark_gray>[<dark_red><b>X</b></dark_red><dark_gray>]"
    private const val CLICK_TO_WHISPER_MSG = "<mango>Click to Whisper</gradient>"
    private const val CLICK_TO_DELETE_MSG = "<mango>Click to delete your message</gradient>"
    private const val PLAYER_IS_NOT_ONLINE_MSG = "<firewatch>Player is not Online!</gradient>"
    private const val JOIN_TITLE = "<firewatch><b>Welcome</b></gradient> <player>"
    private const val JOIN_SUBTITLE = "<mango>Check out /rules!</gradient>"

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("whisper")
                    .then(
                        Commands
                            .argument("target", ArgumentTypes.player())
                            .then(
                                Commands
                                    .argument("message", StringArgumentType.greedyString())
                                    .playerExecuted { player, ctx ->
                                        whisper(
                                            player,
                                            ctx
                                                .getArgument("target", PlayerSelectorArgumentResolver::class.java)
                                                .resolve(ctx.source)
                                                .singleOrNull()
                                                ?: return@playerExecuted player.sendActionBar(
                                                    MM.deserialize(PLAYER_IS_NOT_ONLINE_MSG),
                                                ),
                                            ctx.getArgument("message", String::class.java),
                                        )
                                    },
                            ),
                    ),
                "This command allows you to whisper to players",
                listOf("w", "msg", "tell", "tellraw"),
            ),
        )

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: AsyncChatEvent) = asyncChat(event)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) = handleJoin(event)

    /**
     * Handles player join chat mechanics.
     *
     * @param event The PlayerJoinEvent triggered when a player joins.
     */
    private fun handleJoin(event: PlayerJoinEvent) {
        event.player.showTitle(
            Title.title(
                MM.deserialize(JOIN_TITLE, Placeholder.component("player", event.player.displayName())),
                MM.deserialize(JOIN_SUBTITLE),
            ),
        )
        event.player.playSound(event.player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.0f)
    }

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
                            .hoverEvent(HoverEvent.showText(MM.deserialize(CLICK_TO_WHISPER_MSG))),
                    ),
                    Placeholder.component(
                        "message",
                        message
                            .replaceItemPlaceholder(player)
                            .replacePosPlaceholder(player),
                    ),
                )

            if (audience == player) base = base.appendSpace().append(createDeleteCross(event.signedMessage()))

            base
        }
    }

    /**
     * Replaces ['item'] placeholder with a hoverable component of the player's held item.
     *
     * @param player The player whose held item to display.
     * @return The message with ['item'] replaced, or the original message if hand is empty.
     */
    private fun Component.replaceItemPlaceholder(player: Player): Component {
        val heldItem = player.inventory.itemInMainHand

        if (heldItem.type == Material.AIR) return this

        return replaceText(
            TextReplacementConfig
                .builder()
                .match("\\[item]|\\[i]")
                .replacement(heldItem.displayName().hoverEvent(heldItem.asHoverEvent()))
                .build(),
        )
    }

    /**
     * Replaces ['pos'] with the player's current block position.
     *
     * @param player The player whose position to display.
     * @return The message with ['pos'] replaced.
     */
    private fun Component.replacePosPlaceholder(player: Player): Component =
        replaceText(
            TextReplacementConfig
                .builder()
                .matchLiteral("[pos]")
                .replacement(
                    MM.deserialize(
                        "<yellow>W:</yellow> ${player.location.world.name}, " +
                            "<red>X:</red> ${player.location.blockX}, " +
                            "<green>Y:</green> ${player.location.blockY}, " +
                            "<aqua>Z:</aqua> ${player.location.blockZ}",
                    ),
                ).build(),
        )

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
     * Creates to delete cross-component for message deletion.
     *
     * @param signedMessage The signed message to be deleted.
     * @return A [net.kyori.adventure.text.Component] representing the delete cross with hover text and click action.
     */
    private fun createDeleteCross(signedMessage: SignedMessage): Component =
        MM
            .deserialize(DELETE_SYMBOL)
            .hoverEvent(MM.deserialize(CLICK_TO_DELETE_MSG))
            .clickEvent(ClickEvent.callback { instance.server.deleteMessage(signedMessage) })
}
