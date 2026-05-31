package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Camel
import org.bukkit.entity.Husk
import org.bukkit.entity.Monster
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.xodium.illyriaplus.Utils
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import kotlin.random.Random

/** Represents a mechanic handling husk drops within the system. */
internal object HuskMechanic : MonsterInterface {
    private const val HUSK_SAND_DROP_CHANCE: Double = 1.0
    private const val HUSK_SAND_BASE_MIN: Int = 0
    private const val HUSK_SAND_BASE_MAX: Int = 2
    private const val HUSK_SAND_LOOTING_BONUS: Int = 1
    private const val CAMEL_HUSK_SAND_BASE_MAX: Int = 3
    private const val CAMEL_HUSK_SAND_LOOTING_BONUS: Int = 2

    override val attributes: Map<Attribute, (Monster, AttributeInstance) -> Unit> =
        mapOf(
            Attribute.ARMOR to { _, attr -> attr.baseValue = (2..4).random().toDouble() },
            Attribute.KNOCKBACK_RESISTANCE to { _, attr -> attr.baseValue = (3..6).random() / 10.0 },
        )

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.SAND)
                .setName(Utils.MM.deserialize("<mango>Husk Mechanics</gradient>"))
                .addLoreLines(
                    Utils.MM.deserialize(""),
                    Utils.MM.deserialize(
                        "<yellow>Sand Drops</yellow> <firewatch>></gradient> <white>Drop 0-2 sand " +
                            "(+Looting, bonus on camel)</white>",
                    ),
                    Utils.MM.deserialize(
                        "<yellow>Attribute Modifiers</yellow> <firewatch>></gradient> " +
                            "<white>+2-4 armor, +30-60% KB resist.</white>",
                    ),
                ),
        )

    @EventHandler(ignoreCancelled = true)
    fun on(event: CreatureSpawnEvent) {
        when {
            event.entity.world.difficulty != difficulty -> return
            else -> modifySpawn(event.entity as? Husk ?: return)
        }
    }

    @EventHandler
    fun on(event: EntityDeathEvent) = huskDrop(event)

    /**
     * Handles husk death drops.
     *
     * @param event The EntityDeathEvent triggered when an entity dies.
     */
    private fun huskDrop(event: EntityDeathEvent) {
        if (event.entity !is Husk) return
        if (Random.nextDouble() > HUSK_SAND_DROP_CHANCE) return

        val isCamelHusk = event.entity.vehicle is Camel
        val lootingLevel =
            event.entity.killer
                ?.inventory
                ?.itemInMainHand
                ?.getEnchantmentLevel(Enchantment.LOOTING) ?: 0
        val minAmount = HUSK_SAND_BASE_MIN
        val maxAmount =
            if (isCamelHusk) {
                CAMEL_HUSK_SAND_BASE_MAX + (lootingLevel * CAMEL_HUSK_SAND_LOOTING_BONUS)
            } else {
                HUSK_SAND_BASE_MAX + (lootingLevel * HUSK_SAND_LOOTING_BONUS)
            }
        val amount = Random.nextInt(minAmount, maxAmount + 1)

        if (amount > 0) event.drops.add(ItemStack.of(Material.SAND, amount))
    }
}
