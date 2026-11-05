package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.SmallFireball
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriacore.IllyriaCore
import org.xodium.illyrialib.Utils.MM

/** Custom Arcane Staff item. */
internal object ArcaneStaffItem : ItemsInterface {
    private const val COOLDOWN_TICKS: Long = 10

    override val recipes =
        setOf(
            SmithingTransformRecipe(
                NamespacedKey(IllyriaCore.instance, "${id}_smithing_transform"),
                invoke(),
                RecipeChoice.exactChoice(ArcaneStaffTemplateItem.invoke()),
                RecipeChoice.MaterialChoice(Material.BLAZE_ROD),
                RecipeChoice.MaterialChoice(Material.AMETHYST_SHARD),
            ),
        )

    @EventHandler
    fun on(event: PlayerInteractEvent) {
        val action = event.action
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        if (!isItem(item)) return

        val player = event.player
        if (player.hasCooldown(item.type)) return

        event.isCancelled = true
        player.setCooldown(item.type, COOLDOWN_TICKS.toInt())

        val direction = player.location.direction
        player.world.spawn(player.eyeLocation.add(direction.clone().multiply(0.5)), SmallFireball::class.java) {
            it.velocity = direction.multiply(1.5)
            it.shooter = player
        }
    }

    override fun invoke(): ItemStack =
        ItemStack.of(Material.BLAZE_ROD).apply {
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(
                    listOf(
                        MM.deserialize("A staff humming with raw arcane energy, waiting to be unleashed."),
                        MM.deserialize("Right-click to launch a fireball."),
                    ),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
