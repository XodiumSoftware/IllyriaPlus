package org.xodium.illyriaplus.dialogs

import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import io.papermc.paper.registry.data.dialog.type.DialogType
import org.xodium.illyriaplus.Utils.MM

/** Represents an object handling the quick actions dialog implementation within the system. */
@Suppress("UnstableApiUsage")
internal object QuickActionsDialog : DialogInterface {
    override fun invoke(builder: DialogRegistryEntry.Builder): DialogRegistryEntry.Builder =
        builder
            .base(
                DialogBase
                    .builder(MM.deserialize("<firewatch><b>Quick Actions</b></gradient>"))
                    .canCloseWithEscape(true)
                    .build(),
            ).type(DialogType.notice())
}
