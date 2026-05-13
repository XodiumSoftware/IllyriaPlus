@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.xodium.illyriaplus

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.ItemTypeKeys
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import io.papermc.paper.registry.tag.TagKey
import io.papermc.paper.tag.TagEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.enchantments.spells.*
import org.xodium.illyriaplus.enchantments.utility.*

/** Main bootstrap class of the plugin. */
@Suppress("UnstableApiUsage", "Unused")
internal class IllyriaPlusBootstrap : PluginBootstrap {
    companion object {
        const val INSTANCE = "illyriaplus"

        val TOOLS = TagKey.create(RegistryKey.ITEM, Key.key(INSTANCE, "tools"))
        val WEAPONS = TagKey.create(RegistryKey.ITEM, Key.key(INSTANCE, "weapons"))
        val BLAZE_RODS = TagKey.create(RegistryKey.ITEM, Key.key(INSTANCE, "blaze_rods"))
        val TETHER_ITEMS = TagKey.create(RegistryKey.ITEM, Key.key(INSTANCE, "tether_items"))
    }

    override fun bootstrap(ctx: BootstrapContext) {
        ctx.lifecycleManager.apply {
            registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.ITEM)) {
                it.registrar().apply {
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
                        BLAZE_RODS,
                        setOf(
                            TagEntry.valueEntry(ItemTypeKeys.BLAZE_ROD),
                        ),
                    )
                    setTag(
                        TETHER_ITEMS,
                        setOf(
                            TagEntry.tagEntry(TOOLS),
                            TagEntry.tagEntry(WEAPONS),
                            TagEntry.tagEntry(BLAZE_RODS),
                        ),
                    )
                }
            }
            registerEventHandler(
                RegistryEvents.ENCHANTMENT.compose().newHandler { event ->
                    event.registry().apply {
                        register(VerdanceEnchantment.key) {
                            VerdanceEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.HOES))
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
                        register(EarthrendEnchantment.key) {
                            EarthrendEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.PICKAXES))
                        }
                        register(EmbertreadEnchantment.key) {
                            EmbertreadEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.FOOT_ARMOR))
                        }
                        register(InfernoEnchantment.key) {
                            InfernoEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(BLAZE_RODS))
                        }
                        register(SkysunderEnchantment.key) {
                            SkysunderEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(BLAZE_RODS))
                        }
                        register(WitherbrandEnchantment.key) {
                            WitherbrandEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(BLAZE_RODS))
                        }
                        register(FrostbindEnchantment.key) {
                            FrostbindEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(BLAZE_RODS))
                        }
                        register(TempestEnchantment.key) {
                            TempestEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(BLAZE_RODS))
                        }
                        register(VoidpullEnchantment.key) {
                            VoidpullEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(BLAZE_RODS))
                        }
                        register(QuakeEnchantment.key) {
                            QuakeEnchantment
                                .invoke(it)
                                .supportedItems(event.getOrCreateTag(BLAZE_RODS))
                        }
                    }
                },
            )
            registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT)) {
                it.registrar().apply {
                    val enchants =
                        setOf(
                            VerdanceEnchantment.key,
                            TetherEnchantment.key,
                            NimbusEnchantment.key,
                            EarthrendEnchantment.key,
                            EmbertreadEnchantment.key,
                            InfernoEnchantment.key,
                            SkysunderEnchantment.key,
                            WitherbrandEnchantment.key,
                            FrostbindEnchantment.key,
                            TempestEnchantment.key,
                            VoidpullEnchantment.key,
                            QuakeEnchantment.key,
                        )

                    addToTag(EnchantmentTagKeys.TRADEABLE, enchants)
                    addToTag(EnchantmentTagKeys.NON_TREASURE, enchants)
                    addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, enchants)
                }
            }
        }
    }
}
