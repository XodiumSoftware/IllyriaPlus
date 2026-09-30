package org.xodium.illyriaquests

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker

/** Main class of the plugin. */
internal class IllyriaQuests : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaQuests
            private set
    }

    override fun onEnable() {
        instance = this

        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe("This plugin requires the following supported version: ${pluginMeta.version}.")
            server.pluginManager.disablePlugin(this)
            return
        }

        UpdateChecker(this).check()
    }
}
