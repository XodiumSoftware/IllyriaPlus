package org.xodium.illyriacore.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import org.xodium.illyriacore.IllyriaCore.Companion.ID
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.dialogs.BendingDialog
import org.xodium.illyriacore.enums.BendingTypeEnum
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyriacore.pdcs.PlayerPDC.bendingType
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyrialib.data.CommandData

/** Represents a mechanic handling player bending within the system. */
internal object BendingMechanic : MechanicInterface {
    private val TEAM_NAME_PREFIX = "${ID}_bending_"

    private val scoreboard: Scoreboard
        get() = instance.server.scoreboardManager.mainScoreboard

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("bending")
                    .playerExecuted { player, _ -> player.showDialog(BendingDialog(player)) },
                "Opens the bending selection dialog",
            ),
        )

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) = event.player.assignBendingTeam()

    override fun onDisable() = BendingTypeEnum.entries.forEach { scoreboard.getTeam(teamName(it))?.unregister() }

    /**
     * Assigns this player to the scoreboard team matching their bending type so the
     * bending sprite renders above their head, then refreshes the tab list entry so
     * the sprite suffix is reflected there too. Removes them from any other bending
     * team. No-op if no bending type is set.
     */
    fun Player.assignBendingTeam() {
        val type = bendingType ?: return
        BendingTypeEnum.entries.forEach { teamFor(it).removeEntry(name) }
        teamFor(type).addEntry(name)
        applyBendingSuffix()
    }

    /**
     * Appends this player's bending sprite to their current tab list entry. No-op if
     * no bending type is set. Should be re-applied anytime [Player.playerListName] is
     * overwritten (e.g. by [org.xodium.illyriacore.mechanics.server.TabListMechanic]).
     */
    fun Player.applyBendingSuffix() {
        val type = bendingType ?: return
        playerListName(
            displayName().append(MM.deserialize(""" <sprite:"minecraft:items":item/${type.material.key.key}>""")),
        )
    }

    /**
     * Returns the scoreboard team for the given bending type, creating it (with its
     * sprite suffix) on first access.
     */
    private fun teamFor(type: BendingTypeEnum): Team =
        scoreboard.getTeam(teamName(type)) ?: scoreboard.registerNewTeam(teamName(type)).apply {
            suffix(MM.deserialize(""" <sprite:"minecraft:items":item/${type.material.key.key}>"""))
        }

    private fun teamName(type: BendingTypeEnum): String = TEAM_NAME_PREFIX + type.name.lowercase()
}
