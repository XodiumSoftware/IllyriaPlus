package org.xodium.illyriaplus.items.alcoholics

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.PotionContents
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.items.ItemInterface

/** Represents a bottle of Mead. */
internal object MeadItem : ItemInterface {
    @Suppress("UnstableApiUsage")
    override operator fun invoke(): ItemStack =
        ItemStack.of(Material.POTION).apply {
            setData(DataComponentTypes.ITEM_NAME, MM.deserialize("<yellow>Mead"))
            setData(
                DataComponentTypes.POTION_CONTENTS,
                PotionContents
                    .potionContents()
                    .customColor(Color.YELLOW)
                    .addCustomEffect(PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, true, true))
                    .build(),
            )
        }
}
