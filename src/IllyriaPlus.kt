package org.xodium.illyriaplus

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyriaplus.enchantments.spells.*
import org.xodium.illyriaplus.enchantments.utility.*
import org.xodium.illyriaplus.enchantments.vanilla.FeatherFallingEnchantment
import org.xodium.illyriaplus.enchantments.vanilla.SilkTouchEnchantment
import org.xodium.illyriaplus.guis.FaqGui
import org.xodium.illyriaplus.mechanics.entity.*
import org.xodium.illyriaplus.mechanics.player.*
import org.xodium.illyriaplus.mechanics.server.*
import org.xodium.illyriaplus.mechanics.world.*
import org.xodium.illyriaplus.recipes.*

/** Main class of the plugin. */
internal class IllyriaPlus : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaPlus
            private set

        /** The standardized prefix for IllyriaPlus messages. */
        val IllyriaPlus.prefix: String
            get() = "<mango_r>[</gradient><firewatch>${this.javaClass.simpleName}</gradient><mango>]</gradient>"
    }

    override fun onEnable() {
        instance = this

        val unsupportedVersionMsg =
            "This plugin requires a supported server version. Supported versions: ${pluginMeta.version}."

        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) disablePlugin(unsupportedVersionMsg)

        val recipes =
            listOf(
                ChainmailRecipe,
                DiamondRecycleRecipe,
                PaintingRecipe,
                RottenFleshRecipe,
                WoodLogRecipe,
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
                HuskMechanic,
                ZombieMechanic,
                SkeletonMechanic,
                SilenceMechanic,
                HeadMechanic,
                ChatMechanic,
                InventoryMechanic,
                SitMechanic,
                ChiseledBookshelfMechanic,
                DimensionMechanic,
                MushroomMechanic,
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
            "Registered: ${mechanics.size} module(s) | Took ${mechanics.sumOf { it.register() }}ms",
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

        FaqGui.mechanics = mechanics

        val guis =
            listOf(
                FaqGui,
            )

        logger.info(
            "Registered: ${guis.size} gui's | Took ${guis.sumOf { it.register() }}ms",
        )
    }

    /**
     * Disable the plugin and log the message.
     *
     * @param msg The message to log.
     */
    private fun disablePlugin(msg: String): Nothing {
        logger.severe(msg)
        server.pluginManager.disablePlugin(instance)
        throw IllegalStateException(msg)
    }
}
