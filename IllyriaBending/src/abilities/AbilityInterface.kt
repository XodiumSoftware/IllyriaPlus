package org.xodium.illyriabending.abilities

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriabending.IllyriaBending.Companion.instance
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Represents a contract for a bending ability within the system. */
internal interface AbilityInterface {
    /** The cooldown in milliseconds after this ability completes. */
    val cooldownMs: Long

    /**
     * Called when the player activates the ability (sneak-hold with focus item).
     *
     * @param player The casting player.
     * @param level The player's bending level, in `1..5`.
     */
    fun activate(
        player: Player,
        level: Int,
    )

    /**
     * Gets the remaining cooldown time in milliseconds for a player.
     *
     * @param player The player to check.
     * @return Remaining cooldown in milliseconds, or 0 if not on cooldown.
     */
    fun getCooldownRemaining(player: Player): Long {
        val endTime = COOLDOWNS[this]?.get(player.uniqueId) ?: return 0L
        val remaining = endTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Checks if a player is currently on cooldown.
     *
     * @param player The player to check.
     * @return True if the ability is on cooldown for this player.
     */
    fun isOnCooldown(player: Player): Boolean = getCooldownRemaining(player) > 0L

    /**
     * Sets the cooldown for a player.
     *
     * @param player The player to set cooldown for.
     */
    fun setCooldown(player: Player) {
        COOLDOWNS.getOrPut(this) { ConcurrentHashMap() }[player.uniqueId] =
            System.currentTimeMillis() + cooldownMs
    }

    /**
     * Called when the ability completes successfully. Sets the cooldown.
     *
     * @param player The player who completed the ability.
     */
    fun complete(player: Player) = setCooldown(player)

    // ============ Session Management ============

    /**
     * Whether this ability tracks per-player session state (e.g., AirBlade charging,
     * AirWheel riding). Override to true if the ability has sessions.
     */
    val hasSession: Boolean
        get() = false

    /**
     * Starts a new session for the player. Only called if [hasSession] is true.
     *
     * @param player The player starting the session.
     * @param level The player's bending level.
     */
    fun startSession(
        player: Player,
        level: Int,
    ) {}

    /**
     * Cancels and cleans up a player's session.
     *
     * @param player The player whose session to cancel.
     */
    fun cancelSession(player: Player) {}

    /**
     * Checks if a player has an active session for this ability.
     *
     * @param player The player to check.
     * @return True if the player has an active session.
     */
    fun hasActiveSession(player: Player): Boolean = false

    /**
     * Called every tick for active sessions.
     *
     * @param uuid The player's UUID.
     */
    fun tickSession(uuid: UUID) {}

    companion object {
        /** Global cooldown storage keyed by ability instance, then player UUID. */
        private val COOLDOWNS = ConcurrentHashMap<AbilityInterface, MutableMap<UUID, Long>>()

        /** Active sessions keyed by ability instance, then player UUID. */
        private val SESSIONS = ConcurrentHashMap<AbilityInterface, MutableMap<UUID, Session>>()

        /**
         * Starts a repeating task for an ability session.
         *
         * @param ability The ability to tick.
         * @param uuid The player's UUID.
         * @param periodTicks Ticks between each tick call.
         * @return The created BukkitTask, or null if scheduling failed.
         */
        fun startSessionTask(
            ability: AbilityInterface,
            uuid: UUID,
            periodTicks: Long = 1L,
        ): BukkitTask =
            instance.server.scheduler.runTaskTimer(
                instance,
                Runnable { ability.tickSession(uuid) },
                0L,
                periodTicks,
            )

        /**
         * Gets the session map for an ability.
         *
         * @param ability The ability to get sessions for.
         * @return The session map, or null if none exists.
         */
        fun getSessions(ability: AbilityInterface): MutableMap<UUID, Session>? = SESSIONS[ability]

        /**
         * Gets or creates the session map for an ability.
         *
         * @param ability The ability to get sessions for.
         * @return The session map.
         */
        fun getOrCreateSessions(ability: AbilityInterface): MutableMap<UUID, Session> =
            SESSIONS.getOrPut(ability) { ConcurrentHashMap() }

        /**
         * Removes a player's session for an ability.
         *
         * @param ability The ability.
         * @param uuid The player's UUID.
         */
        fun removeSession(
            ability: AbilityInterface,
            uuid: UUID,
        ) {
            SESSIONS[ability]?.remove(uuid)?.task?.cancel()
        }
    }
}

/**
 * Represents an active ability session with task management.
 */
internal class Session(
    /** The player this session belongs to. */
    val player: Player,
    /** The scheduler task for this session. */
    var task: BukkitTask? = null,
) {
    /** Cancels the session task if active. */
    fun cancel() {
        task?.cancel()
    }
}
