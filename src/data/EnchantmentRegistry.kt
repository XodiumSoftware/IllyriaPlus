package org.xodium.illyriaplus.data

import org.xodium.illyriaplus.enchantments.EnchantmentInterface

/**
 * Holds all registered enchantments.
 *
 * @property all Every enchantment registered by the plugin.
 */
internal data class EnchantmentRegistry(
    val all: List<EnchantmentInterface>,
)
