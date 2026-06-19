package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: we need to go deeper. */
@Suppress("UnstableApiUsage")
internal object WeNeedToGoDeeperPainting : PaintingInterface {
    override fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, "we_need_to_go_deeper"))
            .width(3)
            .height(2)
            .title(MM.deserialize("we_need_to_go_deeper".snakeToProperCase()))
            .author(MM.deserialize(YAPETTO))
}
