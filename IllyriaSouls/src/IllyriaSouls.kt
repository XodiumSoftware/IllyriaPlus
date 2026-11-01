package org.xodium.illyriasouls

import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker
import org.xodium.illyrialib.Utils.validateVersion
import org.xodium.illyriasouls.mechanics.BonfireMechanic
import org.xodium.illyriasouls.mechanics.DeathStainMechanic
import org.xodium.illyriasouls.mechanics.MechanicInterface
import org.xodium.illyriasouls.mechanics.NaturalRegenMechanic
import org.xodium.illyriasouls.recipes.FlaskRecipe
import org.xodium.illyriasouls.recipes.RecipeInterface

/** Main class of the IllyriaSouls plugin. */
internal class IllyriaSouls : JavaPlugin() {
    companion object {
        lateinit var instance: IllyriaSouls
            private set

        /** The ID of the main class */
        val ID = IllyriaSouls::class.java.simpleName.lowercase()
    }

    lateinit var recipes: List<RecipeInterface>
        private set
    lateinit var mechanics: List<MechanicInterface>
        private set

    override fun onEnable() {
        validateVersion()

        instance = this

        recipes =
            listOf(
                FlaskRecipe,
            )

        logger.info(
            "Registered: ${recipes.sumOf { it.recipes.size }} recipe(s) |" +
                " Took ${recipes.sumOf { it.register() }}ms",
        )

        mechanics =
            listOf(
                BonfireMechanic,
                DeathStainMechanic,
                NaturalRegenMechanic,
            )

        logger.info(
            "Registered: ${mechanics.size} mechanic(s) | Took ${mechanics.sumOf { it.register() }}ms",
        )

        UpdateChecker.register(this)
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
    }
}
