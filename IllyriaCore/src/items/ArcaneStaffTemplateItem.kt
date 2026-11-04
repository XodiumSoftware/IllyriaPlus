package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.entity.Blaze
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyrialib.Utils.MM

/** Smithing template used to forge an Arcane Staff. */
internal object ArcaneStaffTemplateItem : ItemsInterface {
    private const val DROP_CHANCE: Double = 0.025
    private const val MIN_DROPS: Int = 0
    private const val MAX_DROPS: Int = 1

    @EventHandler
    fun on(event: EntityDeathEvent) {
        val blaze = event.entity as? Blaze ?: return
        event.entity.killer ?: return
        if (blaze.lastDamageCause?.entity !is Player) return

        if (Math.random() > DROP_CHANCE) return

        val amount = (MIN_DROPS..MAX_DROPS).random()
        if (amount <= 0) return

        event.drops.add(invoke().apply { this.amount = amount })
    }

    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE).apply {
            setData(
                DataComponentTypes.CUSTOM_NAME,
                MM.deserialize("Arcane Staff Upgrade Smithing Template"),
            )
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("Used at a smithing table to upgrade a rod into an Arcane Staff."),
                    ),
                ),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
            setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        }
}
