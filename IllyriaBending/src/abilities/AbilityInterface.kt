package org.xodium.illyriabending.abilities

import org.bukkit.entity.Player
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

    companion object {
        /** Global cooldown storage keyed by ability instance, then player UUID. */
        private val COOLDOWNS = ConcurrentHashMap<AbilityInterface, MutableMap<UUID, Long>>()
    }
}
