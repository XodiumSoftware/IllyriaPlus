package org.xodium.illyriamannequins

import io.papermc.paper.datacomponent.item.ResolvableProfile
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
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.owner

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
                                    DialogBody.plainMessage(
                                        MM.deserialize(
                                            "<gray>Owner: <white>${
                                                mannequin.owner?.let {
                                                    instance
                                                        .server
                                                        .getOfflinePlayer(
                                                            it,
                                                        ).name
                                                } ?: "Unknown"
                                            }",
                                        ),
                                    ),
                                ),
                            ).inputs(
                                listOf(
                                    DialogInput
                                        .text("skin", MM.deserialize("<gray>Skin (player name)"))
                                        .initial(mannequin.profile?.name() ?: "")
                                        .build(),
                                    DialogInput
                                        .text("name", MM.deserialize("<gray>Name"))
                                        .initial(mannequin.customName()?.let { MM.serialize(it) } ?: "")
                                        .build(),
                                    DialogInput
                                        .text("description", MM.deserialize("<gray>Description"))
                                        .initial(mannequin.description?.let { MM.serialize(it) } ?: "")
                                        .maxLength(4096)
                                        .build(),
                                    DialogInput
                                        .bool("immovable", MM.deserialize("<gray>Immovable"))
                                        .initial(mannequin.isImmovable)
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
                                            response.getText("name")?.let { name ->
                                                if (name.isBlank()) {
                                                    mannequin.customName(null)
                                                    mannequin.isCustomNameVisible = false
                                                } else {
                                                    mannequin.customName(MM.deserialize(name))
                                                    mannequin.isCustomNameVisible = true
                                                }
                                            }
                                            response.getText("description")?.let { description ->
                                                mannequin.description =
                                                    if (description.isBlank()) {
                                                        Mannequin.defaultDescription()
                                                    } else {
                                                        MM.deserialize(description)
                                                    }
                                            }
                                            response.getText("skin")?.let { skin ->
                                                val profile = ResolvableProfile.resolvableProfile()
                                                if (skin.isNotBlank()) {
                                                    profile.name(skin)
                                                }
                                                mannequin.profile = profile.build()
                                            }
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
