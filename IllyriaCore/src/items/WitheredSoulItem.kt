package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Wither
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriacore.scoreboards.SoulboundScoreboard
import org.xodium.illyrialib.Utils.MM

/** Custom item handling the Withered Soul drop from the Wither boss. */
internal object WitheredSoulItem : ItemsInterface {
    private const val DROP_CHANCE: Double = 0.25
    private const val MIN_DROPS: Int = 0
    private const val MAX_DROPS: Int = 1

    private val activeEffects = mutableMapOf<String, MutableList<String>>()

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

    @EventHandler
    fun on(event: PlayerItemConsumeEvent) {
        val item = event.item
        if (!item.persistentDataContainer.has(key, PersistentDataType.BOOLEAN)) return

        val player = event.player
        val effectName = "Withered Soul"

        activeEffects.getOrPut(player.name) { mutableListOf() }.add(effectName)
        SoulboundScoreboard.update(player.name, activeEffects[player.name].orEmpty())

        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 200, 1))
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
                DataComponentTypes.CONSUMABLE,
                Consumable.consumable()
                    .consumeSeconds(1.0f)
                    .animation(ItemUseAnimation.EAT)
                    .build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
