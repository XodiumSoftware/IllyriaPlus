package org.xodium.illyriacore.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.event.player.PlayerArmSwingEvent
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.entity.SmallFireball
import org.bukkit.entity.Snowball
import org.bukkit.entity.WindCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.EquipmentSlot
import org.xodium.illyriacore.advancements.BendingAdvancement
import org.xodium.illyriacore.dialogs.BendingDialog
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

    /** Forward speed applied to the earth boulder projectile. */
    private const val EARTH_BOULDER_SPEED = 1.5

    /** Upward bias applied to the earth boulder so it arcs. */
    private const val EARTH_BOULDER_LIFT = 0.3

    /** Damage dealt per fallen block when the boulder lands on an entity. */
    private const val EARTH_BOULDER_DAMAGE_PER_BLOCK = 2.0f

    /** Maximum damage a boulder can deal to a single entity. */
    private const val EARTH_BOULDER_MAX_DAMAGE = 6

    /** The material the earth boulder is made of. */
    private val EARTH_BOULDER_MATERIAL: Material = Material.MUD

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
        when (type) {
            BendingTypeEnum.AIR -> player.launchProjectile(WindCharge::class.java)
            BendingTypeEnum.FIRE -> player.launchProjectile(SmallFireball::class.java)
            BendingTypeEnum.WATER -> player.launchProjectile(Snowball::class.java)
            BendingTypeEnum.EARTH -> launchEarthBoulder(player)
        }
    }

    /**
     * Lobs a falling-block boulder made of [EARTH_BOULDER_MATERIAL] from the player's eye,
     * arcing in the direction they're looking. The boulder damages entities it lands on and
     * does not drop an item when it settles.
     *
     * @param player The player launching the boulder.
     */
    private fun launchEarthBoulder(player: Player) {
        val direction = player.location.direction
        player.world.spawn(player.eyeLocation, FallingBlock::class.java) { boulder ->
            boulder.blockData = EARTH_BOULDER_MATERIAL.createBlockData()
            boulder.velocity = direction.multiply(EARTH_BOULDER_SPEED).setY(direction.y + EARTH_BOULDER_LIFT)
            boulder.setHurtEntities(true)
            boulder.damagePerBlock = EARTH_BOULDER_DAMAGE_PER_BLOCK
            boulder.maxDamage = EARTH_BOULDER_MAX_DAMAGE
            boulder.dropItem = false
            boulder.cancelDrop = true
        }
    }
}
