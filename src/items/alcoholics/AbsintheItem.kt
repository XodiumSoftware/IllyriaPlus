package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.items.ItemInterface
import org.xodium.illyriaplus.items.ItemInterface.Companion.ALCOHOL_STRENGTH_KEY
import org.xodium.illyriaplus.toTicks
import kotlin.time.Duration.Companion.seconds

/** Represents a bottle of Absinthe. */
internal object AbsintheItem : ItemInterface {
    override val alcoholStrength: Int = 4

    @Suppress("UnstableApiUsage")
    override operator fun invoke(): ItemStack =
        ItemStack.of(Material.POTION).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<!i><#9ACD32>Absinthe"))
            setData(
                DataComponentTypes.POTION_CONTENTS,
                PotionContents
                    .potionContents()
                    .customColor(Color.fromRGB(154, 205, 50))
                    .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 120.seconds.toTicks(), 3, false, true, true))
                    .build(),
            )
            editPersistentDataContainer { it.set(ALCOHOL_STRENGTH_KEY, PersistentDataType.INTEGER, alcoholStrength) }
        }
}
