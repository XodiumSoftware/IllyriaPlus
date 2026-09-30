package org.xodium.illyriamannequins

import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.inventory.MainHand
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyriamannequins.IllyriaMannequins.Companion.instance
import org.xodium.illyriamannequins.MannequinPDC.following
import org.xodium.illyriamannequins.MannequinPDC.owner

/** Dialog for editing a mannequin's properties. */
@Suppress("UnstableApiUsage")
internal object MannequinEditDialog {
    private const val TITLE = "<firewatch>Edit Mannequin</gradient>"
    private const val OWNER_LABEL = "<gray>Owner: <white>"
    private const val UNKNOWN_OWNER = "Unknown"
    private const val SKIN_LABEL = "<gray>Skin (player name)"
    private const val NAME_LABEL = "<gray>Name"
    private const val DESCRIPTION_LABEL = "<gray>Description"
    private const val FOLLOW_LABEL = "<gray>Follow Players"
    private const val MAIN_HAND_LABEL = "<gray>Main Hand"
    private const val DISCARD_BUTTON = "<red>Discard</red>"
    private const val SAVE_BUTTON = "<green>Save</green>"

    /** Dialog input identifiers. */
    private enum class Input {
        SKIN,
        NAME,
        DESCRIPTION,
        FOLLOW,
        MAIN_HAND,
        ;

        /** The dialog input key derived from the enum constant name. */
        val key: String get() = name.lowercase()
    }

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
        val ownerName =
            mannequin.owner?.let { instance.server.getOfflinePlayer(it).name } ?: UNKNOWN_OWNER

        player.showDialog(
            Dialog.create { factory ->
                factory
                    .empty()
                    .base(
                        DialogBase
                            .builder(MM.deserialize(TITLE))
                            .body(
                                listOf(
                                    DialogBody.plainMessage(
                                        MM.deserialize(OWNER_LABEL + ownerName),
                                    ),
                                ),
                            ).inputs(
                                listOf(
                                    DialogInput
                                        .text(Input.SKIN.key, MM.deserialize(SKIN_LABEL))
                                        .initial(mannequin.profile.name() ?: "")
                                        .build(),
                                    DialogInput
                                        .text(Input.NAME.key, MM.deserialize(NAME_LABEL))
                                        .initial(mannequin.customName()?.let { MM.serialize(it) } ?: "")
                                        .build(),
                                    DialogInput
                                        .text(Input.DESCRIPTION.key, MM.deserialize(DESCRIPTION_LABEL))
                                        .initial(mannequin.description?.let { MM.serialize(it) } ?: "")
                                        .maxLength(4096)
                                        .build(),
                                    DialogInput
                                        .bool(Input.FOLLOW.key, MM.deserialize(FOLLOW_LABEL))
                                        .initial(mannequin.following)
                                        .build(),
                                    DialogInput
                                        .singleOption(
                                            Input.MAIN_HAND.key,
                                            MM.deserialize(MAIN_HAND_LABEL),
                                            listOf(
                                                SingleOptionDialogInput.OptionEntry.create(
                                                    MainHand.LEFT.name.lowercase(),
                                                    Component.text("Left"),
                                                    mannequin.mainHand == MainHand.LEFT,
                                                ),
                                                SingleOptionDialogInput.OptionEntry.create(
                                                    MainHand.RIGHT.name.lowercase(),
                                                    Component.text("Right"),
                                                    mannequin.mainHand == MainHand.RIGHT,
                                                ),
                                            ),
                                        ).build(),
                                ),
                            ).build(),
                    ).type(
                        DialogType.confirmation(
                            ActionButton
                                .builder(MM.deserialize(DISCARD_BUTTON))
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
                                .builder(MM.deserialize(SAVE_BUTTON))
                                .action(
                                    DialogAction.customClick(
                                        { response, _ ->
                                            response.getBoolean(Input.FOLLOW.key)?.let { mannequin.following = it }
                                            response.getText(Input.MAIN_HAND.key)?.let { hand ->
                                                mannequin.mainHand =
                                                    MainHand.entries.first { it.name.lowercase() == hand }
                                            }
                                            response.getText(Input.NAME.key)?.let { name ->
                                                if (name.isBlank()) {
                                                    mannequin.customName(null)
                                                    mannequin.isCustomNameVisible = false
                                                } else {
                                                    mannequin.customName(MM.deserialize(name))
                                                    mannequin.isCustomNameVisible = true
                                                }
                                            }
                                            response.getText(Input.DESCRIPTION.key)?.let { description ->
                                                mannequin.description =
                                                    if (description.isBlank()) {
                                                        Mannequin.defaultDescription()
                                                    } else {
                                                        MM.deserialize(description)
                                                    }
                                            }
                                            response.getText(Input.SKIN.key)?.let { skin ->
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
