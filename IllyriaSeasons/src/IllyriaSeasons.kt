package org.xodium.illyriaseasons

import dev.wyck.renderer.packet.PacketHandler
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker
import org.xodium.illyrialib.Utils.validateVersion

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

        validateVersion()

        packetHandler =
            PacketHandler.of(this, PacketHandler.Injector.NETTY).also {
                it.register()
                logger.info("Wyck packet handler registered (Netty injector)")
            }

        SeasonBiomes.register()
        SeasonTracker.start()

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            val (n, d, a) = SeasonCommand()
            it.registrar().register(n.build(), d, a)
        }

        UpdateChecker(this).check()
    }

    override fun onDisable() {
        packetHandler.unregister()
        SeasonTracker.stop()
    }
}
