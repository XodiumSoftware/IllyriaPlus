package org.xodium.illyriacore.variants.wolves

import io.papermc.paper.registry.data.WolfVariantRegistryEntry
import io.papermc.paper.registry.data.client.ClientTextureAsset
import net.kyori.adventure.key.Key
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyriacore.variants.VariantInterface

/**
 * A rare golden wolf variant.
 *
 * Golden wolves are extremely rare variants that spawn in forest biomes.
 * Their lustrous golden coat makes them highly prized by collectors.
 */
@Suppress("UnstableApiUsage")
internal object GoldenWolfVariant : VariantInterface {
    override fun invoke(builder: WolfVariantRegistryEntry.Builder): WolfVariantRegistryEntry.Builder =
        builder
            .wildClientTextureAsset(
                ClientTextureAsset.clientTextureAsset(
                    key.key(),
                    Key.key(IllyriaCore.ID, "entity/wolf/golden_wolf"),
                ),
            ).tameClientTextureAsset(
                ClientTextureAsset.clientTextureAsset(
                    key.key(),
                    Key.key(IllyriaCore.ID, "entity/wolf/golden_wolf_tame"),
                ),
            ).angryClientTextureAsset(
                ClientTextureAsset.clientTextureAsset(
                    key.key(),
                    Key.key(IllyriaCore.ID, "entity/wolf/golden_wolf_angry"),
                ),
            )
}
