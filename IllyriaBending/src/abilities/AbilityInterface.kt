package org.xodium.illyriabending.abilities

import org.bukkit.entity.Player
import org.xodium.illyriabending.enums.ActivationTypeEnum
import org.xodium.illyriabending.managers.CooldownManager
import org.xodium.illyriabending.managers.SessionManager
import java.util.UUID

/** Represents a contract for a bending ability within the system. */
internal interface AbilityInterface {
    /** The cooldown in milliseconds after this ability completes. */
    val cooldownMs: Long

    /** How the player activates this ability. */
    val activationType: ActivationTypeEnum

    /**
     * Whether this ability tracks per-player session state (e.g., AirBlade charging,
     * AirWheel riding). Override to true if the ability has sessions.
     */
    val hasSession: Boolean
        get() = false

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
     * Called every tick for active sessions. Only called if [hasSession] is true.
     *
     * @param uuid The player's UUID.
     */
    fun tickSession(uuid: UUID) {}

    /**
     * Gets the remaining cooldown time in milliseconds for a player.
     *
     * @param player The player to check.
     * @return Remaining cooldown in milliseconds, or 0 if not on cooldown.
     */
    fun getCooldownRemaining(player: Player): Long = CooldownManager.getRemaining(this, player.uniqueId)

    /**
     * Checks if a player is currently on cooldown.
     *
     * @param player The player to check.
     * @return True if the ability is on cooldown for this player.
     */
    fun isOnCooldown(player: Player): Boolean = CooldownManager.isOnCooldown(this, player.uniqueId)

    /**
     * Called when the ability completes successfully. Sets the cooldown.
     *
     * @param player The player who completed the ability.
     */
    fun complete(player: Player) = CooldownManager.set(this, player.uniqueId, cooldownMs)

    /**
     * Checks if a player has an active session for this ability.
     *
     * @param player The player to check.
     * @return True if the player has an active session.
     */
    fun hasActiveSession(player: Player): Boolean = SessionManager.has(this, player.uniqueId)
}
