package org.xodium.illyriabending.data

import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard

/**
 * Scoreboard state for a single player.
 *
 * @property slot The currently selected ability slot index.
 * @property objective The sidebar objective for ability display.
 * @property scoreboard The player's personal scoreboard instance.
 */
internal data class ScoreboardData(
    var slot: Int = 0,
    val objective: Objective,
    val scoreboard: Scoreboard,
)
