package org.xodium.illyriaplus

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.ItemTypeKeys
import io.papermc.paper.registry.keys.tags.BannerPatternTagKeys
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys
import io.papermc.paper.registry.keys.tags.PaintingVariantTagKeys
import io.papermc.paper.registry.tag.TagKey
import io.papermc.paper.tag.TagEntry
import net.kyori.adventure.key.Key
import org.xodium.illyriaplus.banners.MoxvallixBanners
import org.xodium.illyriaplus.damagetypes.AlcoholDamageType
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

        private val BANNERS = MoxvallixBanners.banners
        private val DAMAGE_TYPES =
            setOf(
                AlcoholDamageType.key,
            )
        private val ENCHANTMENTS =
            setOf(
                VinemineEnchantment.key,
                TetherEnchantment.key,
                NimbusEnchantment.key,
                EmbertreadEnchantment.key,
            )
        private val PAINTINGS = YapettoPaintings.paintings
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
            ctx.logger.info("Registered: ${ENCHANTMENTS.size} enchantment(s).")

//            registerEventHandler(
//                RegistryEvents.ENCHANTMENT.entryAdd().newHandler { event ->
//                    if (event.key().key().namespace() != "minecraft") return@newHandler
//
//                    event.builder().apply { maxLevel(maxLevel().takeIf { it > 1 }?.times(2) ?: maxLevel()) }
//                },
//            )
//
//            ctx.logger.info("Vanilla enchantments max levels doubled.")

            registerEventHandler(
                RegistryEvents.DAMAGE_TYPE.compose().newHandler { event ->
                    event.registry().apply {
                        DAMAGE_TYPES.forEach { damageType ->
                            register(damageType) { AlcoholDamageType.invoke(it) }
                        }
                    }
                },
            )
            ctx.logger.info("Registered: ${DAMAGE_TYPES.size} damage type(s).")

            registerEventHandler(
                RegistryEvents.PAINTING_VARIANT.compose().newHandler { event ->
                    event.registry().apply {
                        PAINTINGS.forEach { painting ->
                            register(YapettoPaintings.key(painting.name)) { YapettoPaintings.invoke(painting.name, it) }
                        }
                    }
                },
            )
            ctx.logger.info("Registered: ${PAINTINGS.size} painting variant(s).")

            registerEventHandler(
                RegistryEvents.BANNER_PATTERN.compose().newHandler { event ->
                    event.registry().apply {
                        BANNERS.forEach { banner ->
                            register(MoxvallixBanners.key(banner.name)) { MoxvallixBanners.invoke(banner.name, it) }
                        }
                    }
                },
            )
            ctx.logger.info("Registered: ${BANNERS.size} banner pattern(s).")

            registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT)) { event ->
                event.registrar().apply {
                    addToTag(EnchantmentTagKeys.TRADEABLE, ENCHANTMENTS)
                    addToTag(EnchantmentTagKeys.NON_TREASURE, ENCHANTMENTS)
                    addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, ENCHANTMENTS)
                }
            }

            registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.PAINTING_VARIANT)) { event ->
                event.registrar().addToTag(
                    PaintingVariantTagKeys.PLACEABLE,
                    PAINTINGS.map { YapettoPaintings.key(it.name) },
                )
            }

            registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.BANNER_PATTERN)) { event ->
                event.registrar().apply {
                    addToTag(
                        BannerPatternTagKeys.NO_ITEM_REQUIRED,
                        listOf(
                            MoxvallixBanners.key("chequered"),
                            MoxvallixBanners.key("circle_tiles"),
                            MoxvallixBanners.key("cogs"),
                            MoxvallixBanners.key("curtains"),
                            MoxvallixBanners.key("double_bars"),
                            MoxvallixBanners.key("double_gradient"),
                            MoxvallixBanners.key("fancy"),
                            MoxvallixBanners.key("tattered"),
                        ),
                    )
                    addToTag(
                        BannerPatternTagKeys.PATTERN_ITEM_CREEPER,
                        listOf(
                            MoxvallixBanners.key("blam"),
                            MoxvallixBanners.key("ribs"),
                            MoxvallixBanners.key("pillager"),
                            MoxvallixBanners.key("villager"),
                            MoxvallixBanners.key("ghast"),
                        ),
                    )
                    addToTag(
                        BannerPatternTagKeys.PATTERN_ITEM_FLOWER,
                        listOf(
                            MoxvallixBanners.key("moon"),
                            MoxvallixBanners.key("peace"),
                            MoxvallixBanners.key("sun"),
                            MoxvallixBanners.key("yin_yang"),
                            MoxvallixBanners.key("knot"),
                        ),
                    )
                    addToTag(
                        BannerPatternTagKeys.PATTERN_ITEM_GLOBE,
                        listOf(
                            MoxvallixBanners.key("pumpkin"),
                            MoxvallixBanners.key("horn"),
                        ),
                    )
                    addToTag(
                        BannerPatternTagKeys.PATTERN_ITEM_MOJANG,
                        listOf(
                            MoxvallixBanners.key("clubs"),
                            MoxvallixBanners.key("diamonds"),
                            MoxvallixBanners.key("hearts"),
                            MoxvallixBanners.key("spades"),
                            MoxvallixBanners.key("anchor"),
                            MoxvallixBanners.key("eye"),
                            MoxvallixBanners.key("companion"),
                            MoxvallixBanners.key("revolution"),
                            MoxvallixBanners.key("emoji"),
                        ),
                    )
                    addToTag(
                        BannerPatternTagKeys.PATTERN_ITEM_PIGLIN,
                        listOf(
                            MoxvallixBanners.key("castle"),
                            MoxvallixBanners.key("palace"),
                            MoxvallixBanners.key("pyramid"),
                            MoxvallixBanners.key("tower"),
                        ),
                    )
                    addToTag(
                        BannerPatternTagKeys.PATTERN_ITEM_SKULL,
                        listOf(
                            MoxvallixBanners.key("crown"),
                            MoxvallixBanners.key("hammer"),
                            MoxvallixBanners.key("shield"),
                            MoxvallixBanners.key("sword"),
                            MoxvallixBanners.key("trident"),
                        ),
                    )
                }
            }
        }
    }
}
