package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils.Command.playerExecuted
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.data.FaqTab
import org.xodium.illyriaplus.mechanics.MechanicInterface
import org.xodium.illyriaplus.pdcs.PlayerPDC.elytraSwap
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling elytra swapping within the system. */
internal object ElytraSwapMechanic : MechanicInterface {
    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("locator")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> player.toggleElytraSwap() },
                "Allows players to toggle elytra swap",
                listOf("es"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.elytra_swap".lowercase(),
                "Allows use of the elytra swap command",
                PermissionDefault.TRUE,
            ),
        )

    override val faqTab = FaqTab.PLAYER_MECHANIC

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.ELYTRA)
                .setName(MM.deserialize("<mango>Elytra Swap Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<yellow>Auto Equip</yellow> <firewatch>></gradient> " +
                            "<white>Equips elytra when you start gliding</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Auto Restore</yellow> <firewatch>></gradient> " +
                            "<white>Restores chestplate when you land</white>",
                    ),
                ),
        )

    @EventHandler
    fun on(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return

        if (!player.elytraSwap) return

        when {
            event.isGliding -> startGliding(player)
            else -> stopGliding(player)
        }
    }

    /**
     * Equips an elytra from the player's inventory when they start gliding.
     *
     * @param player The [Player] who started gliding.
     */
    private fun startGliding(player: Player) =
        trySwap(
            player,
            guard = { it?.type != Material.ELYTRA },
            find = { it?.type == Material.ELYTRA },
        )

    /**
     * Restores the previously equipped chestplate after the player stops gliding.
     *
     * @param player The [Player] who stopped gliding.
     */
    private fun stopGliding(player: Player) =
        trySwap(
            player,
            guard = { it?.type == Material.ELYTRA },
            find = { it != null && Tag.ITEMS_CHEST_ARMOR.isTagged(it.type) },
        )

    /**
     * Swaps the player's chestplate with an item from their inventory that matches the given predicate.
     *
     * @param player The [Player] whose chestplate will be swapped.
     * @param guard A predicate that must pass for the currently equipped chestplate.
     * @param find A predicate used to locate the replacement item in the player's inventory.
     */
    private inline fun trySwap(
        player: Player,
        crossinline guard: (ItemStack?) -> Boolean,
        crossinline find: (ItemStack?) -> Boolean,
    ) {
        if (!guard(player.inventory.chestplate)) return

        val slot = player.inventory.contents.indexOfFirst(find)

        if (slot == -1) return

        val equipped = player.inventory.chestplate
        val target = player.inventory.getItem(slot) ?: return

        player.inventory.setItem(slot, equipped)
        player.inventory.setChestplate(target)
        player.updateInventory()
    }

    /** Toggles elytra swap preference and sends feedback to the player. */
    private fun Player.toggleElytraSwap() {
        elytraSwap = !elytraSwap
        sendActionBar(
            MM.deserialize(
                if (elytraSwap) "<green>Elytra swap enabled</green>" else "<red>Elytra swap disabled</red>",
            ),
        )
    }
}
