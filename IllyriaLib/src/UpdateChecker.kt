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

    /** Registers the update download command. */
    fun register(plugin: JavaPlugin) {
        val name = plugin.name
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                literal<CommandSourceStack>("${name.lowercase()}-$UPDATE_COMMAND")
                    .executes { ctx ->
                        ctx.source.sender.sendMessage(
                            MM.deserialize(
                                "<mango>[</gradient><firewatch>$name</gradient><mango>]</gradient> " +
                                    "<yellow>Downloading update...</yellow>",
                            ),
                        )
                        downloadUpdate(plugin, name) { success, fileName ->
                            ctx.source.sender.sendMessage(
                                if (success) {
                                    MM.deserialize(
                                        "<mango>[</gradient><firewatch>$name</gradient><mango>]</gradient> " +
                                            "<green>Successfully downloaded $fileName.</green> " +
                                            "<gray>Restart the server to apply.</gray>",
                                    )
                                } else {
                                    MM.deserialize(
                                        "<mango>[</gradient><firewatch>$name</gradient><mango>]</gradient> " +
                                            "<red>Failed to download update. Check console for details.</red>",
                                    )
                                },
                            )
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
     * @param plugin The plugin instance for scheduling and file resolution
     * @param name The plugin simple name used to match the release asset
     * @param callback Invoked with `true` and the downloaded file name on success, `false` on failure.
     */
    private fun downloadUpdate(
        plugin: JavaPlugin,
        name: String,
        callback: (Boolean, String) -> Unit,
    ) {
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                try {
                    val assetUrl = fetchLatestAssetUrl(name) ?: throw IllegalStateException("No release found")
                    val response = httpClient.send(get(assetUrl), HttpResponse.BodyHandlers.ofByteArray())

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

                    val jarName = assetUrl.substringAfterLast('/')
                    updateDir.resolve(jarName).writeBytes(response.body())

                    plugin.logger.info("Downloaded $jarName to plugins/update/")
                    callback(true, jarName)
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to download update: ${e.message}")
                    callback(false, "")
                }
            },
        )
    }

    /**
     * Fetches the download URL for the latest plugin JAR from the nightly release assets.
     *
     * @param name The plugin simple name used to match the release asset
     * @return The asset download URL, or `null` if not found
     */
    private fun fetchLatestAssetUrl(name: String): String? {
        val response = httpClient.send(get(RELEASE_URL), HttpResponse.BodyHandlers.ofString())
        val assets = JsonParser.parseString(response.body()).asJsonObject.getAsJsonArray("assets") ?: return null

        return assets
            .map { it.asJsonObject }
            .firstOrNull {
                val assetName = it.get("name").asString
                assetName.startsWith("$name-") && assetName.endsWith(ASSET_SUFFIX)
            }?.get("browser_download_url")
            ?.asString
    }

    /** Builds a GET request with the GitHub Accept header. */
    private fun get(url: String): HttpRequest =
        HttpRequest
            .newBuilder(URI.create(url))
            .header("Accept", "application/vnd.github+json")
            .build()
}
