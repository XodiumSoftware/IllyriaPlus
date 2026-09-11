package org.xodium.illyriabridge

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyriabridge.bridges.BridgeInterface
import org.xodium.illyriabridge.bridges.FabricRecipeBridge
import org.xodium.illyriabridge.bridges.XaeroMapBridge

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
                FabricRecipeBridge,
                XaeroMapBridge,
            )

        logger.info(
            "Registered: ${bridges.size} bridge(s) | Took ${bridges.sumOf { it.register() }}ms",
        )
    }

    override fun onDisable() {
        server.messenger.unregisterOutgoingPluginChannel(this)
    }
}
