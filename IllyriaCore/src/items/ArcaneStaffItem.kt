package org.xodium.illyriacore.items

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.SmallFireball
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup
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
                RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
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
                        MM.deserialize("<gray>Type: $id <dark_gray>[<light_purple>Legendary<dark_gray>]"),
                        MM.deserialize("<dark_gray>A staff humming with raw arcane energy, waiting to be unleashed."),
                        MM.deserialize("<aqua>Right-click to launch a fireball."),
                    ),
                ),
            )
            setData(DataComponentTypes.ITEM_MODEL, Key.key(IllyriaCore.ID, id))
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ARMOR_TOUGHNESS,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_armor_toughness"),
                            4.0,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.KNOCKBACK_RESISTANCE,
                        AttributeModifier(
                            NamespacedKey(IllyriaCore.instance, "${id}_knockback_resistance"),
                            0.4,
                            AttributeModifier.Operation.ADD_NUMBER,
                        ),
                        EquipmentSlotGroup.MAINHAND,
                    ).build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.BOOLEAN, true) }
        }
}
