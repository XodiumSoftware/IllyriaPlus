package org.xodium.illyriakingdoms

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyriakingdoms.data.DatabaseManager
import org.xodium.illyriakingdoms.gui.KingdomGui
import org.xodium.illyriakingdoms.gui.MemberGui
import org.xodium.illyriakingdoms.mechanics.MechanicInterface
import org.xodium.illyriakingdoms.mechanics.server.KingdomMechanic

/** Main class of the plugin. */
internal class IllyriaKingdoms : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaKingdoms
            private set

        /** The ID of the main class */
        val ID = IllyriaKingdoms::class.java.simpleName.lowercase()
    }

    lateinit var mechanics: List<MechanicInterface>
        private set

    override fun onEnable() {
        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe(
                "This plugin requires the following supported version: ${pluginMeta.version}.",
            )
            server.pluginManager.disablePlugin(this)
            return
        }

        instance = this
        DatabaseManager.init(this)

        mechanics =
            listOf(
                KingdomMechanic,
            )

        logger.info(
            "Registered: ${mechanics.size} mechanic(s) | Took ${mechanics.sumOf { it.register() }}ms",
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
        DatabaseManager.close()
        KingdomGui.closeAll()
        MemberGui.cancelCallTasks()
    }
}
