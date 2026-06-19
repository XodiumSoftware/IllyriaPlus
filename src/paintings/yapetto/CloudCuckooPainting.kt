package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: cloud cuckoo. */
@Suppress("UnstableApiUsage")
internal object CloudCuckooPainting : PaintingInterface {
    override fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, "cloud_cuckoo"))
            .width(4)
            .height(4)
            .title(MM.deserialize("cloud_cuckoo".snakeToProperCase()))
            .author(MM.deserialize(YAPETTO))
}
