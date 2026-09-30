package org.xodium.illyriaquests

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker
import org.xodium.illyrialib.Utils.validateServerVersion

/** Main class of the plugin. */
internal class IllyriaQuests : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaQuests
            private set
    }

    override fun onEnable() {
        if (!validateServerVersion()) return

        instance = this

        UpdateChecker(this).check()
    }
}
