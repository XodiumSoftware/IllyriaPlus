package org.xodium.illyriaplus.items

import com.destroystokyo.paper.profile.ProfileProperty
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.ResolvableProfile
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.PotData
import java.util.UUID

/** Represents a custom flower pot item builder for stonecutting variants. */
@Suppress("UnstableApiUsage")
internal object PotItem : ItemInterface {
    override val key: NamespacedKey = NamespacedKey(instance, "pot")

    override fun invoke(): ItemStack = throw UnsupportedOperationException("Use invoke(data) instead")

    /**
     * Builds a custom flower pot head for the given [PotData].
     *
     * @param data The pot variant data.
     * @return The configured player head [ItemStack].
     */
    operator fun invoke(data: PotData): ItemStack =
        ItemStack.of(Material.PLAYER_HEAD).apply {
            setData(DataComponentTypes.CUSTOM_NAME, MM.deserialize("<reset>Flower Pot"))
            setData(
                DataComponentTypes.LORE,
                ItemLore.lore(listOf(MM.deserialize("<gray>${data.lore}</gray>"))),
            )
            setData(
                DataComponentTypes.PROFILE,
                ResolvableProfile
                    .resolvableProfile()
                    .uuid(UUID.nameUUIDFromBytes(data.key.toByteArray()))
                    .addProperty(ProfileProperty("textures", data.texture))
                    .build(),
            )
            editPersistentDataContainer { it.set(key, PersistentDataType.STRING, data.key) }
        }
}
