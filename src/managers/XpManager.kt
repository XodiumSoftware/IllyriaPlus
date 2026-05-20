package org.xodium.illyriaplus.managers

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.xodium.illyriaplus.Utils.MM

/**
 * Manages XP cost validation and deduction for spell enchantments.
 * Provides a unified way to handle spell casting costs across all wand-based spells.
 */
internal object XpManager {
    private const val NO_XP_MSG = "<gradient:#CB2D3E:#EF473A><b>Not enough XP!</b></gradient>"

    private val NO_XP_SOUND: Sound =
        Sound.sound(Key.key("block.beacon.deactivate"), Sound.Source.PLAYER, 1.0f, 1.0f)

    /**
     * Consumes XP for a spell cast if the player has enough.
     *
     * @param event The interaction event triggering the spell.
     * @param xpCost The XP cost required.
     * @return True if the cast is allowed, false otherwise.
     */
    fun consumeXp(
        event: PlayerInteractEvent,
        xpCost: Int,
    ): Boolean {
        when {
            event.player.gameMode == GameMode.CREATIVE -> {
                event.isCancelled = true
                return true
            }

            !event.player.hasEnoughXp(xpCost) -> {
                event.player.playSound(NO_XP_SOUND)
                event.player.sendActionBar(MM.deserialize(NO_XP_MSG))
                return false
            }

            else -> {
                event.isCancelled = true
                event.player.giveExp(-xpCost)
                return true
            }
        }
    }

    /**
     * Checks if the player has enough total experience points.
     *
     * @param xpCost The required XP amount.
     * @return True if the player has enough XP, false otherwise.
     */
    private fun Player.hasEnoughXp(xpCost: Int): Boolean = calculateTotalExperiencePoints() >= xpCost
}
