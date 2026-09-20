package org.xodium.illyriabridge

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyriabridge.bridges.AppleSkinBridge
import org.xodium.illyriabridge.bridges.BridgeInterface
import org.xodium.illyriabridge.bridges.FabricRecipeBridge
import org.xodium.illyriabridge.bridges.NameplateBridge
import org.xodium.illyriabridge.bridges.XaeroMapBridge
import org.xodium.illyriabridge.bridges.jade.JadeBridge

/** Main class of the plugin. */
internal class IllyriaBridge : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaBridge
            private set
    }

    lateinit var bridges: List<BridgeInterface>
        private set

    override fun onEnable() {
        instance = this

        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe("This plugin requires the following supported version: ${pluginMeta.version}.")
            server.pluginManager.disablePlugin(this)
        }

        bridges =
            listOf(
                AppleSkinBridge,
                FabricRecipeBridge,
                JadeBridge,
                NameplateBridge,
                XaeroMapBridge,
            )

        logger.info(
            "Registered: ${bridges.size} bridge(s) | Took ${bridges.sumOf { it.register() }}ms",
        )

        UpdateChecker.check()
    }

    override fun onDisable() {
        server.messenger.unregisterIncomingPluginChannel(this)
        server.messenger.unregisterOutgoingPluginChannel(this)
    }
}
