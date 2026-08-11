package org.xodium.illyriaplus

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyriaplus.enchantments.EnchantmentInterface
import org.xodium.illyriaplus.enchantments.utility.EmbertreadEnchantment
import org.xodium.illyriaplus.enchantments.utility.NimbusEnchantment
import org.xodium.illyriaplus.enchantments.utility.TetherEnchantment
import org.xodium.illyriaplus.enchantments.utility.VinemineEnchantment
import org.xodium.illyriaplus.enchantments.vanilla.FeatherFallingEnchantment
import org.xodium.illyriaplus.enchantments.vanilla.FortuneEnchantment
import org.xodium.illyriaplus.enchantments.vanilla.SilkTouchEnchantment
import org.xodium.illyriaplus.items.GreatswordItem
import org.xodium.illyriaplus.items.HalberdItem
import org.xodium.illyriaplus.items.LongswordItem
import org.xodium.illyriaplus.mechanics.MechanicInterface
import org.xodium.illyriaplus.mechanics.entity.*
import org.xodium.illyriaplus.mechanics.player.*
import org.xodium.illyriaplus.mechanics.server.*
import org.xodium.illyriaplus.mechanics.world.*
import org.xodium.illyriaplus.recipes.RecipeInterface
import org.xodium.illyriaplus.recipes.custom.AlcoholRecipe
import org.xodium.illyriaplus.recipes.custom.GreatswordRecipe
import org.xodium.illyriaplus.recipes.custom.HalberdRecipe
import org.xodium.illyriaplus.recipes.custom.LongswordRecipe
import org.xodium.illyriaplus.recipes.vanilla.*

/** Main class of the plugin. */
internal class IllyriaPlus : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaPlus
            private set

        /** The ID of the main class */
        val ID = IllyriaPlus::class.java.simpleName.lowercase()
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
            logger.severe("This plugin requires the following supported version: ${pluginMeta.version}.")
            server.pluginManager.disablePlugin(this)
            return
        }

        instance = this

        PacketEvents.getAPI().init()

        recipes =
            listOf(
                AlcoholRecipe,
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
                AlcoholMechanic,
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
                DimensionMechanic,
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
            )

        logger.info("Registered: ${mechanics.size} mechanic(s) | Took ${mechanics.sumOf { it.register() }}ms")

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
            "Registered: ${enchantments.size} enchantment event(s) | Took ${enchantments.sumOf { it.register() }}ms",
        )
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
    }
}
