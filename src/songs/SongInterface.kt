package org.xodium.illyriaplus.songs

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.JukeboxSongRegistryEntry
import net.kyori.adventure.key.Key
import org.bukkit.JukeboxSong
import org.xodium.illyriaplus.IllyriaPlus
import org.xodium.illyriaplus.data.SongData

/** Represents a collection of registerable jukebox songs within the system. */
@Suppress("UnstableApiUsage")
internal interface SongInterface {
    /** The complete map of jukebox songs in this collection, keyed by registry name. */
    val songs: Map<String, SongData>

    /**
     * The unique typed key identifying a jukebox song in the registry.
     *
     * @param name The registry key fragment (snake_case) of the jukebox song.
     * @see io.papermc.paper.registry.TypedKey
     * @see io.papermc.paper.registry.RegistryKey.JUKEBOX_SONG
     */
    fun key(name: String): TypedKey<JukeboxSong> =
        TypedKey.create(RegistryKey.JUKEBOX_SONG, Key.key(IllyriaPlus.ID, name))

    /**
     * Configures the properties of a named jukebox song using the provided builder.
     *
     * @param name The registry key fragment (snake_case) of the jukebox song.
     * @param builder The builder used to define the jukebox song properties.
     * @return The builder for method chaining.
     * @throws NoSuchElementException if the jukebox song is not found in [songs].
     */
    fun invoke(
        name: String,
        builder: JukeboxSongRegistryEntry.Builder,
    ): JukeboxSongRegistryEntry.Builder =
        songs.getValue(name).let { song ->
            builder.apply {
                soundEvent(song.soundEvent)
                description(song.description)
                lengthInSeconds(song.lengthInSeconds)
                comparatorOutput(song.comparatorOutput)
            }
        }

    /**
     * Retrieves a jukebox song from the registry.
     *
     * @param name The registry key fragment (snake_case) of the jukebox song.
     * @return The [JukeboxSong] instance corresponding to the key.
     * @throws NoSuchElementException if the jukebox song is not found in the registry.
     */
    fun get(name: String): JukeboxSong =
        RegistryAccess.registryAccess().getRegistry(RegistryKey.JUKEBOX_SONG).getOrThrow(key(name))
}
