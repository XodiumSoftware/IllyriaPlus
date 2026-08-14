package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareGrindstoneEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.mechanics.MechanicInterface

/**
 * Combines an elytra with a chestplate in an anvil and separates it again in a grindstone.
 */
internal object ArmoredElytraMechanic : MechanicInterface {
    private val ARMORED_TAG = NamespacedKey(instance, "armored_elytra")

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: PrepareAnvilEvent) = prepareArmoredElytra(event)

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: PrepareGrindstoneEvent) = prepareGrindstoneSeparation(event)

    /**
     * Handles a PrepareAnvilEvent by validating inputs and setting the armored elytra result.
     *
     * @param event the prepare anvil event
     */
    private fun prepareArmoredElytra(event: PrepareAnvilEvent) {
        val inventory = event.inventory
        val elytra = inventory.firstItem ?: return
        val chestplate = inventory.secondItem ?: return

        if (!elytra.isElytra()) return
        if (!chestplate.isChestplate()) return
        if (elytra.isArmored()) return

        val armor = chestplate.armorValue()
        val repairCost = armor.toInt() + (chestplate.getData(DataComponentTypes.REPAIR_COST) ?: 0)
        val result = createArmoredElytra(elytra, chestplate)

        event.view.repairCost = repairCost
        event.result = result
    }

    /**
     * Handles a PrepareGrindstoneEvent by separating an armored elytra into its original pieces.
     *
     * The chestplate becomes the grindstone result, and the cleaned elytra is placed back into
     * the upper input slot.
     *
     * @param event the prepare grindstone event
     */
    private fun prepareGrindstoneSeparation(event: PrepareGrindstoneEvent) {
        val inventory = event.inventory
        val upper = inventory.upperItem ?: return
        val lower = inventory.lowerItem

        if (!upper.isArmored()) return
        if (lower != null && lower.type != Material.AIR) return

        val elytra = stripArmorData(upper)
        val chestplate = reconstructChestplate(upper)

        inventory.upperItem = elytra
        event.result = chestplate
    }

    /**
     * Reconstructs a chestplate from an armored elytra's lore line.
     *
     * @param armoredElytra the armored elytra to reconstruct from
     * @return the reconstructed chestplate, or a chainmail chestplate if the material cannot be determined
     */
    private fun reconstructChestplate(armoredElytra: ItemStack): ItemStack {
        val lore = armoredElytra.getData(DataComponentTypes.LORE)?.lines() ?: emptyList()
        val chestplateName =
            lore
                .firstOrNull { it.toString().startsWith("+ ") }
                ?.toString()
                ?.removePrefix("+ ")
        val material =
            chestplateName?.let { name ->
                Material.entries.firstOrNull { material ->
                    Tag.ITEMS_CHEST_ARMOR.isTagged(material) &&
                        name.contains(
                            material.key.key
                                .split("_")
                                .dropLast(1)
                                .joinToString(" "),
                            ignoreCase = true,
                        )
                }
            } ?: Material.CHAINMAIL_CHESTPLATE

        return ItemStack.of(material)
    }

    /**
     * Removes armor data from an armored elytra, returning it to a plain elytra.
     *
     * @param armoredElytra the armored elytra to clean
     * @return a cleaned elytra item stack
     */
    private fun stripArmorData(armoredElytra: ItemStack): ItemStack {
        val result = armoredElytra.clone()

        result.editPersistentDataContainer { it.remove(ARMORED_TAG) }

        result.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS)?.let { existing ->
            val filtered =
                existing.modifiers().filter {
                    it.modifier().key.namespace != ARMORED_TAG.namespace ||
                        it.modifier().key.key != ARMORED_TAG.key
                }
            val builder = ItemAttributeModifiers.itemAttributes()
            filtered.forEach { builder.addModifier(it.attribute(), it.modifier(), it.group) }
            result.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build())
        }

        val lore =
            armoredElytra
                .getData(DataComponentTypes.LORE)
                ?.lines()
                ?.filterNot { it.toString().startsWith("+ ") }
                ?: emptyList()
        result.setData(DataComponentTypes.LORE, ItemLore.lore(lore))

        return result
    }

    /**
     * Creates an armored elytra from the provided elytra and chestplate.
     *
     * Marks the result as armored, applies armor/toughness attribute modifiers,
     * appends descriptive lore, and merges enchantments from both input items.
     *
     * @param elytra the input elytra
     * @param chestplate the chestplate providing armor and enchantments
     * @return the combined armored elytra item
     */
    private fun createArmoredElytra(
        elytra: ItemStack,
        chestplate: ItemStack,
    ): ItemStack {
        val armor = chestplate.armorValue()
        val toughness = chestplate.armorToughnessValue()
        val result = elytra.clone()

        result.editPersistentDataContainer { it.set(ARMORED_TAG, PersistentDataType.BYTE, 1.toByte()) }

        val existing = result.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS)
        val builder =
            if (existing != null) {
                ItemAttributeModifiers
                    .itemAttributes()
                    .apply { existing.modifiers().forEach { addModifier(it.attribute(), it.modifier(), it.group) } }
            } else {
                ItemAttributeModifiers.itemAttributes()
            }

        builder.addModifier(
            Attribute.ARMOR,
            AttributeModifier(
                NamespacedKey(ARMORED_TAG.namespace, ARMORED_TAG.key),
                armor,
                AttributeModifier.Operation.ADD_NUMBER,
            ),
            EquipmentSlotGroup.CHEST,
        )
        builder.addModifier(
            Attribute.ARMOR_TOUGHNESS,
            AttributeModifier(
                NamespacedKey(ARMORED_TAG.namespace, "${ARMORED_TAG.key}_toughness"),
                toughness,
                AttributeModifier.Operation.ADD_NUMBER,
            ),
            EquipmentSlotGroup.CHEST,
        )

        result.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build())

        val chestplateName = chestplate.chestplateDisplayName()
        val loreLine = Component.text("+ ", NamedTextColor.GOLD).append(chestplateName)
        val lore = listOf(loreLine) + (result.getData(DataComponentTypes.LORE)?.lines() ?: emptyList())
        result.setData(DataComponentTypes.LORE, ItemLore.lore(lore))

        val merged = mergeEnchantments(elytra, chestplate)
        result.addUnsafeEnchantments(merged)

        return result
    }

    /**
     * Merges enchantments from an elytra and a chestplate following anvil rules.
     *
     * Higher levels take precedence; equal non-max levels combine by one.
     *
     * @param elytra the elytra enchantments
     * @param chestplate the chestplate enchantments
     * @return a map of merged enchantments and levels
     */
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

    /** Gets the display name of this chestplate, preferring custom name over default item name. */
    private fun ItemStack.chestplateDisplayName(): Component =
        getData(DataComponentTypes.CUSTOM_NAME)
            ?: getData(DataComponentTypes.ITEM_NAME)
            ?: Component.translatable(type.translationKey())

    /** Checks whether this item is an elytra. */
    private fun ItemStack.isElytra(): Boolean = type == Material.ELYTRA

    /** Checks whether this item is any chestplate armor. */
    private fun ItemStack.isChestplate(): Boolean = Tag.ITEMS_CHEST_ARMOR.isTagged(type)

    /** Checks whether this elytra has already been combined with a chestplate. */
    private fun ItemStack.isArmored(): Boolean = persistentDataContainer.has(ARMORED_TAG)

    /** Gets the total armor value provided by this chestplate type. */
    private fun ItemStack.armorValue(): Double = baseAttributeValue(Attribute.ARMOR)

    /** Gets the total armor toughness value provided by this chestplate type. */
    private fun ItemStack.armorToughnessValue(): Double = baseAttributeValue(Attribute.ARMOR_TOUGHNESS)

    /**
     * Sums the default attribute modifiers of the given type for this item's material.
     *
     * @param attribute the attribute to sum
     * @return the summed modifier amount, or 0.0 if none exist
     */
    private fun ItemStack.baseAttributeValue(attribute: Attribute): Double {
        val defaults = type.getDefaultData(DataComponentTypes.ATTRIBUTE_MODIFIERS) ?: return 0.0
        return defaults
            .modifiers()
            .filter { it.attribute() == attribute }
            .sumOf { it.modifier().amount }
    }
}
