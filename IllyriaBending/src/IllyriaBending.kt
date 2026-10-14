package org.xodium.illyriabending

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain
import com.fren_gor.ultimateAdvancementAPI.database.impl.SQLite
import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyriabending.advancements.BendingAdvancement
import org.xodium.illyriabending.mechanics.BendingMechanic
import org.xodium.illyrialib.Utils.validateVersion
import java.io.File

/** Main class of the plugin. */
internal class IllyriaBending : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaBending
            private set

        /** The ID of the main class */
        val ID = IllyriaBending::class.java.simpleName.lowercase()
    }

    lateinit var mechanics: List<MechanicInterface>
        private set
    lateinit var advancementMain: AdvancementMain
        private set

    override fun onLoad() {
        advancementMain = AdvancementMain(this)
        advancementMain.load()
    }

    override fun onEnable() {
        validateVersion()

        instance = this

        advancementMain.enable { SQLite(advancementMain, File(dataFolder, "advancements.db")) }
        BendingAdvancement.register()

        mechanics =
            listOf(
                BendingMechanic,
            )

        logger.info(
            "Registered: ${mechanics.size} mechanic(s) | Took ${mechanics.sumOf { it.register() }}ms",
        )
    }

    override fun onDisable() {
        if (::advancementMain.isInitialized) {
            runCatching { advancementMain.disable() }
                .onFailure { logger.warning("Failed to disable UltimateAdvancementAPI: ${it.message}") }
        }
        if (::mechanics.isInitialized) {
            mechanics.forEach { mechanic ->
                runCatching { mechanic.onDisable() }
                    .onFailure {
                        logger.warning("Failed to disable ${mechanic::class.simpleName}: ${it.message}")
                    }
            }
        }
    }
}
