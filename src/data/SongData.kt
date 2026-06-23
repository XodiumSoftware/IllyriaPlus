package org.xodium.illyriaplus.data

import io.papermc.paper.registry.data.SoundEventRegistryEntry
import io.papermc.paper.registry.holder.RegistryHolder
import net.kyori.adventure.text.Component
import org.bukkit.Sound

/**
 * Holds the properties required to register a single custom jukebox song.
 *
 * @property soundEvent The sound event to play when this song is active in a jukebox.
 * @property description The display name shown for this song, formatted as a text component.
 * @property lengthInSeconds The duration of the song in seconds. Must be a positive value.
 * @property comparatorOutput The redstone comparator output strength emitted by jukeboxes
 *                            playing this song, in the range 0 to 15.
 */
@Suppress("UnstableApiUsage")
internal data class SongData(
    val soundEvent: RegistryHolder<Sound, SoundEventRegistryEntry>,
    val description: Component,
    val lengthInSeconds: Float,
    val comparatorOutput: Int,
) {
    init {
        require(lengthInSeconds > 0) { "lengthInSeconds must be positive, got $lengthInSeconds" }
        require(comparatorOutput in 0..15) { "comparatorOutput must be in 0..15, got $comparatorOutput" }
    }
}
