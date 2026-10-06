package org.xodium.illyriacore.dialogs

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import org.xodium.illyriacore.enums.GlyphEnum
import org.xodium.illyriacore.enums.SeasonStateEnum
import org.xodium.illyriacore.mechanics.world.SeasonMechanic
import org.xodium.illyrialib.Utils.MM

/** Dialog showing the current season with a dropdown and save/discard buttons. */
internal object SeasonDialog : DialogInterface {
    override fun invoke(player: Player): Dialog {
        val world = player.world
        val current = SeasonStateEnum.currentSeason(world)

        return Dialog.create {
            it
                .empty()
                .base(
                    DialogBase
                        .builder(MM.deserialize("<firewatch>Seasons</gradient>"))
                        .inputs(
                            listOf(
                                DialogInput
                                    .singleOption(
                                        "season",
                                        MM.deserialize("<gray>Select season</gray>"),
                                        SeasonStateEnum.entries.map { season ->
                                            OptionEntry.create(
                                                season.name.lowercase(),
                                                MM.deserialize(
                                                    "<${season.color}>${
                                                        season.name
                                                            .lowercase()
                                                            .replaceFirstChar(Char::uppercase)
                                                    }</${season.color}> ${GlyphEnum.of(season)}",
                                                ),
                                                season == current,
                                            )
                                        },
                                    ).width(200)
                                    .labelVisible(true)
                                    .build(),
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
                                        val selected =
                                            response
                                                .getText("season")
                                                ?.let { id ->
                                                    SeasonStateEnum.entries.firstOrNull { entry ->
                                                        entry.name.lowercase() ==
                                                            id
                                                    }
                                                } ?: return@customClick
                                        if (selected == current) return@customClick
                                        SeasonStateEnum.setSeason(world, selected)
                                        SeasonMechanic.swapBiome(world)
                                        player.sendActionBar(
                                            MM.deserialize(
                                                "<gray>Season set to <${selected.color}>${
                                                    selected.name
                                                        .lowercase()
                                                        .replaceFirstChar(Char::uppercase)
                                                }</${selected.color}> ${GlyphEnum.of(selected)}",
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
}
