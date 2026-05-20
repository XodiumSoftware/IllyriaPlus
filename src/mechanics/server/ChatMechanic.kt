package org.xodium.illyriaplus.mechanics.server

import com.google.gson.JsonParser
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
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.IllyriaPlus.Companion.prefix
import org.xodium.illyriaplus.Utils.CommandUtils.executesCatching
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.interfaces.MechanicInterface
import java.awt.Color
import java.net.URI
import javax.imageio.ImageIO
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

/** Represents a mechanic handling chat formatting within the system. */
@OptIn(ExperimentalUuidApi::class)
internal object ChatMechanic : MechanicInterface {
    private const val CHAT_FORMAT: String = "<player_head> <player> <reset><mango>›</gradient> <message>"
    private const val WHISPER_TO_FORMAT: String =
        "<skyline>You</gradient> <mango>➛</gradient> <player> <reset><mango>›</gradient> <message>"
    private const val WHISPER_FROM_FORMAT: String =
        "<player> <reset><mango>➛</gradient> <skyline>You</gradient> <mango>›</gradient> <message>"
    private const val DELETE_SYMBOL: String = "<dark_gray>[<dark_red><b>X</b></dark_red><dark_gray>]"
    private const val CLICK_TO_WHISPER_MSG: String = "<mango>Click to Whisper</gradient>"
    private const val CLICK_TO_DELETE_MSG: String = "<mango>Click to delete your message</gradient>"
    private const val FACE_X = 8
    private const val FACE_Y = 8
    private const val FACE_WIDTH = 8
    private const val FACE_HEIGHT = 8
    private const val PIXEL_CHAR = "█"

    private val faceCache = mutableMapOf<Uuid, String>()
    private val JOIN_BANNER_TEXT: List<String> =
        listOf(
            "<mango_r>]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|" +
                "[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[</gradient>",
            "<image><mango>⯈</gradient>",
            "<image><mango>⯈</gradient>",
            "<image><mango>⯈</gradient> " +
                "<firewatch>Welcome</gradient> <player> " +
                "<click:suggest_command:'/nickname '>" +
                "<hover:show_text:'<mango>Set your nickname!</gradient>'>" +
                "<white><sprite:items:item/name_tag></white></hover></click> " +
                "<click:suggest_command:'/locator '>" +
                "<hover:show_text:'<mango>Change your locator color!</gradient>'>" +
                "<white><sprite:items:item/compass_00></white></hover></click>",
            "<image><mango>⯈</gradient>",
            "<image><mango>⯈</gradient> " +
                "<firewatch>Check out</gradient><gray>:</gray>",
            "<image><mango>⯈</gradient> <gray>✦</gray> " +
                "<click:run_command:'/rules'><deep-ocean>/rules</gradient></click:run_command>",
            "<image><mango>⯈</gradient> <gray>✦</gray> " +
                "<click:open_url:'https://vanillaplus.xodium.org'>" +
                "<deep-ocean>wiki</gradient></click:open_url>",
            "<image><mango>⯈</gradient>",
            "<mango_r>]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|" +
                "[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[=]|[</gradient>",
        )
    private val PLAYER_IS_NOT_ONLINE_MSG: String =
        "${instance.prefix} <firewatch>Player is not Online!</gradient>"

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
                                                ?: return@executesCatching sender.sendMessage(
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
    fun on(event: PlayerJoinEvent) = joinBanner(event.player)

    /**
     * Handles asynchronous chat events.
     *
     * @param event The [AsyncChatEvent] to be processed.
     */
    private fun asyncChat(event: AsyncChatEvent) {
        event.renderer(ChatRenderer.defaultRenderer())
        event.renderer { player, displayName, message, audience ->
            val processedMessage = replaceItemPlaceholder(message, player)
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
                    Placeholder.component("message", processedMessage),
                )

            if (audience == player) base = base.appendSpace().append(createDeleteCross(event.signedMessage()))

            base
        }
    }

    /**
     * Replaces ['item'] placeholder with a hoverable component of the player's held item.
     *
     * @param message The chat message component.
     * @param player The player whose held item to display.
     * @return The message with ['item'] replaced, or the original message if hand is empty.
     */
    private fun replaceItemPlaceholder(
        message: Component,
        player: Player,
    ): Component {
        val heldItem = player.inventory.itemInMainHand

        if (heldItem.type == Material.AIR) return message

        val itemComponent =
            MM
                .deserialize("<mango>[</gradient>")
                .append(heldItem.displayName())
                .append(MM.deserialize("<mango>]</gradient>"))
                .hoverEvent(heldItem.asHoverEvent())

        return message.replaceText(
            TextReplacementConfig
                .builder()
                .match("\\[item]|\\[i]")
                .replacement(itemComponent)
                .build(),
        )
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
            .clickEvent(ClickEvent.callback { instance.server.deleteMessage(signedMessage) })

    /**
     * Generates a MiniMessage string representing the player's face.
     *
     * @param size Output size in pixels.
     * @return The rendered face string.
     */
    private fun Player.face(size: Int = 8): String {
        faceCache[uniqueId.toKotlinUuid()]?.let { return it }

        val skinUrl =
            playerProfile.properties
                .find { it.name == "textures" }
                ?.let { JsonParser.parseString(Base64.decode(it.value).decodeToString()).asJsonObject }
                ?.getAsJsonObject("textures")
                ?.getAsJsonObject("SKIN")
                ?.get("url")
                ?.asString
                ?: error("Player has no skin texture")

        val fullImg =
            ImageIO.read(URI.create(skinUrl).toURL())
                ?: error("Failed to load skin image from URL: $skinUrl")

        val face = fullImg.getSubimage(FACE_X, FACE_Y, FACE_WIDTH, FACE_HEIGHT)
        val scale = FACE_WIDTH.toDouble() / size

        return (0 until size)
            .joinToString("\n") { y ->
                (0 until size).joinToString("") { x ->
                    val px = (x * scale).toInt()
                    val py = (y * scale).toInt()
                    val color = Color(face.getRGB(px, py), true)

                    if (color.alpha == 0) {
                        "<color:#000000>$PIXEL_CHAR</color>"
                    } else {
                        "<color:#%02x%02x%02x>$PIXEL_CHAR</color>".format(color.red, color.green, color.blue)
                    }
                }
            }.also { faceCache[uniqueId.toKotlinUuid()] = it }
    }
}
