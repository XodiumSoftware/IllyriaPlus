package org.xodium.illyriaseasons

import dev.wyck.renderer.packet.PacketHandler

/** Placeholder marker for the seasons system. Will be implemented with Wyck biome renderers. */
internal object SeasonSystem {
    /** Returns a [PacketHandler] for injecting virtual seasonal biomes. Not yet wired. */
    @Suppress("unused")
    fun packetHandler(): PacketHandler = PacketHandler.of(IllyriaSeasons.instance)
}
