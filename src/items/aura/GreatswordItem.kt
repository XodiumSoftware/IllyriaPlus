package org.xodium.illyriaplus.items.aura

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.Utils
import kotlin.math.cos
import kotlin.math.sin

/** Represents a Greatsword. */
@Suppress("UnstableApiUsage")
internal object GreatswordItem : AuraItemInterface {
    override val key: NamespacedKey = NamespacedKey(IllyriaPlus.instance, "greatsword")

    override fun invoke(): ItemStack =
        ItemStack.of(Material.NETHERITE_SWORD).apply {
            if (!hasData(DataComponentTypes.CUSTOM_NAME) && !hasData(DataComponentTypes.ITEM_NAME)) {
                setData(DataComponentTypes.CUSTOM_NAME, Utils.MM.deserialize("Greatsword"))
            }
            setData(DataComponentTypes.ITEM_MODEL, key)
            setData(
                DataComponentTypes.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers
                    .itemAttributes()
                    .addModifier(
                        Attribute.ATTACK_DAMAGE,
                        AttributeModifier(key, 10.0, AttributeModifier.Operation.ADD_NUMBER),
                        EquipmentSlotGroup.MAINHAND,
                    ).addModifier(
                        Attribute.ATTACK_SPEED,
                        AttributeModifier(key, 1.2, AttributeModifier.Operation.ADD_NUMBER),
                        EquipmentSlotGroup.MAINHAND,
                    ).build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.INTEGER, 1) }
        }

    override fun aura(player: Player) {
        val location = player.location
        val direction = location.direction.normalize()
        val pitch = Math.toRadians(location.pitch.toDouble())

        for (i in 1..8) {
            val distance = 0.5 + (i * 0.35)
            val offsetX = direction.x * distance
            val offsetY = direction.y * distance + 1.25
            val offsetZ = direction.z * distance
            val spiralX = cos(i * 0.75) * 0.15 * cos(pitch)
            val spiralY = sin(i * 0.75) * 0.15
            val spiralZ = cos(i * 0.75) * 0.15 * sin(pitch)

            player.world.spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                Location(
                    player.world,
                    location.x + offsetX + spiralX,
                    location.y + offsetY + spiralY,
                    location.z + offsetZ + spiralZ,
                ),
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                null,
                true,
            )
        }
    }
}
