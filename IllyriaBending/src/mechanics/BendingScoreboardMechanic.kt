package org.xodium.illyriabending.mechanics

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import org.xodium.illyriabending.data.AbilityEntryData
import org.xodium.illyriabending.data.AbilityLineData
import org.xodium.illyriabending.data.ScoreboardData
import org.xodium.illyriabending.enums.BendingTypeEnum
import org.xodium.illyriabending.pdcs.PlayerPDC.bendingLevel
import org.xodium.illyriabending.pdcs.PlayerPDC.bendingType
import org.xodium.illyrialib.Utils.MM
import java.util.UUID

/**
 * Manages per-player ability selection scoreboards with scrolling window support.
 * Shows unlocked abilities in the sidebar with the current selection highlighted.
 */
internal object BendingScoreboardMechanic : MechanicInterface {
    private const val OBJECTIVE_NAME = "bending_abilities"
    private const val MAX_SIDEBAR_LINES = 15
    private const val SCROLL_INDICATOR_LINES = 2

    private const val TITLE_TEXT = "<gold>Bending"
    private const val NO_ABILITIES_TEXT = "<red>  No abilities"
    private const val MORE_ABOVE_TEXT = "<dark_gray>▲ <count> more"
    private const val MORE_BELOW_TEXT = "<dark_gray>▼ <count> more"

    /** Per-player scoreboard state. */
    private val playerData = mutableMapOf<UUID, ScoreboardData>()

    @EventHandler
    fun on(event: PlayerJoinEvent) = setupScoreboard(event.player)

    @EventHandler
    fun on(event: PlayerQuitEvent) = cleanupScoreboard(event.player)

    @EventHandler
    fun on(event: PlayerItemHeldEvent) = handleSlotChange(event)

    override fun onDisable() {
        playerData.keys.toList().forEach { uuid ->
            instance.server.getPlayer(uuid)?.scoreboard = instance.server.scoreboardManager.mainScoreboard
        }
        playerData.clear()
    }

    /**
     * Gets the currently selected ability name for a player.
     *
     * @param player The player to query.
     * @return The selected ability name, or null if none selected.
     */
    fun getSelectedAbility(player: Player): String? {
        val data = playerData[player.uniqueId] ?: return null
        val abilities = getAbilitiesForPlayer(player)
        return abilities.getOrNull(data.slot)?.name
    }

    /**
     * Refreshes the sidebar display with current ability window.
     *
     * @param player The player whose scoreboard to refresh.
     */
    fun refreshScoreboard(player: Player) {
        val data = playerData[player.uniqueId] ?: return
        val abilities = getAbilitiesForPlayer(player)

        data.objective.unregister()
        val objective =
            data.scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                MM.deserialize(TITLE_TEXT),
            )
        objective.displaySlot = DisplaySlot.SIDEBAR

        buildAbilityWindow(abilities, data.slot).forEachIndexed { index, line ->
            objective.getScore(line.toPlainText()).score = line.scoreValue(index, abilities.size)
        }
    }

    /**
     * Builds the visible window of abilities with scroll indicators.
     *
     * @param abilities All available abilities.
     * @param currentSlot The currently selected global slot index.
     * @return List of formatted lines for the sidebar.
     */
    private fun buildAbilityWindow(
        abilities: List<AbilityEntryData>,
        currentSlot: Int,
    ): List<AbilityLineData> {
        val total = abilities.size
        if (total == 0) {
            return listOf(AbilityLineData(NO_ABILITIES_TEXT, 0))
        }

        val windowSize = MAX_SIDEBAR_LINES - SCROLL_INDICATOR_LINES
        val start = maxOf(0, currentSlot - windowSize / 2)
        val end = minOf(total, start + windowSize)
        val adjustedStart = maxOf(0, end - windowSize)

        return buildList {
            if (adjustedStart > 0) {
                add(AbilityLineData(MORE_ABOVE_TEXT.replace("<count>", adjustedStart.toString()), total))
            }

            for (i in adjustedStart until end) {
                val ability = abilities[i]
                val isSelected = i == currentSlot
                val prefix = if (isSelected) "<green>> " else "  "
                val color =
                    when {
                        isSelected -> ""
                        ability.isPassive -> "<aqua>"
                        else -> "<gray>"
                    }
                val text = "$color$prefix${ability.name}</>"
                add(AbilityLineData(text, end - i))
            }

            if (end < total) {
                add(AbilityLineData(MORE_BELOW_TEXT.replace("<count>", (total - end).toString()), 0))
            }
        }
    }

    /**
     * Gets all available abilities for a player based on their bending type and level.
     *
     * @param player The player to get abilities for.
     * @return List of [AbilityEntryData] objects representing unlocked abilities.
     */
    private fun getAbilitiesForPlayer(player: Player): List<AbilityEntryData> {
        val type = player.bendingType ?: return emptyList()
        val level = player.bendingLevel

        return when (type) {
            BendingTypeEnum.AIR -> getAirAbilities(level)
            BendingTypeEnum.WATER -> getWaterAbilities(level)
            BendingTypeEnum.EARTH -> getEarthAbilities(level)
            BendingTypeEnum.FIRE -> getFireAbilities(level)
        }
    }

    /**
     * Returns air abilities unlocked at the given bending level.
     *
     * @param level The player's bending level, in `1..5`.
     * @return List of unlocked air abilities.
     */
    private fun getAirAbilities(level: Int): List<AbilityEntryData> =
        buildList {
            if (level >= 1) {
                add(AbilityEntryData("AirBlade"))
                add(AbilityEntryData("AirBlast"))
                add(AbilityEntryData("AirBreath"))
                add(AbilityEntryData("AirBurst"))
                add(AbilityEntryData("AirPunch"))
                add(AbilityEntryData("AirScooter"))
                add(AbilityEntryData("AirShield"))
                add(AbilityEntryData("AirSpout"))
                add(AbilityEntryData("AirSwipe"))
                add(AbilityEntryData("Tornado"))
                add(AbilityEntryData("AirAgility", isPassive = true))
                add(AbilityEntryData("GracefulDescent", isPassive = true))
                add(AbilityEntryData("AirWheel", isPassive = true))
            }
        }

    /**
     * Returns water abilities unlocked at the given bending level.
     *
     * @param level The player's bending level, in `1..5`.
     * @return List of unlocked water abilities.
     */
    private fun getWaterAbilities(level: Int): List<AbilityEntryData> =
        buildList {
            if (level >= 1) {
                add(AbilityEntryData("BloodBending"))
                add(AbilityEntryData("FrostBreath"))
                add(AbilityEntryData("HealingWaters"))
                add(AbilityEntryData("IceCrawl"))
                add(AbilityEntryData("IceSpike"))
                add(AbilityEntryData("IceWall"))
                add(AbilityEntryData("OctopusForm"))
                add(AbilityEntryData("PhaseChange"))
                add(AbilityEntryData("Torrent"))
                add(AbilityEntryData("WaterBubble"))
                add(AbilityEntryData("WaterManipulation"))
                add(AbilityEntryData("WaterRing"))
                add(AbilityEntryData("WaterSpout"))
                add(AbilityEntryData("WaterWave"))
                add(AbilityEntryData("FastSwim", isPassive = true))
                add(AbilityEntryData("HydroSink", isPassive = true))
                add(AbilityEntryData("Iceberg"))
                add(AbilityEntryData("WaterGimbal"))
            }
        }

    /**
     * Returns earth abilities unlocked at the given bending level.
     *
     * @param level The player's bending level, in `1..5`.
     * @return List of unlocked earth abilities.
     */
    private fun getEarthAbilities(level: Int): List<AbilityEntryData> =
        buildList {
            if (level >= 1) {
                add(AbilityEntryData("Bulwark"))
                add(AbilityEntryData("Catapult"))
                add(AbilityEntryData("Collapse"))
                add(AbilityEntryData("EarthArmor"))
                add(AbilityEntryData("EarthBlast"))
                add(AbilityEntryData("EarthGlove"))
                add(AbilityEntryData("EarthLine"))
                add(AbilityEntryData("EarthShot"))
                add(AbilityEntryData("EarthSmash"))
                add(AbilityEntryData("EarthSurf"))
                add(AbilityEntryData("EarthTunnel"))
                add(AbilityEntryData("LavaDisk"))
                add(AbilityEntryData("LavaFlux"))
                add(AbilityEntryData("MetalCable"))
                add(AbilityEntryData("RaiseEarth"))
                add(AbilityEntryData("Shockwave"))
                add(AbilityEntryData("DensityShift", isPassive = true))
                add(AbilityEntryData("EarthCling", isPassive = true))
                add(AbilityEntryData("FerroControl", isPassive = true))
                add(AbilityEntryData("Locksmithing", isPassive = true))
                add(AbilityEntryData("EarthPillars"))
                add(AbilityEntryData("EarthShards"))
            }
        }

    /**
     * Returns fire abilities unlocked at the given bending level.
     *
     * @param level The player's bending level, in `1..5`.
     * @return List of unlocked fire abilities.
     */
    private fun getFireAbilities(level: Int): List<AbilityEntryData> =
        buildList {
            if (level >= 1) {
                add(AbilityEntryData("Blaze"))
                add(AbilityEntryData("Combustion"))
                add(AbilityEntryData("FireBlast"))
                add(AbilityEntryData("FireBreath"))
                add(AbilityEntryData("FireBurst"))
                add(AbilityEntryData("FireJet"))
                add(AbilityEntryData("FirePropagate"))
                add(AbilityEntryData("FireShield"))
                add(AbilityEntryData("FireWall"))
                add(AbilityEntryData("FlameRush"))
                add(AbilityEntryData("HeatControl"))
                add(AbilityEntryData("Lightning"))
                add(AbilityEntryData("FireKick"))
                add(AbilityEntryData("FireSpin"))
                add(AbilityEntryData("FireWheel"))
            }
        }

    /**
     * Sets up a new scoreboard for the joining player.
     *
     * @param player The joining player.
     */
    private fun setupScoreboard(player: Player) {
        val scoreboard: Scoreboard = instance.server.scoreboardManager.newScoreboard
        val objective: Objective =
            scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                MM.deserialize(TITLE_TEXT),
            )
        objective.displaySlot = DisplaySlot.SIDEBAR

        player.scoreboard = scoreboard
        playerData[player.uniqueId] = ScoreboardData(objective = objective, scoreboard = scoreboard)
        refreshScoreboard(player)
    }

    /**
     * Cleans up the scoreboard when a player leaves.
     *
     * @param player The leaving player.
     */
    private fun cleanupScoreboard(player: Player) {
        playerData.remove(player.uniqueId)
    }

    /**
     * Handles hotbar slot changes to update ability selection.
     *
     * @param event The [PlayerItemHeldEvent] triggered when a player changes hotbar slots.
     */
    private fun handleSlotChange(event: PlayerItemHeldEvent) {
        val player = event.player
        val data = playerData[player.uniqueId] ?: return
        val abilities = getAbilitiesForPlayer(player)

        if (abilities.isEmpty()) return

        data.slot = event.newSlot.coerceIn(0, abilities.size - 1)
        refreshScoreboard(player)
    }
}
