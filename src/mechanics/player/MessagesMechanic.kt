@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.xodium.illyriaplus.mechanics.player

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent
import io.papermc.paper.advancement.AdvancementDisplay
import io.papermc.paper.block.bed.BedEnterProblem
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import io.papermc.paper.event.player.PlayerServerFullCheckEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.advancement.Advancement
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.interfaces.MechanicInterface

/** Represents a mechanic handling player messages within the system. */
internal object MessagesMechanic : MechanicInterface {
    /** Player join, quit, death, and kick message strings. */
    object PlayerMessages {
        const val JOIN: String = "<green>➕<reset> <mango>›</gradient> <player>"
        const val QUIT: String = "<red>➖<reset> <mango>›</gradient> <player>"
        const val DEATH_BY_PLAYER: String = "<killer> <mango>⚔</gradient> <player>"
        const val DEATH: String = "<mango>💀</gradient> <mango>›</gradient> <cause>"
        const val DEATH_SCREEN: String = "☠"
        const val KICK: String = "<red>❌<reset> <mango>›</gradient> <player> <mango>reason:</gradient> <reason>"
        const val SET_SPAWN: String = "<firewatch>❗</gradient> <mango>›</gradient> <notification>"
    }

    /** Advancement completion message strings by type (task, goal, challenge). */
    object AdvancementMessages {
        const val TASK: String =
            "🎉 <mango>›</gradient> <player> <mango>has made the advancement:</gradient> <advancement>"
        const val GOAL: String = "🎉 <mango>›</gradient> <player> <mango>has reached the goal:</gradient> <advancement>"
        const val CHALLENGE: String =
            "🎉 <mango>›</gradient> <player> <mango>has completed the challenge:</gradient> <advancement>"
    }

    /** Login denial messages (server full, access denied). */
    object LoginMessages {
        const val FULL: String = "<firewatch>❗</gradient> <mango>›</gradient> The server is full."
        const val DENIED: String =
            "<firewatch>❗</gradient> <mango>›</gradient> You are not allowed to join this server."
    }

    /** Bed enter failure messages by reason. */
    object BedEnterMessages {
        const val TOO_FAR_AWAY: String =
            "<firewatch>❗</gradient> <mango>›</gradient> You are too far away from the bed."
        const val OBSTRUCTED: String = "<firewatch>❗</gradient> <mango>›</gradient> Your bed is obstructed."
        const val NOT_SAFE: String =
            "<firewatch>❗</gradient> <mango>›</gradient> You cannot sleep while monsters are nearby."
        const val EXPLOSION: String = "<firewatch>❗</gradient> <mango>›</gradient> You cannot sleep here."
        const val OTHER: String = ""
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerJoinEvent) {
        playerJoin(event)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerQuitEvent) {
        playerQuit(event)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun on(event: PlayerServerFullCheckEvent) {
        serverFullCheck(event)
    }

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.HIGH)
    fun on(event: PlayerConnectionValidateLoginEvent) {
        loginDenied(event)
    }

    @EventHandler
    fun on(event: PlayerKickEvent) {
        playerKick(event)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun on(event: PlayerDeathEvent) {
        playerDeath(event)
    }

    @EventHandler
    fun on(event: PlayerAdvancementDoneEvent) {
        advancementDone(event)
    }

    @EventHandler
    fun on(event: PlayerSetSpawnEvent) {
        setSpawn(event)
    }

    @Suppress("UnstableApiUsage")
    @EventHandler
    fun on(event: PlayerBedEnterEvent) {
        bedEnter(event)
    }

    private fun playerJoin(event: PlayerJoinEvent) {
        event.joinMessage(handleJoin(event.player) ?: return)
    }

    private fun playerQuit(event: PlayerQuitEvent) {
        event.quitMessage(handleQuit(event.player) ?: return)
    }

    private fun serverFullCheck(event: PlayerServerFullCheckEvent) {
        if (event.isAllowed) return

        event.deny(handleServerFull() ?: return)
    }

    @Suppress("UnstableApiUsage")
    private fun loginDenied(event: PlayerConnectionValidateLoginEvent) {
        if (event.isAllowed) return

        event.kickMessage(handleLoginDenied() ?: return)
    }

    private fun playerKick(event: PlayerKickEvent) {
        event.leaveMessage(handleKick(event.reason()) ?: return)
    }

    private fun playerDeath(event: PlayerDeathEvent) {
        val killer = event.entity.killer
        val deathMessage =
            if (killer != null) {
                handleDeath(event.player, killer)
            } else {
                handleDeathNoPvp(event.player, event.deathMessage())
            }

        if (deathMessage != null) event.deathMessage(deathMessage)

        event.deathScreenMessageOverride(handleDeathScreen())
    }

    private fun advancementDone(event: PlayerAdvancementDoneEvent) {
        event.message(handleAdvancement(event.player, event.advancement) ?: return)
    }

    private fun setSpawn(event: PlayerSetSpawnEvent) {
        event.notification = handleSetSpawn(event.notification ?: return) ?: return
    }

    @Suppress("UnstableApiUsage")
    private fun bedEnter(event: PlayerBedEnterEvent) {
        event.player.sendMessage(handleBedEnter(event.enterAction().problem() ?: return) ?: return)
    }

    /**
     * Handles the player join message.
     *
     * @param player The player who joined.
     * @return The formatted join message component, or null if no message is set.
     */
    fun handleJoin(player: Player): Component? =
        MM.deserialize(
            PlayerMessages.JOIN
                .takeIf { it.isNotEmpty() } ?: return null,
            Placeholder.component("player", player.displayName()),
        )

    /**
     * Handles the player leave message.
     *
     * @param player The player who left.
     * @return The formatted leave message component, or null if no message is set.
     */
    fun handleQuit(player: Player): Component? =
        MM.deserialize(
            PlayerMessages.QUIT
                .takeIf { it.isNotEmpty() } ?: return null,
            Placeholder.component("player", player.displayName()),
        )

    /**
     * Handles the player death message.
     *
     * @param player The player who died.
     * @param killer The player who killed them.
     * @return The formatted death message component, or null if no message is set.
     */
    fun handleDeath(
        player: Player,
        killer: Player?,
    ): Component? =
        MM.deserialize(
            PlayerMessages.DEATH_BY_PLAYER
                .takeIf { it.isNotEmpty() } ?: return null,
            Placeholder.component("player", player.displayName()),
            Placeholder.component("killer", (killer ?: return null).displayName()),
        )

    /**
     * Handles the non-PvP death message (fall, fire, drowning, etc.).
     *
     * @param player The player who died.
     * @param cause The vanilla death message component.
     * @return The formatted death message component, or null if no message is set.
     */
    fun handleDeathNoPvp(
        player: Player,
        cause: Component?,
    ): Component? =
        MM.deserialize(
            PlayerMessages.DEATH
                .takeIf { it.isNotEmpty() } ?: return null,
            Placeholder.component("player", player.displayName()),
            Placeholder.component("cause", cause ?: return null),
        )

    /**
     * Handles the player death screen message.
     *
     * @return The formatted death screen message component, or null if no message is set.
     */
    fun handleDeathScreen(): Component? =
        MM.deserialize(
            PlayerMessages.DEATH_SCREEN
                .takeIf { it.isNotEmpty() } ?: return null,
        )

    /**
     * Handles the player advancement completion message, with different formats per advancement type.
     *
     * @param player The player who completed the advancement.
     * @param advancement The advancement that was completed.
     * @return The formatted advancement completion message component, or null if no message is set or the advancement has no display.
     */
    fun handleAdvancement(
        player: Player,
        advancement: Advancement,
    ): Component? {
        val display = advancement.display ?: return null

        return MM.deserialize(
            when (display.frame()) {
                AdvancementDisplay.Frame.TASK -> AdvancementMessages.TASK
                AdvancementDisplay.Frame.GOAL -> AdvancementMessages.GOAL
                AdvancementDisplay.Frame.CHALLENGE -> AdvancementMessages.CHALLENGE
            }.takeIf { it.isNotEmpty() } ?: return null,
            Placeholder.component("player", player.displayName()),
            Placeholder.component("advancement", display.title()),
        )
    }

    /**
     * Handles the kick message shown when the server is full.
     *
     * @return The formatted kick message component, or null if no message is set.
     */
    fun handleServerFull(): Component? =
        MM.deserialize(
            LoginMessages.FULL
                .takeIf { it.isNotEmpty() } ?: return null,
        )

    /**
     * Handles the kick message shown when a player is denied login (ban, IP ban, whitelist).
     *
     * @return The formatted kick message component, or null if no message is set.
     */
    fun handleLoginDenied(): Component? =
        MM.deserialize(
            LoginMessages.DENIED
                .takeIf { it.isNotEmpty() } ?: return null,
        )

    /**
     * Handles the player kick message.
     *
     * @param reason The reason for the kick.
     * @return The formatted kick message component, or null if no message is set.
     */
    fun handleKick(reason: Component): Component? =
        MM.deserialize(
            PlayerMessages.KICK
                .takeIf { it.isNotEmpty() } ?: return null,
            Placeholder.component("reason", reason),
        )

    /**
     * Handles the bed enter failure message.
     *
     * @param problem The problem preventing the player from sleeping.
     * @return The formatted bed enter message component, or null if no message is set for this problem.
     */
    @Suppress("UnstableApiUsage")
    fun handleBedEnter(problem: BedEnterProblem): Component? =
        MM.deserialize(
            when (problem) {
                BedEnterProblem.TOO_FAR_AWAY -> BedEnterMessages.TOO_FAR_AWAY
                BedEnterProblem.OBSTRUCTED -> BedEnterMessages.OBSTRUCTED
                BedEnterProblem.NOT_SAFE -> BedEnterMessages.NOT_SAFE
                BedEnterProblem.EXPLOSION -> BedEnterMessages.EXPLOSION
                else -> BedEnterMessages.OTHER
            }.takeIf { it.isNotEmpty() } ?: return null,
        )

    /**
     * Handles the player set spawn notification.
     *
     * @param notification The original notification component.
     * @return The formatted set spawn notification component.
     */
    fun handleSetSpawn(notification: Component): Component? =
        MM.deserialize(
            PlayerMessages.SET_SPAWN
                .takeIf { it.isNotEmpty() } ?: return null,
            Placeholder.component("notification", notification),
        )
}
