package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.mechanics.MechanicInterface

/**
 * Combines an elytra with a chestplate in an anvil, granting armor/toughness while keeping flight.
 */
@Suppress("UnstableApiUsage")
internal object ArmoredElytraMechanic : MechanicInterface {
    private val ARMOR_STATS: Map<Material, Pair<Double, Double>> =
            mapOf(
                    Material.LEATHER_CHESTPLATE to (3.0 to 0.0),
                    Material.CHAINMAIL_CHESTPLATE to (5.0 to 0.0),
                    Material.GOLDEN_CHESTPLATE to (5.0 to 0.0),
                    Material.IRON_CHESTPLATE to (6.0 to 0.0),
                    Material.DIAMOND_CHESTPLATE to (8.0 to 2.0),
                    Material.NETHERITE_CHESTPLATE to (8.0 to 3.0),
            )
    private val ARMORED_TAG = org.bukkit.NamespacedKey.minecraft("armored_elytra")

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: PrepareAnvilEvent) {
        val inventory = event.inventory
        val elytra = inventory.getItem(0) ?: return
        val chestplate = inventory.getItem(1) ?: return

        if (!elytra.isElytra()) return
        if (!chestplate.isChestplate()) return
        if (elytra.isArmored()) return

        val result = createArmoredElytra(elytra, chestplate) ?: return

        event.result = result
    }

    private fun ItemStack.isElytra(): Boolean = type == Material.ELYTRA

    private fun ItemStack.isChestplate(): Boolean = type in ARMOR_STATS.keys

    private fun ItemStack.isArmored(): Boolean = persistentDataContainer.has(ARMORED_TAG)

    private fun createArmoredElytra(
            elytra: ItemStack,
            chestplate: ItemStack,
    ): ItemStack? {
        val (armor, toughness) = ARMOR_STATS[chestplate.type] ?: return null
        val result = elytra.clone()

        result.editMeta {
            it.persistentDataContainer.set(
                    ARMORED_TAG,
                    org.bukkit.persistence.PersistentDataType.BYTE,
                    1
            )
        }

        val existing = result.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS)
        val builder =
                if (existing != null) ItemAttributeModifiers.itemAttributeModifiers(existing)
                else ItemAttributeModifiers.builder()

        builder.addModifier(
                Attribute.ARMOR,
                AttributeModifier(
                        org.bukkit.NamespacedKey(ARMORED_TAG.namespace, ARMORED_TAG.key),
                        armor,
                        AttributeModifier.Operation.ADD_NUMBER,
                        org.bukkit.inventory.EquipmentSlotGroup.CHEST,
                ),
        )
        builder.addModifier(
                Attribute.ARMOR_TOUGHNESS,
                AttributeModifier(
                        org.bukkit.NamespacedKey(
                                ARMORED_TAG.namespace,
                                "${ARMORED_TAG.key}_toughness"
                        ),
                        toughness,
                        AttributeModifier.Operation.ADD_NUMBER,
                        org.bukkit.inventory.EquipmentSlotGroup.CHEST,
                ),
        )

        result.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build())

        val materialName =
                chestplate.type.key.key.split("_").dropLast(1).joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
        val loreLine = Utils.MM.deserialize("<gold>+ $materialName Chestplate</gold>")
        val lore = (result.getData(DataComponentTypes.LORE)?.lines() ?: emptyList()) + loreLine
        result.setData(DataComponentTypes.LORE, ItemLore.lore(lore))

        val merged = mergeEnchantments(elytra, chestplate)
        result.addUnsafeEnchantments(merged)

        return result
    }

    private fun mergeEnchantments(
            elytra: ItemStack,
            chestplate: ItemStack,
    ): Map<Enchantment, Int> {
        val result = elytra.enchantments.toMutableMap()

        chestplate.enchantments.forEach { (enchant, level) ->
            val existing = result[enchant]
            result[enchant] =
                    when {
                        existing == null -> level
                        existing < level -> level
                        existing == level && level < enchant.maxLevel -> level + 1
                        else -> existing
                    }
        }

        return result
    }
}
