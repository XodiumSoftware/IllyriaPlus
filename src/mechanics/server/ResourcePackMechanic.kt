package org.xodium.illyriaplus.mechanics.server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.mechanics.MechanicInterface
import org.xodium.illyriaplus.mechanics.server.ResourcePackMechanic.resourcePackInfo
import java.net.URI
import javax.net.ssl.HttpsURLConnection

/** Represents a mechanic that sends the IllyriaPlus resource pack to joining players. */
internal object ResourcePackMechanic : MechanicInterface {
    private const val REPOSITORY = "XodiumSoftware/IllyriaPlus"
    private const val TAG = "nightly_resourcepack"
    private const val API_URL = "https://api.github.com/repos/$REPOSITORY/releases/tags/$TAG"
    private const val ASSET_PREFIX = "irp_v"
    private const val ASSET_SUFFIX = ".zip"
    private const val HASH_PATTERN = "SHA-1 Hash: ([a-f0-9]{40})"
    private const val REQUIRED = true
    private const val PROMPT =
        "<firewatch>The IllyriaPlus resource pack is required to play on this server.</gradient>"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    private val json = Json { ignoreUnknownKeys = true }
    private var resourcePackInfo: ResourcePackInfo? = null

    override fun register(): Long {
        fetchResourcePackInfoAsync()
        return super.register()
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun on(event: PlayerJoinEvent) {
        if (resourcePackInfo == null) {
            instance.logger.warning("Resource pack not available for ${event.player.name}")
            return
        }

        val info = resourcePackInfo!!
        val request =
            ResourcePackRequest
                .resourcePackRequest()
                .packs(info)
                .required(REQUIRED)
                .prompt(
                    MM.deserialize(PROMPT),
                ).build()

        event.player.sendResourcePacks(request)
    }

    /**
     * Fetches the latest resource pack information from GitHub asynchronously.
     *
     * The result is stored in [resourcePackInfo] and logged on success or failure.
     */
    private fun fetchResourcePackInfoAsync() {
        instance.server.scheduler.runTaskAsynchronously(instance) { _ ->
            runCatching { fetchLatestResourcePack() }
                .onSuccess { info ->
                    resourcePackInfo = info
                    instance.logger.info("Resource pack resolved: ${info.uri()}")
                }.onFailure {
                    instance.logger.warning("Failed to resolve latest resource pack URL: ${it.message}")
                }
        }
    }

    /**
     * Queries the GitHub API for the configured nightly release and builds a
     * [ResourcePackInfo] from the first matching `irp_v*.zip` asset.
     *
     * If the release body contains a SHA-1 hash in the expected format, it is included
     * so the client can skip re-downloading an unchanged pack.
     *
     * @return The resolved resource pack information.
     * @throws IllegalStateException if the release or matching asset cannot be found.
     */
    private fun fetchLatestResourcePack(): ResourcePackInfo {
        val connection = URI.create(API_URL).toURL().openConnection() as HttpsURLConnection

        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", "IllyriaPlus/$REPOSITORY")
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.doInput = true

        val response = connection.inputStream.use { it.reader().readText() }
        val release = json.parseToJsonElement(response).jsonObject
        val assets = release["assets"]?.jsonArray ?: error("Release has no assets")
        val asset =
            assets
                .map { it.jsonObject }
                .firstOrNull { asset ->
                    asset["name"]?.jsonPrimitive?.content?.let { name ->
                        name.startsWith(ASSET_PREFIX) && name.endsWith(ASSET_SUFFIX)
                    } == true
                }
                ?: error("No $ASSET_PREFIX*$ASSET_SUFFIX asset found in release $TAG")

        val url =
            asset["browser_download_url"]?.jsonPrimitive?.content
                ?: error("Asset download URL missing")
        val hash = release["body"]?.jsonPrimitive?.content?.let { parseHash(it) }
        val builder = ResourcePackInfo.resourcePackInfo().uri(URI.create(url))

        hash?.let { builder.hash(it) }

        return builder.build()
    }

    /**
     * Parses a 40-character lowercase SHA-1 hash from the release body.
     *
     * @param body The release body text to search.
     * @return The parsed hash, or `null` if no match is found.
     */
    private fun parseHash(body: String): String? =
        Regex(HASH_PATTERN)
            .find(body)
            ?.groupValues
            ?.get(1)
            ?.lowercase()
}
