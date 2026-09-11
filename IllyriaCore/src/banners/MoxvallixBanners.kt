package org.xodium.illyriaplus.banners

import org.xodium.illyriaplus.data.BannerData

/**
 * Aggregates all banner patterns from Moxvallix's "Many More Banners" pack
 * into a single IllyriaPlus collection.
 *
 * The asset paths and translation keys use the `illyriaplus` namespace so that
 * the bundled resource pack under `resourcepack/assets/illyriaplus` is loaded
 * automatically.
 */
internal object MoxvallixBanners : BannerInterface {
    /** The complete list of custom banner patterns in this collection. */
    override val banners: List<BannerData> =
        listOf(
            BannerData("anchor"),
            BannerData("blam"),
            BannerData("castle"),
            BannerData("chequered"),
            BannerData("circle_tiles"),
            BannerData("clubs"),
            BannerData("cogs"),
            BannerData("companion"),
            BannerData("crown"),
            BannerData("curtains"),
            BannerData("diamonds"),
            BannerData("double_bars"),
            BannerData("double_gradient"),
            BannerData("emoji"),
            BannerData("eye"),
            BannerData("fancy"),
            BannerData("ghast"),
            BannerData("hammer"),
            BannerData("hearts"),
            BannerData("horn"),
            BannerData("knot"),
            BannerData("moon"),
            BannerData("palace"),
            BannerData("peace"),
            BannerData("pillager"),
            BannerData("pumpkin"),
            BannerData("pyramid"),
            BannerData("revolution"),
            BannerData("ribs"),
            BannerData("shield"),
            BannerData("spades"),
            BannerData("sun"),
            BannerData("sword"),
            BannerData("tattered"),
            BannerData("tower"),
            BannerData("trident"),
            BannerData("villager"),
            BannerData("yin_yang"),
        )
}
