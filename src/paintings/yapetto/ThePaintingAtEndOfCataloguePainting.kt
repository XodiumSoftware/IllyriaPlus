package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: the painting at end of catalogue. */
@Suppress("UnstableApiUsage")
internal object ThePaintingAtEndOfCataloguePainting : PaintingInterface {
    override fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, "the_painting_at_end_of_catalogue"))
            .width(4)
            .height(2)
            .title(MM.deserialize("the_painting_at_end_of_catalogue".snakeToProperCase()))
            .author(MM.deserialize(YAPETTO))
}
