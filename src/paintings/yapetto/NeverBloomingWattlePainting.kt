package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: never blooming wattle. */
@Suppress("UnstableApiUsage")
internal object NeverBloomingWattlePainting : PaintingInterface {
    override fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, "never_blooming_wattle"))
            .width(4)
            .height(2)
            .title(MM.deserialize("never_blooming_wattle".snakeToProperCase()))
            .author(MM.deserialize(YAPETTO))
}
