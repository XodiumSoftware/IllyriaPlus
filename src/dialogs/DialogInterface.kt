package org.xodium.illyriaplus.dialogs

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils.toRegistryKeyFragment

/** Represents a contract for dialogs within the system. */
@Suppress("UnstableApiUsage")
internal interface DialogInterface {
    /**
     * The unique typed key identifies this dialog in the registry.
     *
     * @see io.papermc.paper.registry.TypedKey
     * @see io.papermc.paper.registry.RegistryKey.DIALOG
     */
    val key: TypedKey<Dialog>
        get() =
            TypedKey.create(
                RegistryKey.DIALOG,
                Key.key(IllyriaPlus.ID, javaClass.toRegistryKeyFragment<Dialog>()),
            )

    /**
     * Configures the properties of the dialog using the provided builder.
     *
     * @param builder The builder used to define the dialog properties.
     * @return The builder for method chaining.
     */
    fun invoke(builder: DialogRegistryEntry.Builder): DialogRegistryEntry.Builder = builder

    /**
     * Retrieves the dialog from the registry.
     *
     * @return The [Dialog] instance corresponding to the key.
     * @throws NoSuchElementException if the dialog is not found in the registry.
     */
    fun get(): Dialog = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).getOrThrow(key)
}
