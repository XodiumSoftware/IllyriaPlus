package org.xodium.illyriaplus.paintings

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.PaintingVariantRegistryEntry
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Art
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.data.PaintingData

/**
 * Holder for all Yapetto paintings from the Portfolio datapack.
 *
 * Each entry in [all] is a [PaintingInterface] registered under `illyriaplus:<key>`
 * with its sprite asset at `portfolio:<key>`.
 */
@Suppress("UnstableApiUsage")
internal object YapettoPaintings {
    val ASSET_NAMESPACE = IllyriaPlus.ID
    val TITLE_COLOR = NamedTextColor.YELLOW
    val AUTHOR_COLOR = NamedTextColor.GRAY

    private val DATA: List<PaintingData> =
        listOf(
            PaintingData("alpha", Pair(1, 1)),
            PaintingData("an_intruder", Pair(2, 3)),
            PaintingData("ancestor", Pair(3, 2)),
            PaintingData("anchor", Pair(3, 3)),
            PaintingData("aquaculture", Pair(2, 2)),
            PaintingData("awful_housing", Pair(2, 4)),
            PaintingData("beachside", Pair(4, 1)),
            PaintingData("best_friend", Pair(2, 3)),
            PaintingData("beta", Pair(1, 1)),
            PaintingData("bliss", Pair(1, 2)),
            PaintingData("blossoms", Pair(2, 1)),
            PaintingData("boscage", Pair(3, 1)),
            PaintingData("bouquet_edition", Pair(1, 2)),
            PaintingData("bubbles", Pair(1, 1)),
            PaintingData("caricature", Pair(2, 4)),
            PaintingData("cat", Pair(1, 2)),
            PaintingData("cave_game", Pair(3, 2)),
            PaintingData("chaos", Pair(2, 2)),
            PaintingData("cherry_moon", Pair(2, 4)),
            PaintingData("cloth", Pair(4, 4)),
            PaintingData("cloud_cuckoo", Pair(4, 4)),
            PaintingData("crusty", Pair(1, 1)),
            PaintingData("death", Pair(3, 3)),
            PaintingData("decay", Pair(2, 2)),
            PaintingData("distant_peaks", Pair(2, 3)),
            PaintingData("drippy", Pair(4, 1)),
            PaintingData("endy_warhol", Pair(3, 3)),
            PaintingData("escapeless", Pair(2, 4)),
            PaintingData("ether", Pair(2, 2)),
            PaintingData("eye", Pair(3, 1)),
            PaintingData("farlander", Pair(1, 3)),
            PaintingData("fauna", Pair(2, 2)),
            PaintingData("feather_falling", Pair(4, 3)),
            PaintingData("film", Pair(1, 2)),
            PaintingData("flora", Pair(2, 2)),
            PaintingData("fox", Pair(1, 3)),
            PaintingData("frost", Pair(2, 2)),
            PaintingData("gears", Pair(3, 1)),
            PaintingData("generator", Pair(2, 2)),
            PaintingData("giant", Pair(4, 4)),
            PaintingData("greatsword", Pair(1, 3)),
            PaintingData("gulls", Pair(2, 4)),
            PaintingData("harvest_moon", Pair(2, 3)),
            PaintingData("heartbeat", Pair(2, 2)),
            PaintingData("heavens_ladder", Pair(1, 4)),
            PaintingData("heirloom", Pair(4, 1)),
            PaintingData("ichor", Pair(2, 2)),
            PaintingData("iconography", Pair(1, 2)),
            PaintingData("jazz_town", Pair(1, 2)),
            PaintingData("john_devouring_his_son", Pair(3, 4)),
            PaintingData("journeys_end", Pair(4, 4)),
            PaintingData("justice", Pair(4, 4)),
            PaintingData("life", Pair(2, 2)),
            PaintingData("life_cycle", Pair(3, 3)),
            PaintingData("light", Pair(2, 2)),
            PaintingData("luminescent", Pair(1, 3)),
            PaintingData("macabre", Pair(2, 2)),
            PaintingData("macabre_alt", Pair(2, 2)),
            PaintingData("macrocosm", Pair(2, 2)),
            PaintingData("medley", Pair(2, 1)),
            PaintingData("mode_creative", Pair(3, 3)),
            PaintingData("moonlight_tower", Pair(2, 3)),
            PaintingData("morning_on_the_seine", Pair(2, 3)),
            PaintingData("mountains", Pair(4, 1)),
            PaintingData("never_blooming_wattle", Pair(4, 2)),
            PaintingData("night", Pair(2, 2)),
            PaintingData("nullity", Pair(2, 2)),
            PaintingData("nyctinasty", Pair(3, 2)),
            PaintingData("oak_door", Pair(3, 4)),
            PaintingData("operator", Pair(2, 2)),
            PaintingData("order", Pair(2, 2)),
            PaintingData("parrot", Pair(2, 2)),
            PaintingData("perennial", Pair(1, 1)),
            PaintingData("picturesque", Pair(4, 2)),
            PaintingData("pixel_gobelin", Pair(2, 3)),
            PaintingData("post_mortem", Pair(2, 1)),
            PaintingData("prickle", Pair(1, 4)),
            PaintingData("pyramid", Pair(3, 2)),
            PaintingData("rainbows", Pair(4, 3)),
            PaintingData("rainbows_alt", Pair(4, 3)),
            PaintingData("rainbows_trans", Pair(4, 3)),
            PaintingData("rainforest", Pair(2, 1)),
            PaintingData("rana", Pair(2, 3)),
            PaintingData("randomtickspeed", Pair(2, 3)),
            PaintingData("red_dawn", Pair(2, 1)),
            PaintingData("rising_sun_and_fading_death", Pair(1, 2)),
            PaintingData("rosemalling", Pair(1, 2)),
            PaintingData("sandstones", Pair(1, 3)),
            PaintingData("serpent", Pair(2, 4)),
            PaintingData("shapes", Pair(1, 3)),
            PaintingData("slime_chunk", Pair(3, 2)),
            PaintingData("squid_games", Pair(1, 2)),
            PaintingData("stair_hall", Pair(4, 4)),
            PaintingData("stalks", Pair(1, 3)),
            PaintingData("statue", Pair(4, 3)),
            PaintingData("still_life", Pair(1, 1)),
            PaintingData("storm", Pair(4, 3)),
            PaintingData("sunflower", Pair(1, 3)),
            PaintingData("sunrise_sparse", Pair(2, 1)),
            PaintingData("table", Pair(1, 4)),
            PaintingData("the_far_lands", Pair(4, 3)),
            PaintingData("the_far_lands_alt", Pair(4, 3)),
            PaintingData("the_painting_at_end_of_catalogue", Pair(4, 2)),
            PaintingData("the_scream", Pair(3, 4)),
            PaintingData("traveller", Pair(2, 2)),
            PaintingData("tussie_mussie", Pair(1, 1)),
            PaintingData("underworld", Pair(3, 4)),
            PaintingData("unwrap", Pair(3, 2)),
            PaintingData("vice", Pair(1, 2)),
            PaintingData("virtuosi_pas_de_deux", Pair(4, 3)),
            PaintingData("void_manor", Pair(2, 3)),
            PaintingData("waves", Pair(3, 3)),
            PaintingData("we_need_to_go_deeper", Pair(3, 2)),
            PaintingData("wildstyle", Pair(2, 1)),
            PaintingData("windmill_field", Pair(3, 2)),
            PaintingData("wrong_side", Pair(1, 1)),
            PaintingData("yonder", Pair(2, 3)),
        )

    /** All Yapetto painting variants derived from [DATA]. */
    val all: List<PaintingInterface> =
        DATA.map { data ->
            object : PaintingInterface {
                override val key: TypedKey<Art> =
                    TypedKey.create(RegistryKey.PAINTING_VARIANT, Key.key(IllyriaPlus.ID, data.key))

                override fun invoke(
                    builder: PaintingVariantRegistryEntry.Builder,
                ): PaintingVariantRegistryEntry.Builder =
                    builder
                        .assetId(Key.key(ASSET_NAMESPACE, data.key))
                        .width(data.size.first)
                        .height(data.size.second)
                        .title(
                            Component
                                .translatable(data.title)
                                .color(TITLE_COLOR),
                        )
                        .author(
                            Component
                                .translatable(data.author)
                                .color(AUTHOR_COLOR),
                        )
            }
        }
}
