package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/**
 * Custom Nullspire item.
 *
 * A slender, void-touched spear whose purple core pierces armor and wards alike. Unbound from
 * mortal limits, it strikes harder and faster than any mortal weapon while reaching well beyond
 * its wielder's arm.
 */
internal object NullspireItem : ItemsInterface {
    private const val BLINDNESS_DURATION_TICKS: Int = 60
    private const val BLINDNESS_AMPLIFIER: Int = 0

    // TODO: The texture illyriacore:item/weapons/nullspire does not exist yet. Add it at
    //  illyria-resourcepack/src/assets/illyriacore/textures/item/weapons/nullspire.png and copy
    //  it to illyria-wiki/src/img/item/nullspire.png for the wiki card icon.
    override val recipes =
        setOf(
            SmithingTransformRecipe(
                NamespacedKey(IllyriaCore.instance, "${id}_smithing_transform"),
                invoke(),
                RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                RecipeChoice.MaterialChoice(Material.NETHERITE_SWORD),
                RecipeChoice.MaterialChoice(Material.DRAGON_EGG),
            ),
        )

    @EventHandler
    fun on(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (!isItem(damager.inventory.itemInMainHand)) return

        val victim = event.entity as? LivingEntity ?: return
        victim.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, BLINDNESS_DURATION_TICKS, BLINDNESS_AMPLIFIER))
    }

    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SWORD).apply {
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("A needle of collapsed void, its violet core humming with nothingness."),
                        MM.deserialize("Striking a foe blinds them for 3 seconds."),
                    ),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ATTACK_DAMAGE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_attack_damage"),
                            20.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ATTACK_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_attack_speed"),
                            2.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ATTACK_KNOCKBACK,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_attack_knockback"),
                            1.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ENTITY_INTERACTION_RANGE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_entity_interaction_range"),
                            2.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.MOVEMENT_SPEED,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_movement_speed"),
                            0.2,
                            AttributeModifier.Operation.ADD_SCALAR,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
