package org.xodium.illyriacore.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.event.player.PlayerArmSwingEvent
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EquipmentSlot
import org.xodium.illyriacore.advancements.BendingAdvancement
import org.xodium.illyriacore.dialogs.BendingDialog
import org.xodium.illyriacore.effects.AirEffect
import org.xodium.illyriacore.effects.EarthEffect
import org.xodium.illyriacore.effects.EffectInterface
import org.xodium.illyriacore.effects.FireEffect
import org.xodium.illyriacore.effects.WaterEffect
import org.xodium.illyriacore.enums.BendingTypeEnum
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyriacore.pdcs.PlayerPDC.bendingType
import org.xodium.illyriacore.pdcs.PlayerPDC.hasBendingType
import org.xodium.illyrialib.Utils.Command.playerExecuted
import org.xodium.illyrialib.Utils.Command.requiresPlayer
import org.xodium.illyrialib.Utils.MM
import org.xodium.illyrialib.data.CommandData

/** Represents a mechanic handling player bending within the system. */
internal object BendingMechanic : MechanicInterface {
    private const val PLAYER_IS_NOT_ONLINE_MSG = "<red>That player is not online!"
    private const val RESET_SENDER_MSG = "<green>Reset bending type for <gold><target></gold>."
    private const val RESET_TARGET_MSG = "<green>Your bending type has been reset."

    /** Y-offset applied to the effect location so particles appear around the player's torso. */
    private const val EFFECT_HEIGHT_OFFSET = 1.0

    /** Effects keyed by their [BendingTypeEnum] for offhand lookup. */
    private val effects: Map<BendingTypeEnum, EffectInterface> =
        listOf(AirEffect, EarthEffect, FireEffect, WaterEffect).associateBy { it.type }

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("bending")
                    .playerExecuted { player, _ -> player.showDialog(BendingDialog(player)) }
                    .then(
                        Commands
                            .literal("reset")
                            .requiresPlayer { isOp }
                            .then(
                                Commands
                                    .argument("target", ArgumentTypes.player())
                                    .playerExecuted { player, ctx ->
                                        val target =
                                            ctx
                                                .getArgument("target", PlayerSelectorArgumentResolver::class.java)
                                                .resolve(ctx.source)
                                                .singleOrNull()
                                                ?: return@playerExecuted player.sendActionBar(
                                                    MM.deserialize(PLAYER_IS_NOT_ONLINE_MSG),
                                                )
                                        target.bendingType = null
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
    fun on(event: PlayerJoinEvent) {
        if (!event.player.hasBendingType) BendingAdvancement.grantRoot(event.player)
    }

    @EventHandler
    fun on(event: PlayerArmSwingEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        val type = player.bendingType ?: return
        if (player.inventory.itemInOffHand.type != type.material) return
        effects[type]?.spawn(player.location.add(0.0, EFFECT_HEIGHT_OFFSET, 0.0))
    }
}
