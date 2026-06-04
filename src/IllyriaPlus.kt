package org.xodium.illyriaplus

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyriaplus.enchantments.spells.*
import org.xodium.illyriaplus.enchantments.utility.*
import org.xodium.illyriaplus.enchantments.vanilla.FeatherFallingEnchantment
import org.xodium.illyriaplus.enchantments.vanilla.SilkTouchEnchantment
import org.xodium.illyriaplus.guis.FaqGui
import org.xodium.illyriaplus.mechanics.entity.*
import org.xodium.illyriaplus.mechanics.entity.monster.*
import org.xodium.illyriaplus.mechanics.player.*
import org.xodium.illyriaplus.mechanics.server.*
import org.xodium.illyriaplus.mechanics.world.*
import org.xodium.illyriaplus.recipes.*

/** Main class of the plugin. */
internal class IllyriaPlus : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaPlus
            private set

        /** The ID of the main class */
        val ID = this.javaClass.simpleName.lowercase()
    }

    override fun onEnable() {
        instance = this

        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe("This plugin requires the following supported version: ${pluginMeta.version}.")
            server.pluginManager.disablePlugin(this)
        }

        val recipes =
            listOf(
                ChainmailRecipe,
                DiamondRecycleRecipe,
                IceBreakdownRecipe,
                NetherWartBlockRecipe,
                PaintingRecipe,
                RottenFleshRecipe,
                WoodLogRecipe,
                WoolToStringRecipe,
            )

        logger.info(
            "Registered: ${recipes.sumOf { it.recipes.size }} recipes(s) | Took ${recipes.sumOf { it.register() }}ms",
        )

        val mechanics =
            listOf(
                RulesMechanic,
                NicknameMechanic,
                ScoreBoardMechanic,
                LocatorMechanic,
                OpenableMechanic,
                TameableMechanic,
                EnderchestMechanic,
                XpMechanic,
                MonsterMechanic,
                HuskMechanic,
                ZombieMechanic,
                AbstractSkeletonMechanic,
                CreeperMechanic,
                SilenceMechanic,
                HeadMechanic,
                ChatMechanic,
                InventoryMechanic,
                SitMechanic,
                ChiseledBookshelfMechanic,
                DimensionMechanic,
                MushroomMechanic,
                TreeMechanic,
                BatMechanic,
                SpawnEggMechanic,
                GriefingMechanic,
                MotdMechanic,
                MessagesMechanic,
                ServerInfoMechanic,
                SpellMechanic,
                TabListMechanic,
            )

        logger.info(
            "Registered: ${mechanics.size} mechanic(s) | Took ${mechanics.sumOf { it.register() }}ms",
        )

        val enchantments =
            listOf(
                EarthrendEnchantment,
                EmbertreadEnchantment,
                FeatherFallingEnchantment,
                FrostbindEnchantment,
                InfernoEnchantment,
                NimbusEnchantment,
                QuakeEnchantment,
                SilkTouchEnchantment,
                SkysunderEnchantment,
                TempestEnchantment,
                TetherEnchantment,
                VerdanceEnchantment,
                VoidpullEnchantment,
                WitherbrandEnchantment,
            )

        logger.info(
            "Registered: ${enchantments.size} enchantment events | Took ${enchantments.sumOf { it.register() }}ms",
        )

        FaqGui.recipes = recipes
        FaqGui.mechanics = mechanics

        val guis =
            listOf(
                FaqGui,
            )

        logger.info(
            "Registered: ${guis.size} gui's | Took ${guis.sumOf { it.register() }}ms",
        )
    }
}
