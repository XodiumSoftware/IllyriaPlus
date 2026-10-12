package org.xodium.illyrialib

import com.google.gson.JsonParser
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import org.xodium.illyrialib.Utils.MM
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

/**
 * Checks GitHub nightly releases for newer builds of a plugin.
 */
object UpdateChecker {
    private const val REPO = "XodiumSoftware/IllyriaPlus"
    private const val RELEASE_URL = "https://api.github.com/repos/$REPO/releases/tags/nightly"
    private const val ASSET_SUFFIX = ".jar"
    private const val UPDATE_COMMAND = "update"

    private val httpClient: HttpClient =
        HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

    /** Registers the update command. */
    fun register(plugin: JavaPlugin) {
        registerCommand(plugin)
    }


    /**
     * Fetches the latest version string from the nightly release assets.
     *
     * @return The version string (e.g. `26.2+build.881`), or `null` if not found.
     */
    private fun fetchLatestVersion(plugin: JavaPlugin): String? {
        val name = plugin::class.simpleName ?: "Unknown"
        val request =
            HttpRequest
                .newBuilder(URI.create(RELEASE_URL))
                .header("Accept", "application/vnd.github+json")
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val root = JsonParser.parseString(response.body()).asJsonObject
        val assets = root.getAsJsonArray("assets") ?: return null

        for (asset in assets) {
            val assetName = asset.asJsonObject.get("name").asString
            if (assetName.startsWith("$name-") && assetName.endsWith(ASSET_SUFFIX)) {
                return assetName.removePrefix("$name-").removeSuffix(ASSET_SUFFIX)
            }
        }
        return null
    }

    /** Registers the update download command. */
    private fun registerCommand(plugin: JavaPlugin) {
        val name = plugin::class.simpleName ?: "Unknown"
        val commandName = "${name.lowercase()}-$UPDATE_COMMAND"
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                literal<CommandSourceStack>(commandName)
                    .executes { ctx ->
                        ctx.source.sender.sendMessage(
                            MM.deserialize(
                                "<mango>[</gradient><firewatch>$name</gradient><mango>]</gradient> " +
                                    "<yellow>Downloading update...</yellow>",
                            ),
                        )
                        downloadUpdate(plugin) { success, version ->
                            if (success) {
                                ctx.source.sender.sendMessage(
                                    MM.deserialize(
                                        "<mango>[</gradient><firewatch>$name</gradient><mango>]</gradient> " +
                                            "<green>Successfully downloaded $version.</green> " +
                                            "<gray>Restart the server to apply.</gray>",
                                    ),
                                )
                            } else {
                                ctx.source.sender.sendMessage(
                                    MM.deserialize(
                                        "<mango>[</gradient><firewatch>$name</gradient><mango>]</gradient> " +
                                            "<red>Failed to download update. Check console for details.</red>",
                                    ),
                                )
                            }
                        }
                        Command.SINGLE_SUCCESS
                    }.build(),
                "Downloads the latest $name nightly build",
            )
        }
    }

    /**
     * Downloads the latest plugin JAR from the GitHub nightly release into the server's plugins/update folder.
     *
     * @param callback Invoked with `true` and the downloaded version on success, `false` on failure.
     */
    private fun downloadUpdate(
        plugin: JavaPlugin,
        callback: (Boolean, String) -> Unit,
    ) {
        val name = plugin::class.simpleName ?: "Unknown"
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                try {
                    val version = fetchLatestVersion(plugin) ?: throw IllegalStateException("No release found")
                    val assetUrl =
                        "https://github.com/$REPO/releases/download/nightly/$name-$version$ASSET_SUFFIX"
                    val request =
                        HttpRequest
                            .newBuilder(URI.create(assetUrl))
                            .header("Accept", "application/vnd.github+json")
                            .build()
                    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())

                    if (response.statusCode() != 200) {
                        throw IllegalStateException("Download failed with status ${response.statusCode()}")
                    }

                    val updateDir =
                        plugin
                            .dataFolder
                            .parentFile
                            .toPath()
                            .resolve("update")
                    updateDir.createDirectories()

                    val jarFile = updateDir.resolve("$name-$version.jar")
                    jarFile.writeBytes(response.body())

                    plugin.logger.info("Downloaded $name-$version.jar to plugins/update/")
                    callback(true, version)
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to download update: ${e.message}")
                    callback(false, "")
                }
            },
        )
    }

    /**
     * Compares the current version with the latest version, determining if the latest is newer.
     * Extraction is done by grabbing the numeric build number after `+build.`.
     *
     * @param current The currently installed version string
     * @param latest The latest available version string
     * @return `true` if the latest build number is greater than the current build number
     */
    private fun isNewer(
        current: String,
        latest: String,
    ): Boolean {
        val currentBuild = extractBuildNumber(current)
        val latestBuild = extractBuildNumber(latest)
        return latestBuild > currentBuild
    }

    /**
     * Extracts the numeric build number from a version string like `26.2+build.881`.
     *
     * @param version The version string to parse
     * @return The build number, or `0` if it cannot be parsed
     */
    private fun extractBuildNumber(version: String): Int = version.substringAfter("+build.").toIntOrNull() ?: 0
}
