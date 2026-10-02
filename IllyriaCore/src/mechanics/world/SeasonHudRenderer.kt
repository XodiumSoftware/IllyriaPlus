package org.xodium.illyriacore.mechanics.world

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import org.xodium.illyriacore.enums.HudGlyphEnum
import org.xodium.illyriacore.enums.SeasonStateEnum
import org.xodium.illyrialib.Utils.MM
import java.time.Duration

/**
 * Renders a seasonal HUD indicator via title packets.
 *
 * Uses large bitmap font glyphs (registered in the resource pack) sent as the title text
 * so the client paints the glyph as a screen-space overlay. Structured as a dedicated
 * renderer so the implementation can later be swapped to a client-side HUD mod via
 * IllyriaBridge without touching callers.
 */
internal object SeasonHudRenderer {
    private const val FADE_IN_MS: Long = 0L
    private const val STAY_MS: Long = 3000L
    private const val FADE_OUT_MS: Long = 500L

    /**
     * Renders the given season's HUD glyph for the player.
     *
     * @param player The player to render for.
     * @param season The season to display.
     */
    fun render(
        player: Player,
        season: SeasonStateEnum,
    ) {
        player.showTitle(
            Title.title(
                MM.deserialize(HudGlyphEnum.of(season).toString()),
                Component.empty(),
                Title.Times.times(
                    Duration.ofMillis(FADE_IN_MS),
                    Duration.ofMillis(STAY_MS),
                    Duration.ofMillis(FADE_OUT_MS),
                ),
            ),
        )
    }
}
