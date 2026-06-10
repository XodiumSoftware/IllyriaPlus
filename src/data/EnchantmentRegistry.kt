package org.xodium.illyriaplus.data

import org.xodium.illyriaplus.enchantments.EnchantmentInterface
import org.xodium.illyriaplus.enchantments.spells.SpellEnchantmentInterface

/**
 * Holds all registered enchantments, partitioned by type.
 *
 * @property all Every enchantment registered by the plugin.
 * @property spells Only spell enchantments (wand-based Blaze Rod spells).
 */
internal data class EnchantmentRegistry(
    val all: List<EnchantmentInterface>,
    val spells: List<SpellEnchantmentInterface> = all.filterIsInstance<SpellEnchantmentInterface>(),
)
