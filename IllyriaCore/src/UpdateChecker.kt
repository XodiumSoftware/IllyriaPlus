package org.xodium.illyriacore

import com.google.gson.JsonParser
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** Checks GitHub nightly releases for newer builds of this plugin. */
internal object UpdateChecker {
    private const val REPO = "XodiumSoftware/IllyriaPlus"
    private const val RELEASE_URL = "https://api.github.com/repos/$REPO/releases/tags/nightly"
    private const val ASSET_SUFFIX = ".jar"

    private val name: String get() = instance::class.simpleName ?: "Unknown"
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    /** Checks asynchronously whether a newer build is available and logs the result. */
    fun check() {
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
                    "<green>$latest</green> <gray>(current: $current)</gray>\n" +
                    "<gray>Download: <click:open_url:'https://github.com/$REPO/releases/tag/nightly'>" +
                    "<underlined>GitHub Nightly</underlined></click></gray>",
            )
        instance.server.scheduler.runTask(
            instance,
            Runnable {
                instance.server.onlinePlayers
                    .filter { it.isOp }
                    .forEach { it.sendMessage(component) }
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
    private fun extractBuildNumber(version: String): Int =
        version.substringAfter("+build.").toIntOrNull() ?: 0
}
