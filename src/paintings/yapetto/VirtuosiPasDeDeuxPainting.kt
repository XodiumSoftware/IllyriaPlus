package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: virtuosi pas de deux. */
@Suppress("UnstableApiUsage")
internal object VirtuosiPasDeDeuxPainting : PaintingInterface {
    override fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, "virtuosi_pas_de_deux"))
            .width(4)
            .height(3)
            .title(MM.deserialize("virtuosi_pas_de_deux".snakeToProperCase()))
            .author(MM.deserialize(YAPETTO))
}
