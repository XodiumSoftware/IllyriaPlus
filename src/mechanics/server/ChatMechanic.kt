package org.xodium.illyriaplus.mechanics.server

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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.Command.executesCatching
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.mechanics.MechanicInterface

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
    private const val JOIN_SUBTITLE = "<mango>Press: <key:key.quickActions> for Quick Actions!</gradient>"

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
                                            instance.logger.warning(
                                                "Command can only be executed by a Player!",
                                            )
                                        }

                                        val sender = it.source.sender as Player
                                        val targetResolver =
                                            it.getArgument("target", PlayerSelectorArgumentResolver::class.java)
                                        val target =
                                            targetResolver.resolve(it.source).singleOrNull()
                                                ?: return@executesCatching sender.sendActionBar(
                                                    MM.deserialize(PLAYER_IS_NOT_ONLINE_MSG),
                                                )
                                        val message = it.getArgument("message", String::class.java)

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
                "${instance.javaClass.simpleName}.whisper".lowercase(),
                "Allows use of the whisper command",
                PermissionDefault.TRUE,
            ),
        )

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: AsyncChatEvent) = asyncChat(event)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) = handleJoin(event)

    @EventHandler
    fun on(event: PlayerQuitEvent) = handleQuit(event)

    /**
     * Handles player join chat mechanics.
     *
     * @param event The PlayerJoinEvent triggered when a player joins.
     */
    private fun handleJoin(event: PlayerJoinEvent) {
        instance.server.onlinePlayers.forEach { it.addCustomChatCompletions(listOf("@${event.player.name}")) }
        syncMentionCompletions(event.player)
        event.player.showTitle(
            Title.title(
                MM.deserialize(JOIN_TITLE, Placeholder.component("player", event.player.displayName())),
                MM.deserialize(JOIN_SUBTITLE),
            ),
        )
        event.player.playSound(event.player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
    }

    /**
     * Handles player quit chat mechanics.
     *
     * @param event The PlayerQuitEvent triggered when a player quits.
     */
    private fun handleQuit(event: PlayerQuitEvent) {
        instance.server.onlinePlayers.forEach { it.removeCustomChatCompletions(listOf("@${event.player.name}")) }
    }

    /**
     * Adds @-prefixed names for all online players to the given player's chat completions.
     *
     * @param player The player to update completions for.
     */
    private fun syncMentionCompletions(player: Player) {
        player.addCustomChatCompletions(instance.server.onlinePlayers.map { "@${it.name}" })
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
                            .replacePosPlaceholder(player)
                            .replaceMentions(player),
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
     * Replaces @mentions with a highlighted component and sends a title notification.
     *
     * @param player The player sending the message.
     * @return The message with @mentions replaced.
     */
    private fun Component.replaceMentions(player: Player): Component {
        val plain = PlainTextComponentSerializer.plainText().serialize(this)
        val mentions = "(?<!\\w)@\\w+(?!\\w)".toRegex().findAll(plain).map { it.value }.toSet()

        if (mentions.isEmpty()) return this

        var result = this

        val notified = mutableSetOf<Player>()

        for (mention in mentions) {
            val name = mention.removePrefix("@")
            val target = instance.server.onlinePlayers.find { it.name.equals(name, ignoreCase = true) }

            if (target != null && target != player && target !in notified) {
                target.showTitle(
                    Title.title(
                        MM.deserialize("<red>Mentioned</red>"),
                        MM.deserialize(
                            "<white><player> mentioned you in the chat!</white>",
                            Placeholder.component("player", player.displayName()),
                        ),
                    ),
                )
                notified.add(target)
            }

            result =
                result.replaceText(
                    TextReplacementConfig
                        .builder()
                        .matchLiteral(mention)
                        .replacement(MM.deserialize("<yellow>$mention</yellow>"))
                        .build(),
                )
        }

        return result
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
