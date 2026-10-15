package org.xodium.illyriabending.abilities

import org.bukkit.entity.Player

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
}
