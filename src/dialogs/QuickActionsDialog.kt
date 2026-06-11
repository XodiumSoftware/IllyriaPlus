package org.xodium.illyriaplus.dialogs

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import io.papermc.paper.registry.data.dialog.type.DialogType
import io.papermc.paper.registry.keys.DialogKeys
import io.papermc.paper.registry.set.RegistrySet
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

    override fun invoke(builder: DialogRegistryEntry.Builder): DialogRegistryEntry.Builder =
        builder
            .base(
                DialogBase
                    .builder(MM.deserialize("<firewatch><b>Quick Actions [<key:key.quickActions>]</b></gradient>"))
                    .canCloseWithEscape(true)
                    .build(),
            ).type(
                DialogType
                    .dialogList(
                        RegistrySet.keySet(
                            RegistryKey.DIALOG,
                            listOf(
                                DialogKeys.SERVER_LINKS, // vanilla Server Links dialog
                                // SettingsDialog.key,  // example custom dialog (must be registered)
                                // WarpDialog.key,      // example custom dialog (must be registered)
                            ),
                        ),
                    ).columns(1)
                    .exitAction(
                        ActionButton.builder(MM.deserialize("<red>Close [Esc]</red>")).build(),
                    ).build(),
            )
}
