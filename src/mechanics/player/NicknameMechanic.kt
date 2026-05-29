package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.interfaces.MechanicInterface
import org.xodium.illyriaplus.mechanics.server.TabListMechanic.tablist
import org.xodium.illyriaplus.pdcs.PlayerPDC.nickname
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling player nicknames within the system. */
internal object NicknameMechanic : MechanicInterface {
    override val faqTab = FaqTab.PLAYER_MECHANIC

    override val faqItem =
        Item
            .builder()
            .setItemProvider(
                ItemBuilder(Material.NAME_TAG)
                    .setName(MM.deserialize("<mango>Nickname</gradient>"))
                    .addLoreLines(MM.deserialize(""), MM.deserialize("<gray>Click to set your nickname</gray>")),
            ).addClickHandler { _, click ->
                val player = click.player

                if (player.hasPermission(perms[0])) player.showDialog(dialog(player))
            }.build()

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.nickname".lowercase(),
                "Allows to change your nickname",
                PermissionDefault.TRUE,
            ),
        )

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) = handleJoin(event)

    @Suppress("UnstableApiUsage")
    private fun dialog(player: Player): Dialog =
        Dialog.create {
            it
                .empty()
                .base(
                    DialogBase
                        .builder(MM.deserialize("<firewatch>Nickname</gradient>"))
                        .inputs(
                            listOf(
                                DialogInput
                                    .text("nickname", MM.deserialize("<gray>Enter nickname</gray>"))
                                    .width(200)
                                    .maxLength(4096)
                                    .initial(MM.serialize(player.displayName()))
                                    .labelVisible(true)
                                    .build(),
                            ),
                        ).build(),
                ).type(
                    DialogType.confirmation(
                        ActionButton
                            .builder(MM.deserialize("<red>Discard</red>"))
                            .action(
                                DialogAction.customClick(
                                    { _, _ -> },
                                    ClickCallback.Options
                                        .builder()
                                        .uses(ClickCallback.UNLIMITED_USES)
                                        .build(),
                                ),
                            ).build(),
                        ActionButton
                            .builder(MM.deserialize("<green>Save</green>"))
                            .action(
                                DialogAction.customClick(
                                    { response, _ -> player.nickname(response.getText("nickname") ?: "") },
                                    ClickCallback.Options
                                        .builder()
                                        .uses(ClickCallback.UNLIMITED_USES)
                                        .build(),
                                ),
                            ).build(),
                    ),
                )
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
        playerListName(displayName())
        tablist(this)
    }
}
