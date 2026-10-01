@file:Suppress("Unused")

package org.xodium.illyrialib

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import org.bukkit.plugin.java.JavaPlugin

/** General utilities shared across the IllyriaPlus plugin modules. */
object Utils {
    /** MiniMessage instance for parsing formatted strings with custom gradient aliases. */
    val MM: MiniMessage =
        MiniMessage
            .builder()
            .editTags {
                listOf(
                    "mango" to "#FFE259:#FFA751",
                    "mango_r" to "#FFA751:#FFE259",
                    "firewatch" to "#CB2D3E:#EF473A",
                    "skyline" to "#1488CC:#2B32B2",
                    "deep-ocean" to "#13547a:#80d0c7",
                    "rose" to "#F4C4F3:#FC67FA",
                ).forEach { (name, colors) -> it.tag(name, Tag.preProcessParsed("<gradient:$colors>")) }
            }.build()

    /**
     * Validates that the server is running the expected Minecraft version.
     *
     * Compares the server version string against the plugin's declared version.
     * If they don't match, the plugin is disabled and a severe error is logged.
     *
     * @return `true` if the version check passed, `false` if the plugin was disabled.
     */
    fun JavaPlugin.validateServerVersion(): Boolean {
        val expected = pluginMeta.version.substringBefore("+")
        if (server.version.contains(expected)) return true
        logger.severe("This plugin requires the following supported version: ${pluginMeta.version}.")
        server.pluginManager.disablePlugin(this)
        return false
    }
}
