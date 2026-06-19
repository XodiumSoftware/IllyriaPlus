package org.xodium.illyriaplus

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.ItemTypeKeys
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import io.papermc.paper.registry.keys.tags.PaintingVariantTagKeys
import io.papermc.paper.registry.tag.TagKey
import io.papermc.paper.tag.TagEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.enchantments.utility.EmbertreadEnchantment
import org.xodium.illyriaplus.enchantments.utility.NimbusEnchantment
import org.xodium.illyriaplus.enchantments.utility.TetherEnchantment
import org.xodium.illyriaplus.enchantments.utility.VinemineEnchantment
import org.xodium.illyriaplus.paintings.YapettoPaintings

/** Main bootstrap class of the plugin. */
@Suppress("UnstableApiUsage", "Unused")
internal class IllyriaPlusBootstrap : PluginBootstrap {
    companion object {
        val TOOLS = TagKey.create(RegistryKey.ITEM, Key.key(IllyriaPlus.ID, "tools"))
        val WEAPONS = TagKey.create(RegistryKey.ITEM, Key.key(IllyriaPlus.ID, "weapons"))
        val TETHER_ITEMS = TagKey.create(RegistryKey.ITEM, Key.key(IllyriaPlus.ID, "tether_items"))
    }

    override fun bootstrap(ctx: BootstrapContext) {
        ctx.lifecycleManager.apply {
            registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.ITEM)) { event ->
                event.registrar().apply {
                    setTag(
                        TOOLS,
                        setOf(
                            TagEntry.tagEntry(ItemTypeTagKeys.PICKAXES),
                            TagEntry.tagEntry(ItemTypeTagKeys.AXES),
                            TagEntry.tagEntry(ItemTypeTagKeys.SHOVELS),
                            TagEntry.tagEntry(ItemTypeTagKeys.HOES),
                            TagEntry.valueEntry(ItemTypeKeys.SHEARS),
                            TagEntry.valueEntry(ItemTypeKeys.BRUSH),
                            TagEntry.valueEntry(ItemTypeKeys.FISHING_ROD),
                        ),
                    )
                    setTag(
                        WEAPONS,
                        setOf(
                            TagEntry.tagEntry(ItemTypeTagKeys.SWORDS),
                            TagEntry.valueEntry(ItemTypeKeys.BOW),
                            TagEntry.valueEntry(ItemTypeKeys.CROSSBOW),
                            TagEntry.valueEntry(ItemTypeKeys.TRIDENT),
                            TagEntry.valueEntry(ItemTypeKeys.MACE),
                            TagEntry.tagEntry(ItemTypeTagKeys.SPEARS),
                        ),
                    )
                    setTag(
                        TETHER_ITEMS,
                        setOf(
                            TagEntry.tagEntry(TOOLS),
                            TagEntry.tagEntry(WEAPONS),
                        ),
                    )
                }
            }
            registerEventHandler(
                RegistryEvents.ENCHANTMENT.compose().newHandler { event ->
                    event.registry().apply {
                        register(VinemineEnchantment.key) {
                            VinemineEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.PICKAXES))
                        }
                        register(TetherEnchantment.key) {
                            TetherEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(TETHER_ITEMS))
                        }
                        register(NimbusEnchantment.key) {
                            NimbusEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.HARNESSES))
                        }
                        register(EmbertreadEnchantment.key) {
                            EmbertreadEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.FOOT_ARMOR))
                        }
                    }
                },
            )
            registerEventHandler(
                RegistryEvents.PAINTING_VARIANT.compose().newHandler { event ->
                    event.registry().apply {
                        YapettoPaintings.paintings.forEach { painting ->
                            register(painting.key) { painting.invoke(it) }
                        }
                    }
                },
            )
            registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT)) { event ->
                event.registrar().apply {
                    val enchants =
                        setOf(
                            VinemineEnchantment.key,
                            TetherEnchantment.key,
                            NimbusEnchantment.key,
                            EmbertreadEnchantment.key,
                        )

                    addToTag(EnchantmentTagKeys.TRADEABLE, enchants)
                    addToTag(EnchantmentTagKeys.NON_TREASURE, enchants)
                    addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, enchants)
                }
            }
            registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.PAINTING_VARIANT)) { event ->
                event.registrar().addToTag(
                    PaintingVariantTagKeys.PLACEABLE,
                    YapettoPaintings.paintings.map { it.key },
                )
            }
        }
    }
}
