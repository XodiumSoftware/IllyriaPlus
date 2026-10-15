package org.xodium.illyriabending.mechanics

import com.mojang.brigadier.arguments.IntegerArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.event.player.PlayerArmSwingEvent
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.xodium.illyriabending.advancements.BendingAdvancement
import org.xodium.illyriabending.dialogs.BendingDialog
import org.xodium.illyriabending.pdcs.PlayerPDC.MAX_BENDING_LEVEL
import org.xodium.illyriabending.pdcs.PlayerPDC.MIN_BENDING_LEVEL
import org.xodium.illyriabending.pdcs.PlayerPDC.bendingLevel
import org.xodium.illyriabending.pdcs.PlayerPDC.bendingType
import org.xodium.illyriabending.pdcs.PlayerPDC.hasBendingType
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.Utils.Command.requiresPlayer
import org.xodium.illyrialib.Utils.Command.resolvePlayerArg
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyrialib.data.CommandData
import java.util.UUID

/** Represents a mechanic handling player bending within the system. */
internal object BendingMechanic : MechanicInterface {
    private const val RESET_SENDER_MSG = "<green>Reset bending type for <gold><target></gold>."
    private const val RESET_TARGET_MSG = "<green>Your bending type has been reset."
    private const val LEVEL_SENDER_MSG = "<green>Set <gold><target></gold>'s bending level to <gold><level></gold>."
    private const val LEVEL_TARGET_MSG = "<green>Your bending level is now <gold><level></gold>."
    private const val LEVEL_NO_TYPE_MSG = "<red><target> has not chosen a bending type."

    /** Wall-clock milliseconds per Minecraft tick. */
    private const val TICK_MS = 50L

    /** Last successful cast time per player, in epoch milliseconds. */
    private val lastCastMs = mutableMapOf<UUID, Long>()

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("bending")
                    .playerExecuted { player, _ -> player.showDialog(BendingDialog(player)) }
                    .then(
                        Commands
                            .argument("target", ArgumentTypes.player())
                            .requiresPlayer { isOp }
                            .then(
                                Commands
                                    .literal("level")
                                    .then(
                                        Commands
                                            .argument(
                                                "level",
                                                IntegerArgumentType.integer(
                                                    MIN_BENDING_LEVEL,
                                                    MAX_BENDING_LEVEL,
                                                ),
                                            ).playerExecuted { player, ctx ->
                                                val target = ctx.resolvePlayerArg(player) ?: return@playerExecuted
                                                val type =
                                                    target.bendingType
                                                        ?: return@playerExecuted player.sendActionBar(
                                                            MM.deserialize(
                                                                LEVEL_NO_TYPE_MSG,
                                                                Placeholder.unparsed("target", target.name),
                                                            ),
                                                        )
                                                val level = IntegerArgumentType.getInteger(ctx, "level")
                                                target.bendingLevel = level
                                                BendingAdvancement.grantLevels(target, type, level)
                                                player.sendActionBar(
                                                    MM.deserialize(
                                                        LEVEL_SENDER_MSG,
                                                        Placeholder.unparsed("target", target.name),
                                                        Placeholder.unparsed("level", level.toString()),
                                                    ),
                                                )
                                                target.sendActionBar(
                                                    MM.deserialize(
                                                        LEVEL_TARGET_MSG,
                                                        Placeholder.unparsed("level", level.toString()),
                                                    ),
                                                )
                                            },
                                    ),
                            ).then(
                                Commands
                                    .literal("reset")
                                    .playerExecuted { player, ctx ->
                                        val target = ctx.resolvePlayerArg(player) ?: return@playerExecuted
                                        target.bendingType = null
                                        target.bendingLevel = MIN_BENDING_LEVEL
                                        BendingAdvancement.revokeElements(target)
                                        player.sendActionBar(
                                            MM.deserialize(
                                                RESET_SENDER_MSG,
                                                Placeholder.unparsed("target", target.name),
                                            ),
                                        )
                                        target.sendActionBar(MM.deserialize(RESET_TARGET_MSG))
                                    },
                            ),
                    ),
                "Opens the bending selection dialog",
            ),
        )

    @EventHandler
    fun on(event: PlayerJoinEvent) = grantRootAdvancement(event)

    @EventHandler
    fun on(event: PlayerQuitEvent) = clearCastCooldown(event)

    @EventHandler
    fun on(event: PlayerArmSwingEvent) = trackCastCooldown(event)

    /**
     * Grants the root advancement if the player has not chosen a bending type.
     *
     * @param event The [PlayerJoinEvent] triggered when a player joins the server.
     */
    private fun grantRootAdvancement(event: PlayerJoinEvent) {
        if (!event.player.hasBendingType) BendingAdvancement.grantRoot(event.player)
    }

    /**
     * Clears the player's cast cooldown entry.
     *
     * @param event The [PlayerQuitEvent] triggered when a player leaves the server.
     */
    private fun clearCastCooldown(event: PlayerQuitEvent) {
        lastCastMs.remove(event.player.uniqueId)
    }

    /**
     * Records a bending cast if the swing is valid and the player's cooldown has elapsed.
     *
     * A swing is a valid cast when it is performed with the main hand, the main hand is empty,
     * and the off hand holds the material matching the player's bending type.
     *
     * @param event The [PlayerArmSwingEvent] triggered when a player swings their arm.
     */
    private fun trackCastCooldown(event: PlayerArmSwingEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        if (!player.inventory.itemInMainHand.isEmpty) return
        val type = player.bendingType ?: return
        if (player.inventory.itemInOffHand.type != type.material) return
        val now = System.currentTimeMillis()
        val last = lastCastMs[player.uniqueId]
        if (last != null && now - last < type.cooldownTicks * TICK_MS) return
        lastCastMs[player.uniqueId] = now
    }
}
