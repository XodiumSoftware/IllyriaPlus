package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: morning on the seine. */
@Suppress("UnstableApiUsage")
internal object MorningOnTheSeinePainting : PaintingInterface {
    override fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, "morning_on_the_seine"))
            .width(2)
            .height(3)
            .title(MM.deserialize("morning_on_the_seine".snakeToProperCase()))
            .author(MM.deserialize(YAPETTO))
}
