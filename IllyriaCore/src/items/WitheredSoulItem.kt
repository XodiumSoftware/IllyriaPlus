package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.entity.Wither
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyrialib.Utils.MM

/** Custom item handling the Withered Soul drop from the Wither boss. */
internal object WitheredSoulItem : ItemsInterface {
    private const val DROP_CHANCE: Double = 0.25
    private const val MIN_DROPS: Int = 0
    private const val MAX_DROPS: Int = 1

    @EventHandler
    fun on(event: EntityDeathEvent) {
        val wither = event.entity as? Wither ?: return
        event.entity.killer ?: return
        if (wither.lastDamageCause?.entity !is Player) return

        if (Math.random() > DROP_CHANCE) return

        val amount = (MIN_DROPS..MAX_DROPS).random()
        if (amount <= 0) return

        event.drops.add(invoke().apply { this.amount = amount })
    }

    override fun invoke(): ItemStack =
        ItemStack.of(Material.WITHER_ROSE).apply {
            setData(
                DataComponentTypes.CUSTOM_NAME,
                MM.deserialize("<gradient:#1a1a1a:#4a1942:#1a1a1a>$name</gradient>"),
            )
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize(
                            "A crystallized fragment of the Wither's soul, " +
                                "containing the destructive energy of the Nether.",
                        ),
                    ),
                ),
            )
            setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.itemAttributes()
                    .addModifier(
                        Attribute.ARMOR,
                        AttributeModifier(
                            NamespacedKey(instance, "withered_soul_armor"),
                            4.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.HAND,
                    )
                    .addModifier(
                        Attribute.MAX_HEALTH,
                        AttributeModifier(
                            NamespacedKey(instance, "withered_soul_health"),
                            4.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.HAND,
                    )
                    .build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
