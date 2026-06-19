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
 * Each entry in [all] is a [PaintingInterface] registered under `illyriaplus:<name>`
 * with its sprite asset at `portfolio:<name>`.
 */
@Suppress("UnstableApiUsage")
internal object YapettoPaintings {
    private val DATA: List<PaintingData> =
        listOf(
            PaintingData("alpha", 1, 1),
            PaintingData("an_intruder", 2, 3),
            PaintingData("ancestor", 3, 2),
            PaintingData("anchor", 3, 3),
            PaintingData("aquaculture", 2, 2),
            PaintingData("awful_housing", 2, 4),
            PaintingData("beachside", 4, 1),
            PaintingData("best_friend", 2, 3),
            PaintingData("beta", 1, 1),
            PaintingData("bliss", 1, 2),
            PaintingData("blossoms", 2, 1),
            PaintingData("boscage", 3, 1),
            PaintingData("bouquet_edition", 1, 2),
            PaintingData("bubbles", 1, 1),
            PaintingData("caricature", 2, 4),
            PaintingData("cat", 1, 2),
            PaintingData("cave_game", 3, 2),
            PaintingData("chaos", 2, 2),
            PaintingData("cherry_moon", 2, 4),
            PaintingData("cloth", 4, 4),
            PaintingData("cloud_cuckoo", 4, 4),
            PaintingData("crusty", 1, 1),
            PaintingData("death", 3, 3),
            PaintingData("decay", 2, 2),
            PaintingData("distant_peaks", 2, 3),
            PaintingData("drippy", 4, 1),
            PaintingData("endy_warhol", 3, 3),
            PaintingData("escapeless", 2, 4),
            PaintingData("ether", 2, 2),
            PaintingData("eye", 3, 1),
            PaintingData("farlander", 1, 3),
            PaintingData("fauna", 2, 2),
            PaintingData("feather_falling", 4, 3),
            PaintingData("film", 1, 2),
            PaintingData("flora", 2, 2),
            PaintingData("fox", 1, 3),
            PaintingData("frost", 2, 2),
            PaintingData("gears", 3, 1),
            PaintingData("generator", 2, 2),
            PaintingData("giant", 4, 4),
            PaintingData("greatsword", 1, 3),
            PaintingData("gulls", 2, 4),
            PaintingData("harvest_moon", 2, 3),
            PaintingData("heartbeat", 2, 2),
            PaintingData("heavens_ladder", 1, 4),
            PaintingData("heirloom", 4, 1),
            PaintingData("ichor", 2, 2),
            PaintingData("iconography", 1, 2),
            PaintingData("jazz_town", 1, 2),
            PaintingData("john_devouring_his_son", 3, 4),
            PaintingData("journeys_end", 4, 4),
            PaintingData("justice", 4, 4),
            PaintingData("life", 2, 2),
            PaintingData("life_cycle", 3, 3),
            PaintingData("light", 2, 2),
            PaintingData("luminescent", 1, 3),
            PaintingData("macabre", 2, 2),
            PaintingData("macabre_alt", 2, 2),
            PaintingData("macrocosm", 2, 2),
            PaintingData("medley", 2, 1),
            PaintingData("mode_creative", 3, 3),
            PaintingData("moonlight_tower", 2, 3),
            PaintingData("morning_on_the_seine", 2, 3),
            PaintingData("mountains", 4, 1),
            PaintingData("never_blooming_wattle", 4, 2),
            PaintingData("night", 2, 2),
            PaintingData("nullity", 2, 2),
            PaintingData("nyctinasty", 3, 2),
            PaintingData("oak_door", 3, 4),
            PaintingData("operator", 2, 2),
            PaintingData("order", 2, 2),
            PaintingData("parrot", 2, 2),
            PaintingData("perennial", 1, 1),
            PaintingData("picturesque", 4, 2),
            PaintingData("pixel_gobelin", 2, 3),
            PaintingData("post_mortem", 2, 1),
            PaintingData("prickle", 1, 4),
            PaintingData("pyramid", 3, 2),
            PaintingData("rainbows", 4, 3),
            PaintingData("rainbows_alt", 4, 3),
            PaintingData("rainbows_trans", 4, 3),
            PaintingData("rainforest", 2, 1),
            PaintingData("rana", 2, 3),
            PaintingData("randomtickspeed", 2, 3),
            PaintingData("red_dawn", 2, 1),
            PaintingData("rising_sun_and_fading_death", 1, 2),
            PaintingData("rosemalling", 1, 2),
            PaintingData("sandstones", 1, 3),
            PaintingData("serpent", 2, 4),
            PaintingData("shapes", 1, 3),
            PaintingData("slime_chunk", 3, 2),
            PaintingData("squid_games", 1, 2),
            PaintingData("stair_hall", 4, 4),
            PaintingData("stalks", 1, 3),
            PaintingData("statue", 4, 3),
            PaintingData("still_life", 1, 1),
            PaintingData("storm", 4, 3),
            PaintingData("sunflower", 1, 3),
            PaintingData("sunrise_sparse", 2, 1),
            PaintingData("table", 1, 4),
            PaintingData("the_far_lands", 4, 3),
            PaintingData("the_far_lands_alt", 4, 3),
            PaintingData("the_painting_at_end_of_catalogue", 4, 2),
            PaintingData("the_scream", 3, 4),
            PaintingData("traveller", 2, 2),
            PaintingData("tussie_mussie", 1, 1),
            PaintingData("underworld", 3, 4),
            PaintingData("unwrap", 3, 2),
            PaintingData("vice", 1, 2),
            PaintingData("virtuosi_pas_de_deux", 4, 3),
            PaintingData("void_manor", 2, 3),
            PaintingData("waves", 3, 3),
            PaintingData("we_need_to_go_deeper", 3, 2),
            PaintingData("wildstyle", 2, 1),
            PaintingData("windmill_field", 3, 2),
            PaintingData("wrong_side", 1, 1),
            PaintingData("yonder", 2, 3),
        )

    /** All Yapetto painting variants derived from [DATA]. */
    val all: List<PaintingInterface> =
        DATA.map { data ->
            object : PaintingInterface {
                override val key: TypedKey<Art> =
                    TypedKey.create(RegistryKey.PAINTING_VARIANT, Key.key(IllyriaPlus.ID, data.name))

                override fun invoke(
                    builder: PaintingVariantRegistryEntry.Builder,
                ): PaintingVariantRegistryEntry.Builder =
                    builder
                        .assetId(Key.key("portfolio", data.name))
                        .width(data.width)
                        .height(data.height)
                        .title(
                            Component
                                .translatable("painting.portfolio.${data.name}.title")
                                .color(NamedTextColor.YELLOW),
                        )
                        .author(
                            Component
                                .translatable("painting.portfolio.${data.name}.author")
                                .color(NamedTextColor.GRAY),
                        )
            }
        }
}
