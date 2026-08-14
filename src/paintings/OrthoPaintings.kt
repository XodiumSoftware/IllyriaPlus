package org.xodium.illyriaplus.paintings

import org.xodium.illyriaplus.data.PaintingData

/** Aggregates all Orthodox icon painting variants into a single collection. */
internal object OrthoPaintings : PaintingInterface {
    /** Shared namespace/author key for Orthodox icon painting assets. */
    private const val ORTHO = "illyrius"

    /** The complete list of Orthodox icon painting variants and their block sizes. */
    override val paintings: List<PaintingData> =
        listOf(
            PaintingData("archangel_michael", Pair(1, 2), ORTHO),
            PaintingData("archangel_michael_2", Pair(1, 2), ORTHO),
            PaintingData("ascension_of_christ", Pair(3, 2), ORTHO),
            PaintingData("baptism_of_christ", Pair(3, 2), ORTHO),
            PaintingData("christ_pantocrator", Pair(1, 2), ORTHO),
            PaintingData("crucifixion_of_christ", Pair(2, 3), ORTHO),
            PaintingData("mystical_supper", Pair(3, 2), ORTHO),
            PaintingData("nativity_of_christ", Pair(3, 2), ORTHO),
            PaintingData("saint_andrew", Pair(1, 2), ORTHO),
            PaintingData("saint_anthony", Pair(1, 2), ORTHO),
            PaintingData("saint_bartholomew", Pair(1, 2), ORTHO),
            PaintingData("saint_gabriel", Pair(1, 2), ORTHO),
            PaintingData("saint_james", Pair(1, 2), ORTHO),
            PaintingData("saint_john", Pair(1, 2), ORTHO),
            PaintingData("saint_matthew", Pair(1, 2), ORTHO),
            PaintingData("saint_matthias", Pair(1, 2), ORTHO),
            PaintingData("saint_paul", Pair(1, 2), ORTHO),
            PaintingData("saint_peter", Pair(1, 2), ORTHO),
            PaintingData("saint_philip", Pair(1, 2), ORTHO),
            PaintingData("saint_simon", Pair(1, 2), ORTHO),
            PaintingData("saint_thomas", Pair(1, 2), ORTHO),
            PaintingData("theotokos_joy_of_all", Pair(1, 2), ORTHO),
        )
}
