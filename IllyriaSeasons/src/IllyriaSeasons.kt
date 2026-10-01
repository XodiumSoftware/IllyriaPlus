package org.xodium.illyriaseasons

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker

/** Main class of the plugin. */
internal class IllyriaSeasons : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaSeasons
            private set
    }

    override fun onEnable() {
        instance = this

        if (!server.version.contains(pluginMeta.version.substringBefore("+"))) {
            logger.severe("This plugin requires the following supported version: ${pluginMeta.version}.")
            server.pluginManager.disablePlugin(this)
            return
        }

        SeasonSystem.load()
        SeasonAdvancementMechanic.start()
        SeasonCommand.register(this)

        UpdateChecker(this).check()
    }

    override fun onDisable() {
        SeasonAdvancementMechanic.stop()
        SeasonSystem.save()
    }
}
