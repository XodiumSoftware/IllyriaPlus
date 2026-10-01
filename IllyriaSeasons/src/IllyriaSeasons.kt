package org.xodium.illyriaseasons

import dev.wyck.renderer.packet.PacketHandler
import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker

/** Main class of the plugin. */
internal class IllyriaSeasons : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaSeasons
            private set
        const val NAMESPACE = "illyriaseasons"
    }

    /** Wyck packet handler for injecting virtual seasonal biomes. */
    lateinit var packetHandler: PacketHandler
        private set

    override fun onEnable() {
        instance = this

        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe("This plugin requires the following supported version: ${pluginMeta.version}.")
            server.pluginManager.disablePlugin(this)
            return
        }

        packetHandler =
            PacketHandler.of(this, PacketHandler.Injector.NETTY).also {
                it.register()
                logger.info("Wyck packet handler registered (Netty injector)")
            }

        SeasonBiomes.register()
        SeasonCommand.register(this)

        UpdateChecker(this).check()
    }

    override fun onDisable() {
        packetHandler.unregister()
    }
}
