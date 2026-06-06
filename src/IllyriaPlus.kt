package org.xodium.illyriaplus

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyriaplus.enchantments.EnchantmentInterface
import org.xodium.illyriaplus.enchantments.spells.*
import org.xodium.illyriaplus.enchantments.utility.*
import org.xodium.illyriaplus.enchantments.vanilla.FeatherFallingEnchantment
import org.xodium.illyriaplus.enchantments.vanilla.SilkTouchEnchantment
import org.xodium.illyriaplus.mechanics.MechanicInterface
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

    lateinit var recipes: List<RecipeInterface>
        private set
    lateinit var mechanics: List<MechanicInterface>
        private set
    lateinit var enchantments: List<EnchantmentInterface>
        private set

    override fun onEnable() {
        instance = this

        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe("This plugin requires the following supported version: ${pluginMeta.version}.")
            server.pluginManager.disablePlugin(this)
        }

        recipes =
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

        mechanics =
            listOf(
                FaqMechanic,
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
                BatMechanic,
                SpawnEggMechanic,
                GriefingMechanic,
                MotdMechanic,
                MessagesMechanic,
                ServerInfoMechanic,
                SpellMechanic,
                TabListMechanic,
                ElytraSwapMechanic,
            )

        logger.info(
            "Registered: ${mechanics.size} module(s) | Took ${mechanics.sumOf { it.register() }}ms",
        )

        enchantments =
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
    }
}
