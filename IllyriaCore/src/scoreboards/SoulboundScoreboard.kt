package org.xodium.illyriacore.scoreboards

import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import org.xodium.illyriacore.IllyriaCore.Companion.instance

/** Represents a scoreboard displaying active Withered Soul effects for players. */
internal object SoulboundScoreboard : ScoreboardInterface {
    override val scoreboard: Scoreboard = instance.server.scoreboardManager.newScoreboard

    @EventHandler
    fun on(event: PlayerJoinEvent) {
        event.player.scoreboard = scoreboard
    }

    /**
     * Updates the scoreboard sidebar for the given player with active soul effects.
     *
     * @param playerName The name of the player whose scoreboard is being updated.
     * @param effects The list of active effect display strings.
     */
    fun update(
        playerName: String,
        effects: List<String>,
    ) {
        val objective =
            scoreboard.getObjective("soulbound") ?: scoreboard.registerNewObjective(
                "soulbound",
                Criteria.DUMMY,
                Component.text("Soulbound"),
            )

        objective.displaySlot = DisplaySlot.SIDEBAR

        scoreboard.entries.forEach { scoreboard.resetScores(it) }

        effects
            .reversed()
            .forEachIndexed { index, effect ->
                objective.getScore(effect).score = index
            }
    }
}
