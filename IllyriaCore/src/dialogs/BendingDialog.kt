package org.xodium.illyriacore.dialogs

import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.key.Key
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyriacore.enums.BendingTypeEnum
import org.xodium.illyrialib.Utils.MM

/** Key definitions and builder for the blocking bending-type selection dialog. */
internal object BendingDialog {
    /** Registry key of the dialog. */
    val DIALOG_KEY: Key = Key.key(IllyriaCore.ID, "bending_selection")

    /** Click identifier prefix; individual keys are derived per [BendingTypeEnum]. */
    private const val CLICK_PREFIX = "bending/select/"

    /** Click identifier [Key] for the given [type]. */
    fun clickKey(type: BendingTypeEnum): Key = Key.key(IllyriaCore.ID, CLICK_PREFIX + type.name.lowercase())

    /** Builds the [ActionButton] for the given [type]. */
    private fun button(type: BendingTypeEnum): ActionButton {
        val label = type.name.lowercase().replaceFirstChar { it.uppercase() }
        return ActionButton
            .builder(MM.deserialize("<${type.color}>$label</${type.color}>"))
            .tooltip(
                MM.deserialize(
                    "<gray>Click to become a <${type.color}>${label.lowercase()}</${type.color}> bender</gray>",
                ),
            ).action(DialogAction.customClick(clickKey(type), null))
            .build()
    }

    /** Dialog base with title, escape lock, and body. */
    val base: DialogBase =
        DialogBase
            .builder(MM.deserialize("<firewatch>Choose Your Bending</gradient>"))
            .canCloseWithEscape(false)
            .body(
                listOf(
                    DialogBody.plainMessage(
                        MM.deserialize(
                            "<yellow>Before you can enter the world, you must choose the <red>bending type</red> " +
                                "you wish to master. This choice is <red>permanent</red> — choose wisely.",
                        ),
                    ),
                ),
            ).build()

    /** Dialog type containing one button per [BendingTypeEnum] entry. */
    val type: DialogType =
        DialogType.multiAction(
            BendingTypeEnum.entries.map { button(it) },
            null,
            2,
        )
}
