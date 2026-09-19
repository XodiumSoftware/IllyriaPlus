package org.xodium.illyriacore

import com.google.gson.JsonParser
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

/** Checks GitHub nightly releases for newer builds of this plugin. */
@Suppress("UnstableApiUsage")
internal object UpdateChecker {
    private const val REPO = "XodiumSoftware/IllyriaPlus"
    private const val RELEASE_URL = "https://api.github.com/repos/$REPO/releases/tags/nightly"
    private const val ASSET_SUFFIX = ".jar"
    private const val UPDATE_COMMAND = "update"

    private val name: String get() = instance::class.simpleName ?: "Unknown"
    private val commandName: String get() = "${name.lowercase()}-$UPDATE_COMMAND"
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    /** Checks asynchronously whether a newer build is available and logs the result. */
    fun check() {
        registerCommand()

        val currentVersion = instance.pluginMeta.version
        instance.server.scheduler.runTaskAsynchronously(
            instance,
            Runnable {
                try {
                    val latestVersion = fetchLatestVersion() ?: return@Runnable
                    if (isNewer(currentVersion, latestVersion)) {
                        instance.logger.warning(
                            "A new nightly build is available: $latestVersion (current: $currentVersion). " +
                                "Download: https://github.com/$REPO/releases/tag/nightly",
                        )
                        notifyOps(latestVersion, currentVersion)
                    } else {
                        instance.logger.info("No newer nightly build available (current: $currentVersion).")
                    }
                } catch (e: Exception) {
                    instance.logger.warning("Failed to check for updates: ${e.message}")
                }
            },
        )
    }

    /**
     * Fetches the latest version string from the nightly release assets.
     *
     * @return The version string (e.g. `26.2+build.881`), or `null` if not found.
     */
    private fun fetchLatestVersion(): String? {
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

    /**
     * Sends a chat notification to all online OP players about a newer available build.
     *
     * @param latest The latest available version string
     * @param current The currently installed version string
     */
    private fun notifyOps(
        latest: String,
        current: String,
    ) {
        val component =
            Utils.MM.deserialize(
                "<mango>[</mango><firewatch>$name</firewatch><mango>]</mango> <yellow>Update available:</yellow> " +
                    "<green>$latest</green> <gray>(current: $current)</gray> " +
                    "<click:run_command:'/$commandName'>" +
                    "<mango>[<b>Update Now</b>]</mango></click>",
            )
        instance.server.scheduler.runTask(
            instance,
            Runnable {
                instance
                    .server
                    .onlinePlayers
                    .filter { it.isOp }
                    .forEach { it.sendMessage(component) }
            },
        )
    }

    /** Registers the update download command. */
    private fun registerCommand() {
        instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                literal<CommandSourceStack>(commandName).build(),
            )
        }
        instance.server.commandMap.register(
            name.lowercase(),
            object : Command(commandName) {
                init {
                    description = "Downloads the latest $name nightly build"
                    usage = "/$commandName"
                }

                override fun execute(
                    sender: CommandSender,
                    commandLabel: String,
                    args: Array<out String>,
                ): Boolean {
                    sender.sendMessage(
                        Utils.MM.deserialize(
                            "<mango>[</mango><firewatch>$name</firewatch><mango>]</mango> " +
                                "<yellow>Downloading update...</yellow>",
                        ),
                    )
                    downloadUpdate { success, version ->
                        if (success) {
                            sender.sendMessage(
                                Utils.MM.deserialize(
                                    "<mango>[</mango><firewatch>$name</firewatch><mango>]</mango> " +
                                        "<green>Successfully downloaded $version.</green> " +
                                        "<gray>Restart the server to apply.</gray>",
                                ),
                            )
                        } else {
                            sender.sendMessage(
                                Utils.MM.deserialize(
                                    "<mango>[</mango><firewatch>$name</firewatch><mango>]</mango> " +
                                        "<red>Failed to download update. Check console for details.</red>",
                                ),
                            )
                        }
                    }
                    return true
                }
            },
        )
    }

    /**
     * Downloads the latest plugin JAR from the GitHub nightly release into the server's plugins/update folder.
     *
     * @param callback Invoked with `true` and the downloaded version on success, `false` on failure.
     */
    private fun downloadUpdate(callback: (Boolean, String) -> Unit) {
        instance.server.scheduler.runTaskAsynchronously(
            instance,
            Runnable {
                try {
                    val version = fetchLatestVersion() ?: throw IllegalStateException("No release found")
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
                        instance
                            .dataFolder
                            .parentFile
                            .toPath()
                            .resolve("update")
                    updateDir.createDirectories()

                    val jarFile = updateDir.resolve("$name.jar")
                    jarFile.writeBytes(response.body())

                    instance.logger.info("Downloaded $name-$version to plugins/update/")
                    callback(true, version)
                } catch (e: Exception) {
                    instance.logger.warning("Failed to download update: ${e.message}")
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
