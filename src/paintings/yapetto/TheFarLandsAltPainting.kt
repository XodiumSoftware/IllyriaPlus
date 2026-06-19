package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: the far lands alt. */
@Suppress("UnstableApiUsage")
internal object TheFarLandsAltPainting : PaintingInterface {
    override fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, "the_far_lands_alt"))
            .width(4)
            .height(3)
            .title(MM.deserialize("the_far_lands_alt".snakeToProperCase()))
            .author(MM.deserialize(YAPETTO))
}
