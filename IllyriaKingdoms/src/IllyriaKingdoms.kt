package org.xodium.illyriakingdoms

import org.bukkit.plugin.java.JavaPlugin

/** Main class of the plugin. */
internal class IllyriaKingdoms : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaKingdoms
            private set

        /** The ID of the main class */
        val ID = IllyriaKingdoms::class.java.simpleName.lowercase()
    }

    override fun onEnable() {
        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe(
                "This plugin requires the following supported version: ${pluginMeta.version}.",
            )
            server.pluginManager.disablePlugin(this)
            return
        }

        instance = this
    }
}
