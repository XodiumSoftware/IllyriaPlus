package org.xodium.illyriamannequins

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker
import org.xodium.illyrialib.Utils.validateServerVersion

/** Main class of the plugin. */
internal class IllyriaMannequins : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaMannequins
            private set
    }

    override fun onEnable() {
        if (!validateServerVersion()) return

        instance = this

        MannequinMechanic.register()

        UpdateChecker(this).check()
    }
}
