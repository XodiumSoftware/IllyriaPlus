package org.xodium.illyriabending.managers

import org.xodium.illyriabending.abilities.AbilityInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Manages per-player cooldown state for abilities. */
internal object CooldownManager {
    /** Cooldown end times keyed by ability instance, then player UUID. */
    private val cooldowns = ConcurrentHashMap<AbilityInterface, MutableMap<UUID, Long>>()

    /**
     * Gets the remaining cooldown time in milliseconds.
     *
     * @param ability The ability to check.
     * @param uuid The player's UUID.
     * @return Remaining cooldown in milliseconds, or 0 if not on cooldown.
     */
    fun getRemaining(
        ability: AbilityInterface,
        uuid: UUID,
    ): Long {
        val endTime = cooldowns[ability]?.get(uuid) ?: return 0L
        val remaining = endTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    /**
     * Checks if a player is on cooldown for an ability.
     *
     * @param ability The ability to check.
     * @param uuid The player's UUID.
     * @return True if on cooldown.
     */
    fun isOnCooldown(
        ability: AbilityInterface,
        uuid: UUID,
    ): Boolean = getRemaining(ability, uuid) > 0L

    /**
     * Sets the cooldown for a player.
     *
     * @param ability The ability.
     * @param uuid The player's UUID.
     * @param durationMs The cooldown duration in milliseconds.
     */
    fun set(
        ability: AbilityInterface,
        uuid: UUID,
        durationMs: Long,
    ) {
        cooldowns.getOrPut(ability) { ConcurrentHashMap() }[uuid] =
            System.currentTimeMillis() + durationMs
    }

    /**
     * Clears the cooldown for a player.
     *
     * @param ability The ability.
     * @param uuid The player's UUID.
     */
    fun clear(
        ability: AbilityInterface,
        uuid: UUID,
    ) {
        cooldowns[ability]?.remove(uuid)
    }
}
