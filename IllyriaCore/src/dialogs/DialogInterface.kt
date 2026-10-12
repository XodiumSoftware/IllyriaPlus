package org.xodium.illyriacore.dialogs

import io.papermc.paper.dialog.Dialog
import org.bukkit.entity.Player

/** Represents a contract for dialogs within the system. */
internal interface DialogInterface {
    /**
     * Builds the dialog for the given player.
     *
     * @param player The player viewing the dialog.
     * @return The constructed [Dialog].
     */
    operator fun invoke(player: Player): Dialog
}
