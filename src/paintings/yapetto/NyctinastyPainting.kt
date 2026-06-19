package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: nyctinasty. */
@Suppress("UnstableApiUsage")
internal object NyctinastyPainting : PaintingInterface {
    override fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, "nyctinasty"))
            .width(3)
            .height(2)
            .title(MM.deserialize("nyctinasty".snakeToProperCase()))
            .author(MM.deserialize(YAPETTO))
}
