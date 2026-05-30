package org.xodium.illyriaplus.mechanics.entity.monster

import org.bukkit.Difficulty

/** Represents a contract for a monster-specific mechanic within the system. */
internal interface MonsterInterface {
    /** The difficulty on which this mechanic's spawn modifications apply. */
    val difficulty: Difficulty get() = Difficulty.HARD
}
