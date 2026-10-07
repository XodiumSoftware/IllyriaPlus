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
import org.xodium.illyriacore.recipes.*
import org.xodium.illyrialib.UpdateChecker
import org.xodium.illyrialib.Utils.validateVersion

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
        validateVersion()

        instance = this

        PacketEvents.getAPI().init()

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
            "Registered: ${recipes.sumOf { it.recipes.size }} recipe(s) |" +
                "Took ${recipes.sumOf { it.register() }}ms",
        )

        mechanics =
            listOf(
                NicknameMechanic,
                LocatorMechanic,
                OpenableMechanic,
                TameableMechanic,
                EnderchestMechanic,
                XpMechanic,
                AnvilMechanic,
                SilenceMechanic,
                HeadMechanic,
                ChatMechanic,
                InventoryMechanic,
                SpawnEggMechanic,
                GriefingMechanic,
                CondenseMechanic,
                MotdMechanic,
                MessagesMechanic,
                TabListMechanic,
                TreeMechanic,
                RulesMechanic,
                ResourcePackMechanic,
                SpawnProtectionMechanic,
                PortalMechanic,
                LootMechanic,
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

        UpdateChecker(this).check()
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
