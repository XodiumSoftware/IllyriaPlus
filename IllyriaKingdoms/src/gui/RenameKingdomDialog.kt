package org.xodium.illyriakingdoms.gui

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.entity.Player
import org.xodium.illyriakingdoms.Utils.MM
import org.xodium.illyriakingdoms.data.KingdomData

/** Builds and shows the rename kingdom dialog. */
internal object RenameKingdomDialog {
    /**
     * Shows the rename kingdom dialog to the given player.
     *
     * @param player The player to show the dialog to.
     * @param kingdom The kingdom being renamed.
     * @param onComplete Called after the dialog is dismissed (regardless of action), to reopen the GUI.
     */
    fun show(player: Player, kingdom: KingdomData, onComplete: (Player) -> Unit) {
        player.showDialog(
            Dialog.create {
                it
                    .empty()
                    .base(
                        DialogBase
                            .builder(MM.deserialize("<gold>Rename Kingdom"))
                            .body(
                                listOf(
                                    DialogBody.plainMessage(
                                        MM.deserialize("<gray>Enter a new name for your kingdom."),
                                    ),
                                ),
                            ).inputs(
                                listOf(
                                    DialogInput
                                        .text("name", MM.deserialize("<gray>New kingdom name"))
                                        .width(200)
                                        .maxLength(4096)
                                        .initial(MM.serialize(kingdom.name))
                                        .labelVisible(true)
                                        .build(),
                                ),
                            ).build(),
                    ).type(
                        DialogType.confirmation(
                            ActionButton
                                .builder(MM.deserialize("<red>Cancel</red>"))
                                .action(
                                    DialogAction.customClick(
                                        { _, _ -> onComplete(player) },
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
                                        { response, _ ->
                                            val newName = response.getText("name")
                                            if (!newName.isNullOrBlank()) {
                                                KingdomData.renameKingdom(kingdom.owner, MM.deserialize(newName))
                                                player.sendActionBar(
                                                    MM.deserialize("<green>Kingdom renamed."),
                                                )
                                            }
                                            onComplete(player)
                                        },
                                        ClickCallback
                                            .Options
                                            .builder()
                                            .uses(ClickCallback.UNLIMITED_USES)
                                            .build(),
                                    ),
                                ).build(),
                        ),
                    )
            },
        )
    }
}
