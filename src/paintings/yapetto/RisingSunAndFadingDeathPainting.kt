package org.xodium.illyriaplus.paintings.yapetto

import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.snakeToProperCase
import org.xodium.illyriaplus.paintings.PaintingInterface
import org.xodium.illyriaplus.paintings.PaintingInterface.Companion.YAPETTO

/** Yapetto painting variant: rising sun and fading death. */
@Suppress("UnstableApiUsage")
internal object RisingSunAndFadingDeathPainting : PaintingInterface {
    override fun invoke(builder: PaintingVariantRegistryEntry.Builder): PaintingVariantRegistryEntry.Builder =
        builder
            .assetId(Key.key(YAPETTO, "rising_sun_and_fading_death"))
            .width(1)
            .height(2)
            .title(MM.deserialize("rising_sun_and_fading_death".snakeToProperCase()))
            .author(MM.deserialize(YAPETTO))
}
