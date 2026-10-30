package org.xodium.illyriacore.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.items.ItemsInterface
import org.xodium.illyriacore.items.adamantite.AdamantiteAxeItem
import org.xodium.illyriacore.items.adamantite.AdamantiteBootsItem
import org.xodium.illyriacore.items.adamantite.AdamantiteChestplateItem
import org.xodium.illyriacore.items.adamantite.AdamantiteHelmetItem
import org.xodium.illyriacore.items.adamantite.AdamantiteHoeItem
import org.xodium.illyriacore.items.adamantite.AdamantiteItem
import org.xodium.illyriacore.items.adamantite.AdamantiteLeggingsItem
import org.xodium.illyriacore.items.adamantite.AdamantitePickaxeItem
import org.xodium.illyriacore.items.adamantite.AdamantiteShovelItem
import org.xodium.illyriacore.items.adamantite.AdamantiteSpearItem
import org.xodium.illyriacore.items.adamantite.AdamantiteSwordItem
import org.xodium.illyriacore.items.draconic.DraconicAxeItem
import org.xodium.illyriacore.items.draconic.DraconicBootsItem
import org.xodium.illyriacore.items.draconic.DraconicChestplateItem
import org.xodium.illyriacore.items.draconic.DraconicHelmetItem
import org.xodium.illyriacore.items.draconic.DraconicHoeItem
import org.xodium.illyriacore.items.draconic.DraconicItem
import org.xodium.illyriacore.items.draconic.DraconicLeggingsItem
import org.xodium.illyriacore.items.draconic.DraconicPickaxeItem
import org.xodium.illyriacore.items.draconic.DraconicShovelItem
import org.xodium.illyriacore.items.draconic.DraconicSpearItem
import org.xodium.illyriacore.items.draconic.DraconicSwordItem
import org.xodium.illyriacore.items.mithril.MithrilAxeItem
import org.xodium.illyriacore.items.mithril.MithrilBootsItem
import org.xodium.illyriacore.items.mithril.MithrilChestplateItem
import org.xodium.illyriacore.items.mithril.MithrilHelmetItem
import org.xodium.illyriacore.items.mithril.MithrilHoeItem
import org.xodium.illyriacore.items.mithril.MithrilItem
import org.xodium.illyriacore.items.mithril.MithrilLeggingsItem
import org.xodium.illyriacore.items.mithril.MithrilPickaxeItem
import org.xodium.illyriacore.items.mithril.MithrilShovelItem
import org.xodium.illyriacore.items.mithril.MithrilSpearItem
import org.xodium.illyriacore.items.mithril.MithrilSwordItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumAxeItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumBootsItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumChestplateItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumHelmetItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumHoeItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumLeggingsItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumPickaxeItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumShovelItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumSpearItem
import org.xodium.illyriacore.items.orichalcum.OrichalcumSwordItem

/** Smithing upgrade chain: Netherite → Mithril → Adamantite → Orichalcum → Draconic. */
internal object CustomItemRecipe : RecipeInterface {
    private val TEMPLATE = RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)

    override val recipes: Set<SmithingTransformRecipe> =
        buildSet {
            // Swords
            smithingFromVanilla(
                "mithril_sword_smithing_recipe",
                Material.NETHERITE_SWORD,
                MithrilItem,
                MithrilSwordItem,
            )
            smithingFromCustom(
                "adamantite_sword_smithing_recipe",
                MithrilSwordItem,
                AdamantiteItem,
                AdamantiteSwordItem,
            )
            smithingFromCustom(
                "orichalcum_sword_smithing_recipe",
                AdamantiteSwordItem,
                OrichalcumItem,
                OrichalcumSwordItem,
            )
            smithingFromCustom("draconic_sword_smithing_recipe", OrichalcumSwordItem, DraconicItem, DraconicSwordItem)

            // Spears
            smithingFromVanilla(
                "mithril_spear_smithing_recipe",
                Material.NETHERITE_SPEAR,
                MithrilItem,
                MithrilSpearItem,
            )
            smithingFromCustom(
                "adamantite_spear_smithing_recipe",
                MithrilSpearItem,
                AdamantiteItem,
                AdamantiteSpearItem,
            )
            smithingFromCustom(
                "orichalcum_spear_smithing_recipe",
                AdamantiteSpearItem,
                OrichalcumItem,
                OrichalcumSpearItem,
            )
            smithingFromCustom("draconic_spear_smithing_recipe", OrichalcumSpearItem, DraconicItem, DraconicSpearItem)

            // Pickaxes
            smithingFromVanilla(
                "mithril_pickaxe_smithing_recipe",
                Material.NETHERITE_PICKAXE,
                MithrilItem,
                MithrilPickaxeItem,
            )
            smithingFromCustom(
                "adamantite_pickaxe_smithing_recipe",
                MithrilPickaxeItem,
                AdamantiteItem,
                AdamantitePickaxeItem,
            )
            smithingFromCustom(
                "orichalcum_pickaxe_smithing_recipe",
                AdamantitePickaxeItem,
                OrichalcumItem,
                OrichalcumPickaxeItem,
            )
            smithingFromCustom(
                "draconic_pickaxe_smithing_recipe",
                OrichalcumPickaxeItem,
                DraconicItem,
                DraconicPickaxeItem,
            )

            // Axes
            smithingFromVanilla("mithril_axe_smithing_recipe", Material.NETHERITE_AXE, MithrilItem, MithrilAxeItem)
            smithingFromCustom("adamantite_axe_smithing_recipe", MithrilAxeItem, AdamantiteItem, AdamantiteAxeItem)
            smithingFromCustom("orichalcum_axe_smithing_recipe", AdamantiteAxeItem, OrichalcumItem, OrichalcumAxeItem)
            smithingFromCustom("draconic_axe_smithing_recipe", OrichalcumAxeItem, DraconicItem, DraconicAxeItem)

            // Shovels
            smithingFromVanilla(
                "mithril_shovel_smithing_recipe",
                Material.NETHERITE_SHOVEL,
                MithrilItem,
                MithrilShovelItem,
            )
            smithingFromCustom(
                "adamantite_shovel_smithing_recipe",
                MithrilShovelItem,
                AdamantiteItem,
                AdamantiteShovelItem,
            )
            smithingFromCustom(
                "orichalcum_shovel_smithing_recipe",
                AdamantiteShovelItem,
                OrichalcumItem,
                OrichalcumShovelItem,
            )
            smithingFromCustom(
                "draconic_shovel_smithing_recipe",
                OrichalcumShovelItem,
                DraconicItem,
                DraconicShovelItem,
            )

            // Hoes
            smithingFromVanilla("mithril_hoe_smithing_recipe", Material.NETHERITE_HOE, MithrilItem, MithrilHoeItem)
            smithingFromCustom("adamantite_hoe_smithing_recipe", MithrilHoeItem, AdamantiteItem, AdamantiteHoeItem)
            smithingFromCustom("orichalcum_hoe_smithing_recipe", AdamantiteHoeItem, OrichalcumItem, OrichalcumHoeItem)
            smithingFromCustom("draconic_hoe_smithing_recipe", OrichalcumHoeItem, DraconicItem, DraconicHoeItem)

            // Helmets
            smithingFromVanilla(
                "mithril_helmet_smithing_recipe",
                Material.NETHERITE_HELMET,
                MithrilItem,
                MithrilHelmetItem,
            )
            smithingFromCustom(
                "adamantite_helmet_smithing_recipe",
                MithrilHelmetItem,
                AdamantiteItem,
                AdamantiteHelmetItem,
            )
            smithingFromCustom(
                "orichalcum_helmet_smithing_recipe",
                AdamantiteHelmetItem,
                OrichalcumItem,
                OrichalcumHelmetItem,
            )
            smithingFromCustom(
                "draconic_helmet_smithing_recipe",
                OrichalcumHelmetItem,
                DraconicItem,
                DraconicHelmetItem,
            )

            // Chestplates
            smithingFromVanilla(
                "mithril_chestplate_smithing_recipe",
                Material.NETHERITE_CHESTPLATE,
                MithrilItem,
                MithrilChestplateItem,
            )
            smithingFromCustom(
                "adamantite_chestplate_smithing_recipe",
                MithrilChestplateItem,
                AdamantiteItem,
                AdamantiteChestplateItem,
            )
            smithingFromCustom(
                "orichalcum_chestplate_smithing_recipe",
                AdamantiteChestplateItem,
                OrichalcumItem,
                OrichalcumChestplateItem,
            )
            smithingFromCustom(
                "draconic_chestplate_smithing_recipe",
                OrichalcumChestplateItem,
                DraconicItem,
                DraconicChestplateItem,
            )

            // Leggings
            smithingFromVanilla(
                "mithril_leggings_smithing_recipe",
                Material.NETHERITE_LEGGINGS,
                MithrilItem,
                MithrilLeggingsItem,
            )
            smithingFromCustom(
                "adamantite_leggings_smithing_recipe",
                MithrilLeggingsItem,
                AdamantiteItem,
                AdamantiteLeggingsItem,
            )
            smithingFromCustom(
                "orichalcum_leggings_smithing_recipe",
                AdamantiteLeggingsItem,
                OrichalcumItem,
                OrichalcumLeggingsItem,
            )
            smithingFromCustom(
                "draconic_leggings_smithing_recipe",
                OrichalcumLeggingsItem,
                DraconicItem,
                DraconicLeggingsItem,
            )

            // Boots
            smithingFromVanilla(
                "mithril_boots_smithing_recipe",
                Material.NETHERITE_BOOTS,
                MithrilItem,
                MithrilBootsItem,
            )
            smithingFromCustom(
                "adamantite_boots_smithing_recipe",
                MithrilBootsItem,
                AdamantiteItem,
                AdamantiteBootsItem,
            )
            smithingFromCustom(
                "orichalcum_boots_smithing_recipe",
                AdamantiteBootsItem,
                OrichalcumItem,
                OrichalcumBootsItem,
            )
            smithingFromCustom("draconic_boots_smithing_recipe", OrichalcumBootsItem, DraconicItem, DraconicBootsItem)
        }

    @Suppress("DEPRECATION")
    private fun MutableSet<SmithingTransformRecipe>.smithingFromVanilla(
        key: String,
        base: Material,
        addition: ItemsInterface,
        result: ItemsInterface,
    ) {
        add(
            SmithingTransformRecipe(
                NamespacedKey(instance, key),
                result(),
                TEMPLATE,
                RecipeChoice.MaterialChoice(base),
                RecipeChoice.ExactChoice(addition()),
            ),
        )
    }

    @Suppress("DEPRECATION")
    private fun MutableSet<SmithingTransformRecipe>.smithingFromCustom(
        key: String,
        base: ItemsInterface,
        addition: ItemsInterface,
        result: ItemsInterface,
    ) {
        add(
            SmithingTransformRecipe(
                NamespacedKey(instance, key),
                result(),
                TEMPLATE,
                RecipeChoice.ExactChoice(base()),
                RecipeChoice.ExactChoice(addition()),
            ),
        )
    }
}
