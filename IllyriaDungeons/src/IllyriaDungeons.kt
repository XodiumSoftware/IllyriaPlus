package org.xodium.illyriadungeons

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker
import org.xodium.illyrialib.Utils.validateVersion

internal class IllyriaDungeons : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaDungeons
            private set
    }

    override fun onEnable() {
        instance = this

        validateVersion()

        UpdateChecker(this).check()
    }
}
