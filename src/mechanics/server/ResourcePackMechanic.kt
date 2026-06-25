package org.xodium.illyriaplus.mechanics.server

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.Command.playerExecuted
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.mechanics.MechanicInterface
import java.net.URI
import java.util.concurrent.CompletableFuture
import javax.net.ssl.HttpsURLConnection
import kotlin.time.measureTime

/** Represents a mechanic that sends the IllyriaPlus resource pack to joining players. */
internal object ResourcePackMechanic : MechanicInterface {
    private const val RELEASE_TAG = "nightly_resourcepack"
    private const val API_URL = "https://api.github.com/repos/XodiumSoftware/IllyriaPlus/releases/tags/$RELEASE_TAG"
    private const val RELEASE_URL = "https://github.com/XodiumSoftware/IllyriaPlus/releases/download/$RELEASE_TAG/"
    private const val ASSET_PREFIX = "irp_v"
    private const val ASSET_SUFFIX = ".zip"
    private const val REQUIRED = true
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var resourcePackInfo: ResourcePackInfo? = null

    override val cmds: Collection<CommandData> =
        listOf(
            CommandData(
                Commands
                    .literal("reloadresourcepack")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ ->
                        player.sendActionBar(
                            MM.deserialize("<green>Reloading IllyriaPlus resource pack for all online players..."),
                        )
                        reloadResourcePackForAll()
                    },
                "Reloads the IllyriaPlus resource pack for all online players",
                listOf("rrp"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.reloadresourcepack".lowercase(),
                "Allows reloading the IllyriaPlus resource pack",
                PermissionDefault.OP,
            ),
        )

    override fun register(): Long = super.register() + measureTime { fetchResourcePackInfoAsync() }.inWholeMilliseconds

    @Suppress("UnstableApiUsage")
    @EventHandler(priority = EventPriority.NORMAL)
    fun on(event: AsyncPlayerConnectionConfigureEvent) {
        val connection = event.connection as? Audience ?: return

        connection.sendResourcePacks(createRequest(resourcePackInfo ?: return))
    }

    private fun createRequest(info: ResourcePackInfo): ResourcePackRequest =
        ResourcePackRequest
            .resourcePackRequest()
            .packs(info)
            .required(REQUIRED)
            .build()

    private fun fetchResourcePackInfoAsync(onResolved: ((ResourcePackInfo) -> Unit)? = null) {
        instance.server.scheduler.runTaskAsynchronously(instance) { _ ->
            try {
                fetchLatestResourcePack()
                    .thenAccept { info ->
                        instance.server.scheduler.runTask(instance) { _ ->
                            resourcePackInfo = info
                            instance.logger.info("Resource pack resolved: ${info.uri()}")
                            onResolved?.invoke(info)
                        }
                    }.exceptionally {
                        instance.logger.warning("Failed to compute resource pack hash: ${it.message}")
                        null
                    }
            } catch (ex: Exception) {
                instance.logger.warning("Failed to resolve latest resource pack URL: ${ex.message}")
            }
        }
    }

    /**
     * Queries the GitHub API for the configured nightly release and returns a future that
     * builds a [ResourcePackInfo] from the first matching `irp_v*.zip` asset.
     *
     * @return A future that resolves to the resource pack information.
     */
    private fun fetchLatestResourcePack(): CompletableFuture<ResourcePackInfo> =
        ResourcePackInfo
            .resourcePackInfo()
            .uri(URI.create(fetchAssetUrl()))
            .computeHashAndBuild()

    /**
     * Reloads the resource pack by re-querying GitHub and sending it to every online player.
     */
    private fun reloadResourcePackForAll() {
        fetchResourcePackInfoAsync { info ->
            val request = createRequest(info)

            instance.server.onlinePlayers.forEach {
                it.clearResourcePacks()
                it.sendResourcePacks(request)
            }

            instance.logger.info(
                "Resource pack reloaded for ${instance.server.onlinePlayers.size} player(s)",
            )
        }
    }

    /**
     * Fetches the download URL for the first matching `irp_v*.zip` asset in the
     * configured GitHub release.
     *
     * @return The full asset download URL.
     * @throws IllegalStateException if the release or matching asset cannot be found.
     */
    private fun fetchAssetUrl(): String {
        val connection = URI.create(API_URL).toURL().openConnection() as HttpsURLConnection

        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.setRequestProperty("User-Agent", "IllyriaPlus")
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.doInput = true

        val response = connection.inputStream.use { it.reader().readText() }
        val release = json.parseToJsonElement(response).jsonObject

        val assetName =
            release["assets"]
                ?.jsonArray
                ?.map { it.jsonObject }
                ?.firstNotNullOfOrNull { asset ->
                    asset["name"]
                        ?.jsonPrimitive
                        ?.content
                        ?.takeIf {
                            it.startsWith(ASSET_PREFIX) &&
                                it.endsWith(ASSET_SUFFIX)
                        }
                }
                ?: error("No $ASSET_PREFIX*$ASSET_SUFFIX asset found in $RELEASE_TAG")

        return "$RELEASE_URL$assetName"
    }
}
