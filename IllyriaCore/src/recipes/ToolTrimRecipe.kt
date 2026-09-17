package org.xodium.illyriacore.recipes

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemArmorTrim
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.keys.TrimMaterialKeys
import io.papermc.paper.registry.keys.TrimPatternKeys
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyriacore.IllyriaCore.Companion.instance

/**
 * Adds smithing recipes that let players apply any vanilla armor-trim pattern to any tool
 * (sword, pickaxe, axe, shovel, hoe — every tier from wooden to netherite). Each
 * (tool, pattern, material) triple yields one [SmithingTransformRecipe] whose result carries
 * both the vanilla `minecraft:trim` component (tooltip + worn-model rendering) and an
 * `item_model` component that points to a pre-baked sprite in the bundled resource pack
 * (so trimmed tools render properly in inventories and on drop).
 *
 * Total recipe count: 6 tiers × 5 tool types × 18 patterns × 11 materials = 5,940.
 */
@Suppress("UnstableApiUsage")
internal object ToolTrimRecipe : RecipeInterface {
    private val TIERS = listOf("wooden", "stone", "iron", "golden", "diamond", "netherite")
    private val TOOL_TYPES = listOf("sword", "pickaxe", "axe", "shovel", "hoe")

    private val PATTERN_KEYS =
        listOf(
            "bolt" to TrimPatternKeys.BOLT,
            "coast" to TrimPatternKeys.COAST,
            "dune" to TrimPatternKeys.DUNE,
            "eye" to TrimPatternKeys.EYE,
            "flow" to TrimPatternKeys.FLOW,
            "host" to TrimPatternKeys.HOST,
            "raiser" to TrimPatternKeys.RAISER,
            "rib" to TrimPatternKeys.RIB,
            "sentry" to TrimPatternKeys.SENTRY,
            "shaper" to TrimPatternKeys.SHAPER,
            "silence" to TrimPatternKeys.SILENCE,
            "snout" to TrimPatternKeys.SNOUT,
            "spire" to TrimPatternKeys.SPIRE,
            "tide" to TrimPatternKeys.TIDE,
            "vex" to TrimPatternKeys.VEX,
            "ward" to TrimPatternKeys.WARD,
            "wayfinder" to TrimPatternKeys.WAYFINDER,
            "wild" to TrimPatternKeys.WILD,
        )

    private val MATERIAL_KEYS =
        listOf(
            Triple("amethyst", TrimMaterialKeys.AMETHYST, Material.AMETHYST_SHARD),
            Triple("copper", TrimMaterialKeys.COPPER, Material.COPPER_INGOT),
            Triple("diamond", TrimMaterialKeys.DIAMOND, Material.DIAMOND),
            Triple("emerald", TrimMaterialKeys.EMERALD, Material.EMERALD),
            Triple("gold", TrimMaterialKeys.GOLD, Material.GOLD_INGOT),
            Triple("iron", TrimMaterialKeys.IRON, Material.IRON_INGOT),
            Triple("lapis", TrimMaterialKeys.LAPIS, Material.LAPIS_LAZULI),
            Triple("netherite", TrimMaterialKeys.NETHERITE, Material.NETHERITE_INGOT),
            Triple("quartz", TrimMaterialKeys.QUARTZ, Material.QUARTZ),
            Triple("redstone", TrimMaterialKeys.REDSTONE, Material.REDSTONE),
            Triple("resin", TrimMaterialKeys.RESIN, Material.RESIN_BRICK),
        )

    private fun templateItemFor(patternName: String): Material =
        Material.getMaterial("${patternName.uppercase()}_ARMOR_TRIM_SMITHING_TEMPLATE")
            ?: throw IllegalArgumentException("No armor-trim template item found for pattern: $patternName")

    override val recipes: Collection<SmithingTransformRecipe> by lazy {
        val patternRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN)
        val materialRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL)

        val patterns: List<Pair<String, TrimPattern>> =
            PATTERN_KEYS.map { (name, key) -> name to patternRegistry.getOrThrow(key) }
        val materials: List<Triple<String, TrimMaterial, Material>> =
            MATERIAL_KEYS.map { (name, key: TypedKey<TrimMaterial>, ingredient) ->
                Triple(name, materialRegistry.getOrThrow(key), ingredient)
            }

        val out =
            ArrayList<SmithingTransformRecipe>(
                TIERS.size * TOOL_TYPES.size * patterns.size * materials.size,
            )
        TIERS.forEach { tier ->
            TOOL_TYPES.forEach { tool ->
                val toolMaterial = Material.getMaterial("${tier.uppercase()}_${tool.uppercase()}") ?: return@forEach
                patterns.forEach { (patternName, pattern) ->
                    val template = templateItemFor(patternName)
                    val modelKey = Key.key(IllyriaCore.ID, "${tier}_${tool}_$patternName")
                    materials.forEach { (materialName, material, ingredient) ->
                        val result =
                            ItemStack.of(toolMaterial).apply {
                                setData(
                                    DataComponentTypes.TRIM,
                                    ItemArmorTrim.itemArmorTrim(ArmorTrim(material, pattern)),
                                )
                                setData(DataComponentTypes.ITEM_MODEL, modelKey)
                            }
                        out +=
                            SmithingTransformRecipe(
                                NamespacedKey(instance, "${tier}_${tool}_${materialName}_${patternName}_smithing_trim"),
                                result,
                                RecipeChoice.MaterialChoice(template),
                                RecipeChoice.MaterialChoice(toolMaterial),
                                RecipeChoice.MaterialChoice(ingredient),
                            )
                    }
                }
            }
        }
        out
    }
}
