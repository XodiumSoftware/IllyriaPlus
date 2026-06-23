package org.xodium.illyriaplus.recipes.vanilla

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.PotionContents
import io.papermc.paper.potion.PotionMix
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.potion.PotionType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.items.alcoholics.AbsintheItem
import org.xodium.illyriaplus.items.alcoholics.AleItem
import org.xodium.illyriaplus.items.alcoholics.ChorusWineItem
import org.xodium.illyriaplus.items.alcoholics.CiderItem
import org.xodium.illyriaplus.items.alcoholics.GlowshineItem
import org.xodium.illyriaplus.items.alcoholics.MeadItem
import org.xodium.illyriaplus.items.alcoholics.MoonshineItem
import org.xodium.illyriaplus.items.alcoholics.NetherAleItem
import org.xodium.illyriaplus.items.alcoholics.RedWineItem
import org.xodium.illyriaplus.items.alcoholics.RumItem
import org.xodium.illyriaplus.items.alcoholics.StoutItem
import org.xodium.illyriaplus.items.alcoholics.VodkaItem
import org.xodium.illyriaplus.items.alcoholics.WhiskeyItem
import org.xodium.illyriaplus.recipes.RecipeInterface

/** Represents an object handling custom alcoholic drink brewing recipes within the system. */
internal object AlcoholRecipe : RecipeInterface {
    override val potions: Collection<PotionMix>
        get() =
            setOf(
                PotionMix(
                    NamespacedKey(instance, "absinthe_brewing_recipe"),
                    AbsintheItem(),
                    RecipeChoice.ExactChoice(input(PotionType.AWKWARD)),
                    RecipeChoice.MaterialChoice(Material.WITHER_ROSE),
                ),
                PotionMix(
                    NamespacedKey(instance, "ale_brewing_recipe"),
                    AleItem(),
                    RecipeChoice.ExactChoice(input(PotionType.AWKWARD)),
                    RecipeChoice.MaterialChoice(Material.WHEAT),
                ),
                PotionMix(
                    NamespacedKey(instance, "chorus_wine_brewing_recipe"),
                    ChorusWineItem(),
                    RecipeChoice.ExactChoice(input(PotionType.WATER)),
                    RecipeChoice.MaterialChoice(Material.CHORUS_FRUIT),
                ),
                PotionMix(
                    NamespacedKey(instance, "cider_brewing_recipe"),
                    CiderItem(),
                    RecipeChoice.ExactChoice(input(PotionType.WATER)),
                    RecipeChoice.MaterialChoice(Material.APPLE),
                ),
                PotionMix(
                    NamespacedKey(instance, "glowshine_brewing_recipe"),
                    GlowshineItem(),
                    RecipeChoice.ExactChoice(input(PotionType.AWKWARD)),
                    RecipeChoice.MaterialChoice(Material.GLOW_BERRIES),
                ),
                PotionMix(
                    NamespacedKey(instance, "mead_brewing_recipe"),
                    MeadItem(),
                    RecipeChoice.ExactChoice(input(PotionType.WATER)),
                    RecipeChoice.MaterialChoice(Material.HONEYCOMB),
                ),
                PotionMix(
                    NamespacedKey(instance, "moonshine_brewing_recipe"),
                    MoonshineItem(),
                    RecipeChoice.ExactChoice(input(PotionType.AWKWARD)),
                    RecipeChoice.MaterialChoice(Material.SUGAR),
                ),
                PotionMix(
                    NamespacedKey(instance, "nether_ale_brewing_recipe"),
                    NetherAleItem(),
                    RecipeChoice.ExactChoice(input(PotionType.AWKWARD)),
                    RecipeChoice.MaterialChoice(Material.NETHER_WART),
                ),
                PotionMix(
                    NamespacedKey(instance, "red_wine_brewing_recipe"),
                    RedWineItem(),
                    RecipeChoice.ExactChoice(input(PotionType.WATER)),
                    RecipeChoice.MaterialChoice(Material.SWEET_BERRIES),
                ),
                PotionMix(
                    NamespacedKey(instance, "rum_brewing_recipe"),
                    RumItem(),
                    RecipeChoice.ExactChoice(input(PotionType.AWKWARD)),
                    RecipeChoice.MaterialChoice(Material.SUGAR_CANE),
                ),
                PotionMix(
                    NamespacedKey(instance, "stout_brewing_recipe"),
                    StoutItem(),
                    RecipeChoice.ExactChoice(input(PotionType.AWKWARD)),
                    RecipeChoice.MaterialChoice(Material.COCOA_BEANS),
                ),
                PotionMix(
                    NamespacedKey(instance, "vodka_brewing_recipe"),
                    VodkaItem(),
                    RecipeChoice.ExactChoice(input(PotionType.AWKWARD)),
                    RecipeChoice.MaterialChoice(Material.POTATO),
                ),
                PotionMix(
                    NamespacedKey(instance, "whiskey_brewing_recipe"),
                    WhiskeyItem(),
                    RecipeChoice.ExactChoice(input(PotionType.AWKWARD)),
                    RecipeChoice.MaterialChoice(Material.WHEAT),
                ),
            )

    /**
     * Creates a potion input bottle with the specified base potion type.
     *
     * @param type The base [PotionType] to set on the input bottle.
     * @return An [ItemStack] representing the brewing input.
     */
    @Suppress("UnstableApiUsage")
    private fun input(type: PotionType): ItemStack =
        ItemStack.of(Material.POTION).apply {
            setData(
                DataComponentTypes.POTION_CONTENTS,
                PotionContents.potionContents().potion(type).build(),
            )
        }
}
