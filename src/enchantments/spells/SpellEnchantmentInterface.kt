package org.xodium.illyriaplus.enchantments.spells

import org.bukkit.event.player.PlayerInteractEvent
import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/**
 * Represents a contract for spell enchantments within the system.
 * Extends [EnchantmentInterface] with cooldown, cast delay, and category metadata.
 */
internal interface SpellEnchantmentInterface : EnchantmentInterface {
    /** The cooldown of this specific spell in ticks. */
    val cooldown: Long

    /** The shared cooldown for all spells in the same category, in ticks. */
    val categoryCooldown: Long

    /** The cast delay before the spell fires, in ticks. */
    val castDelay: Long

    /** The category this spell belongs to for shared cooldowns. */
    val category: SpellCategory

    /** The unique key string for this spell, used for storage and lookup. */
    val spellKey: String get() = key.key().asString()

    /**
     * Executes the spell effect.
     *
     * @param event The interaction event triggering the cast.
     */
    fun cast(event: PlayerInteractEvent)
}
