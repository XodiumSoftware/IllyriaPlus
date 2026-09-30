package org.xodium.illyrianpcs

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker
import org.xodium.illyrialib.Utils.validateServerVersion

/** Main class of the plugin. */
internal class IllyriaNPCs : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaNPCs
            private set
    }

    override fun onEnable() {
        if (!validateServerVersion()) return

        instance = this

        QuestNpcMechanic.register()

        UpdateChecker(this).check()
    }
}
