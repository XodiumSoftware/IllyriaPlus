package org.xodium.illyriaplus.data

/**
 * Holds the metadata for a single custom banner pattern.
 *
 * @property name The registry key fragment (snake_case) identifying this banner pattern.
 * @property translationKey The lang key used for the pattern's tooltip in banners and looms.
 */
internal data class BannerData(
    val name: String,
    val translationKey: String = "block.minecraft.banner.illyriaplus.$name",
)
