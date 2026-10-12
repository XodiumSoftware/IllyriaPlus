package org.xodium.illyriacore.dialogs

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import org.xodium.illyriacore.mechanics.server.TabListMechanic.tablist
import org.xodium.illyriacore.pdcs.PlayerPDC.nickname
import org.xodium.illyrialib.Utils.MM

/** Dialog for configuring a player's nickname. */
internal object NicknameDialog : DialogInterface {
    override fun invoke(player: Player): Dialog =
        Dialog.create {
            it
                .empty()
                .base(
                    DialogBase
                        .builder(MM.deserialize("<firewatch>Nickname</gradient>"))
                        .body(
                            listOf(
                                DialogBody.plainMessage(
                                    MM.deserialize(
                                        "<yellow>1. Configure your nickname at: " +
                                            "<aqua><click:copy_to_clipboard:www.birdflop.com/resources/rgb/>" +
                                            "www.birdflop.com/resources/rgb/" +
                                            "</aqua>\n" +
                                            "2. Set <red>output</red> format to: <green>MiniMessage</green>\n" +
                                            "3. Copy <red>output</red> from the site → paste into the " +
                                            "<red>input</red> below.",
                                    ),
                                ),
                            ),
                        ).inputs(
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
                                    ClickCallback
                                        .Options
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
                                    ClickCallback
                                        .Options
                                        .builder()
                                        .uses(ClickCallback.UNLIMITED_USES)
                                        .build(),
                                ),
                            ).build(),
                    ),
                )
        }

    /** Applies the player's stored nickname to their display name. */
    fun Player.nickname() = displayName(MM.deserialize(nickname))

    /**
     * Sets the player's nickname to the given name, applies it, and updates the tab list.
     *
     * @param name The new nickname. Blank or empty clears the nickname.
     */
    fun Player.nickname(name: String) {
        nickname = name
        nickname()
        playerListName(displayName())
        tablist(this)
    }
}
