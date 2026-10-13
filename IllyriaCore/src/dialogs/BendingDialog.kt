package org.xodium.illyriacore.dialogs

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import org.xodium.illyriacore.enums.BendingTypeEnum
import org.xodium.illyriacore.pdcs.PlayerPDC.bendingType
import org.xodium.illyrialib.Utils.MM

/** Dialog for selecting a player's bending type. */
internal object BendingDialog : DialogInterface {
    override fun invoke(player: Player): Dialog =
        Dialog.create {
            it
                .empty()
                .base(
                    DialogBase
                        .builder(MM.deserialize("<firewatch>Choose Your Bending</gradient>"))
                        .body(
                            listOf(
                                DialogBody.plainMessage(
                                    MM.deserialize(
                                        "<yellow>Choose the <red>bending type</red> you wish to master. " +
                                            "This choice is <red>permanent</red> — choose wisely.",
                                    ),
                                ),
                            ),
                        ).inputs(
                            listOf(
                                DialogInput
                                    .singleOption(
                                        "bending_type",
                                        200,
                                        BendingTypeEnum.entries.map { type ->
                                            val label = type.name.lowercase().replaceFirstChar(Char::uppercase)
                                            SingleOptionDialogInput.OptionEntry.create(
                                                type.name,
                                                MM.deserialize("<${type.color}>$label</${type.color}>"),
                                                player.bendingType == type,
                                            )
                                        },
                                        MM.deserialize("<gray>Bending type</gray>"),
                                        true,
                                    ),
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
                                        response
                                            .getText("bending_type")
                                            ?.let(BendingTypeEnum::valueOf)
                                            ?.takeIf { type -> type != player.bendingType }
                                            ?.let { type -> player.bendingType = type }
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
        }
}
