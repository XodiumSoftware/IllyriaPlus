package org.xodium.illyriaplus.recipes.custom

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData
import io.papermc.paper.datacomponent.item.Equippable
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.StonecuttingRecipe
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.recipes.RecipeInterface

/**
 * Represents an object handling armor skin recipe implementation within the system.
 *
 * Players can place an existing armor piece in a stonecutter to apply a cosmetic skin.
 * The output keeps the original material, durability, enchantments, and other components;
 * the [DataComponentTypes.ITEM_MODEL], [DataComponentTypes.CUSTOM_MODEL_DATA],
 * [DataComponentTypes.EQUIPPABLE], and [DataComponentTypes.CUSTOM_NAME] components are changed.
 */
@Suppress("UnstableApiUsage")
internal object ArmorSkinRecipe : RecipeInterface {
    /** Armor slot tags from Paper mapped to their [EquipmentSlot]. */
    private val ARMOR_SLOTS =
        mapOf(
            Tag.ITEMS_HEAD_ARMOR to EquipmentSlot.HEAD,
            Tag.ITEMS_CHEST_ARMOR to EquipmentSlot.CHEST,
            Tag.ITEMS_LEG_ARMOR to EquipmentSlot.LEGS,
            Tag.ITEMS_FOOT_ARMOR to EquipmentSlot.FEET,
        )

    /** Available cosmetic skin sets. */
    private val SKINS =
        listOf(
            "knight",
        )

    override val recipes =
        buildSet {
            ARMOR_SLOTS.forEach { (tag, slot) ->
                val slotKey = slot.name.lowercase()
                tag.values.forEach { material ->
                    val materialKey = material.key.value()
                    SKINS.forEach { skin ->
                        val recipeId = "armor_skin_${materialKey}_${slotKey}_${skin}_stonecutting_recipe"
                        val recipeKey = NamespacedKey(instance, recipeId)
                        val displayName = skin.replaceFirstChar { it.uppercase() }
                        val result = ItemStack.of(material)
                        add(
                            StonecuttingRecipe(
                                recipeKey,
                                result.apply {
                                    setData(DataComponentTypes.ITEM_MODEL, NamespacedKey(instance, skin))
                                    setData(
                                        DataComponentTypes.CUSTOM_MODEL_DATA,
                                        CustomModelData.customModelData().addString(slotKey).build(),
                                    )
                                    setData(
                                        DataComponentTypes.EQUIPPABLE,
                                        result.getData(DataComponentTypes.EQUIPPABLE)?.toBuilder()
                                            ?.assetId(NamespacedKey(instance, skin))?.build()
                                            ?: Equippable.equippable(slot)
                                                .assetId(NamespacedKey(instance, skin)).build(),
                                    )
                                    setData(
                                        DataComponentTypes.CUSTOM_NAME,
                                        Utils.MM.deserialize(displayName),
                                    )
                                },
                                material,
                            ),
                        )
                    }
                }
            }
        }
}
