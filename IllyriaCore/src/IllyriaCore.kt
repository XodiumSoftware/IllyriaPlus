package org.xodium.illyriacore

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyriacore.enchantments.EnchantmentInterface
import org.xodium.illyriacore.enchantments.utility.EmbertreadEnchantment
import org.xodium.illyriacore.enchantments.utility.NimbusEnchantment
import org.xodium.illyriacore.enchantments.utility.TetherEnchantment
import org.xodium.illyriacore.enchantments.utility.VinemineEnchantment
import org.xodium.illyriacore.enchantments.vanilla.FeatherFallingEnchantment
import org.xodium.illyriacore.enchantments.vanilla.FortuneEnchantment
import org.xodium.illyriacore.enchantments.vanilla.SilkTouchEnchantment
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyriacore.mechanics.entity.*
import org.xodium.illyriacore.mechanics.player.*
import org.xodium.illyriacore.mechanics.server.*
import org.xodium.illyriacore.mechanics.world.*
import org.xodium.illyriacore.recipes.RecipeInterface
import org.xodium.illyriacore.recipes.custom.GreatswordRecipe
import org.xodium.illyriacore.recipes.custom.HalberdRecipe
import org.xodium.illyriacore.recipes.custom.LongswordRecipe
import org.xodium.illyriacore.recipes.vanilla.*

/** Main class of the plugin. */
internal class IllyriaCore : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaCore
            private set

        /** The ID of the main class */
        val ID = IllyriaCore::class.java.simpleName.lowercase()
    }

    lateinit var recipes: List<RecipeInterface>
        private set
    lateinit var mechanics: List<MechanicInterface>
        private set
    lateinit var enchantments: List<EnchantmentInterface>
        private set

    @Suppress("UnstableApiUsage")
    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings.reEncodeByDefault(false)
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe(
                "This plugin requires the following supported version: ${pluginMeta.version}.",
            )
            server.pluginManager.disablePlugin(this)
            return
        }

        instance = this

        PacketEvents.getAPI().init()

        recipes =
            listOf(
                ChainmailRecipe,
                DiamondRecycleRecipe,
                GreatswordRecipe,
                HalberdRecipe,
                IceBreakdownRecipe,
                LongswordRecipe,
                NetherWartBlockRecipe,
                PaintingRecipe,
                RottenFleshRecipe,
                WoodLogRecipe,
                WoolToStringRecipe,
            )

        logger.info(
            "Registered: ${recipes.sumOf { it.recipes.size }} recipe(s) " +
                "and ${recipes.sumOf { it.potions.size }} potion mix(es) |" +
                "Took ${recipes.sumOf { it.register() }}ms",
        )

        mechanics =
            listOf(
                NicknameMechanic,
                ScoreBoardMechanic,
                LocatorMechanic,
                OpenableMechanic,
                TameableMechanic,
                EnderchestMechanic,
                XpMechanic,
                AnvilMechanic,
                HuskMechanic,
                SilenceMechanic,
                HeadMechanic,
                ChatMechanic,
                InventoryMechanic,
                SitMechanic,
                ChiseledBookshelfMechanic,
                BlockPlacementMechanic,
                BatMechanic,
                SpawnEggMechanic,
                GriefingMechanic,
                MotdMechanic,
                MessagesMechanic,
                TabListMechanic,
                TreeMechanic,
                RulesMechanic,
                ResourcePackMechanic,
                WanderingTraderMechanic,
            )

        logger.info(
            "Registered: ${mechanics.size} mechanic(s) | Took ${mechanics.sumOf { it.register() }}ms",
        )

        enchantments =
            listOf(
                EmbertreadEnchantment,
                FeatherFallingEnchantment,
                FortuneEnchantment,
                NimbusEnchantment,
                SilkTouchEnchantment,
                TetherEnchantment,
                VinemineEnchantment,
            )

        logger.info(
            "Registered: ${enchantments.size} enchantment event(s) | Took ${
                enchantments.sumOf {
                    it.register()
                }
            }ms",
        )
    }

    override fun onDisable() {
        if (::mechanics.isInitialized) {
            mechanics.forEach { mechanic ->
                runCatching { mechanic.onDisable() }
                    .onFailure {
                        logger.warning("Failed to disable ${mechanic::class.simpleName}: ${it.message}")
                    }
            }
        }
        PacketEvents.getAPI().terminate()
    }
}
