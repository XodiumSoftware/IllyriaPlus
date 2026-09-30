package org.xodium.illyriaquests

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.UpdateChecker
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyriaquests.Utils.Toast
import org.xodium.illyriaquests.Utils.Toast.showToast

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

        registerTestCommand()

        UpdateChecker(this).check()
    }

    @Suppress("UnstableApiUsage")
    private fun registerTestCommand() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                Commands
                    .literal("toast")
                    .requires { it.sender is Player }
                    .executes { ctx ->
                        val player = ctx.source.sender as Player
                        player.showToast(
                            title = MM.deserialize("<gold>Quest Complete!"),
                            description = MM.deserialize("Slay 10 zombies"),
                            icon = ItemStack.of(Material.DIAMOND_SWORD),
                            frame = Toast.Frame.CHALLENGE,
                        )
                        Command.SINGLE_SUCCESS
                    }.build(),
                "Test the quest toast notification",
                listOf("testtoast"),
            )
        }
    }
}
