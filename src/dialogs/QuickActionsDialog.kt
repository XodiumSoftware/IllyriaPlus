package org.xodium.illyriaplus.dialogs

import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.type.DialogType
import io.papermc.paper.registry.keys.DialogKeys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import org.xodium.illyriaplus.Utils.MM

/**
 * Represents the quick actions dialog implementation within the system.
 *
 * Overrides the vanilla [DialogKeys.QUICK_ACTIONS] registry entry so the
 * client’s Quick Actions key (default **G**) opens this dialog.
 */
@Suppress("UnstableApiUsage")
internal object QuickActionsDialog : DialogInterface {
    override val key = DialogKeys.QUICK_ACTIONS

    /**
     * List of dialog entries displayed in this dialog.
     *
     * Each pair is `(label, action)` where:
     * - `label` is the Adventure [Component] shown on the button
     * - `action` is the [DialogAction] executed when the button is clicked
     *
     * Add new entries here to expand the quick actions menu.
     */
    private val DIALOG_LIST: List<Pair<Component, DialogAction>> =
        listOf(
            // Example: open a message dialog
            MM.deserialize("<green>Show Info</green>") to
                DialogAction.customClick(
                    { _, audience ->
                        if (audience is Player) {
                            audience.sendMessage(MM.deserialize("<yellow>Hello from Quick Actions!</yellow>"))
                        }
                    },
                    ClickCallback.Options
                        .builder()
                        .uses(ClickCallback.UNLIMITED_USES)
                        .build(),
                ),
        )

    override fun invoke(builder: DialogRegistryEntry.Builder): DialogRegistryEntry.Builder =
        builder
            .base(
                DialogBase
                    .builder(MM.deserialize("<firewatch><b>Quick Actions</b></gradient>"))
                    .canCloseWithEscape(true)
                    .build(),
            ).type(
                DialogType
                    .multiAction(
                        DIALOG_LIST.map { (label, action) ->
                            ActionButton.builder(label).action(action).build()
                        },
                    ).columns(1)
                    .exitAction(
                        ActionButton.builder(MM.deserialize("<red>Close</red>")).build(),
                    ).build(),
            )
}
