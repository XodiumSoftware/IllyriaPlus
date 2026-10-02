package org.xodium.illyriacore.dialogs

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import org.xodium.illyriacore.data.SeasonState
import org.xodium.illyrialib.Utils.MM

/** Dialog showing the current season with a button to advance to the next one. */
internal object SeasonDialog : DialogInterface {
    override fun invoke(player: Player): Dialog {
        val world = player.world
        val current = SeasonState.currentSeason(world)
        val next = current.next()

        return Dialog.create {
            it
                .empty()
                .base(
                    DialogBase
                        .builder(MM.deserialize("<firewatch>Seasons</gradient>"))
                        .body(
                            listOf(
                                DialogBody.plainMessage(
                                    MM.deserialize(
                                        "<gray>Current season: <${current.color}>${
                                            current.name
                                                .lowercase()
                                                .replaceFirstChar(Char::uppercase)
                                        }</${current.color}>",
                                    ),
                                ),
                            ),
                        ).build(),
                ).type(
                    DialogType.notice(
                        ActionButton
                            .builder(
                                MM.deserialize(
                                    "<${next.color}>Advance to ${
                                        next.name
                                            .lowercase()
                                            .replaceFirstChar(Char::uppercase)
                                    }</${next.color}>",
                                ),
                            ).action(
                                DialogAction.customClick(
                                    { _, _ ->
                                        SeasonState.advanceSeason(world)
                                        player.sendMessage(
                                            MM.deserialize(
                                                "<gray>Skipped to <${next.color}>${
                                                    next.name
                                                        .lowercase()
                                                        .replaceFirstChar(Char::uppercase)
                                                }</${next.color}>.",
                                            ),
                                        )
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

    /** MiniMessage color used to represent this season in dialog text. */
    private val SeasonState.color: String
        get() =
            when (this) {
                SeasonState.SPRING -> "green"
                SeasonState.SUMMER -> "yellow"
                SeasonState.AUTUMN -> "red"
                SeasonState.WINTER -> "aqua"
            }
}
