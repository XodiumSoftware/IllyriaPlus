package org.xodium.illyriamannequins

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.xodium.illyrialib.Utils.MM

/** Dialog for editing a mannequin's properties. */
@Suppress("UnstableApiUsage")
internal object MannequinEditDialog {
    /**
     * Opens the edit dialog for a mannequin.
     *
     * @param player The player to show the dialog to.
     * @param mannequin The mannequin to edit.
     */
    fun show(
        player: Player,
        mannequin: Mannequin,
    ) {
        player.showDialog(
            Dialog.create {
                it
                    .empty()
                    .base(
                        DialogBase
                            .builder(MM.deserialize("<firewatch>Edit Mannequin</gradient>"))
                            .body(
                                listOf(
                                    DialogBody.plainMessage(MM.deserialize("<gray>Adjust the mannequin's properties.")),
                                ),
                            ).inputs(
                                listOf(
                                    DialogInput
                                        .bool("immovable", MM.deserialize("<gray>Immovable"))
                                        .initial(mannequin.isImmovable)
                                        .build(),
                                    DialogInput
                                        .text("description", MM.deserialize("<gray>Description"))
                                        .initial(mannequin.description?.let { MM.serialize(it) } ?: "")
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
                                        { response, _ ->
                                            mannequin.isImmovable =
                                                response.getBoolean("immovable") ?: mannequin.isImmovable
                                            mannequin.description =
                                                MM.deserialize(response.getText("description") ?: "")
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
