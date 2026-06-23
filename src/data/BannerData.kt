package org.xodium.illyriaplus.data

/**
 * Holds the metadata for a single custom banner pattern.
 *
 * @property name The registry key fragment (snake_case) identifying this banner pattern.
 */
internal data class BannerData(
    val name: String,
)
