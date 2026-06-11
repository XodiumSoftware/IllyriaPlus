package org.xodium.illyriaplus.dialogs

import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.mechanics.player.NicknameMechanic.nickname

/** Represents the nickname dialog implementation within the system. */
@Suppress("UnstableApiUsage")
internal object NicknameDialog : DialogInterface {
    override fun invoke(builder: DialogRegistryEntry.Builder): DialogRegistryEntry.Builder =
        builder
            .base(
                DialogBase
                    .builder(MM.deserialize("<firewatch><b>Nickname</b></gradient>"))
                    .externalTitle(MM.deserialize("Nickname"))
                    .inputs(
                        listOf(
                            DialogInput
                                .text("nickname", MM.deserialize("<gray>Enter nickname</gray>"))
                                .width(200)
                                .maxLength(4096)
//                                .initial(MM.serialize(player.displayName())) //TODO
                                .labelVisible(true)
                                .build(),
                        ),
                    ).canCloseWithEscape(true)
                    .build(),
            ).type(
                DialogType.confirmation(
                    ActionButton.builder(MM.deserialize("<red>Discard</red>")).build(),
                    ActionButton
                        .builder(MM.deserialize("<green>Save</green>"))
                        .action(
                            DialogAction.customClick(
                                { response, audience ->
                                    val player = audience as? Player ?: return@customClick

                                    player.nickname(response.getText("nickname") ?: "")
                                },
                                ClickCallback.Options
                                    .builder()
                                    .uses(ClickCallback.UNLIMITED_USES)
                                    .build(),
                            ),
                        ).build(),
                ),
            )
}
