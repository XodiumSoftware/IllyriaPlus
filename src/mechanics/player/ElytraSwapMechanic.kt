package org.xodium.illyriaplus.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerInteractEvent
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
                    .literal("elytraswap")
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
                            "<white>Equips elytra when using firework while falling</white>",
                    ),
                    MM.deserialize(
                        "<yellow>Auto Restore</yellow> <firewatch>></gradient> " +
                            "<white>Restores chestplate when you land</white>",
                    ),
                    MM.deserialize(
                        "<gray>cmd:</gray> <yellow>/elytraswap</yellow> <firewatch>></gradient> " +
                            "<white>Toggle this mechanic on/off</white>",
                    ),
                ),
        )

    @EventHandler
    fun on(event: PlayerInteractEvent) {
        val player = event.player

        when {
            !player.elytraSwap -> return
            event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK -> return
            event.item?.type != Material.FIREWORK_ROCKET -> return
            player.isGliding -> return
            player.velocity.y >= 0 -> return
            player.isInWater || player.isInLava -> return
            player.isFlying -> return
            player.gameMode != GameMode.SURVIVAL && player.gameMode != GameMode.ADVENTURE -> return
            else -> startGliding(player)
        }
    }

    @EventHandler
    fun on(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return

        if (!player.elytraSwap) return
        if (event.isGliding) return

        stopGliding(player)
    }

    /**
     * Equips an elytra from the player's inventory when they use a firework while falling.
     *
     * @param player The [Player] who used a firework while falling.
     */
    private fun startGliding(player: Player) {
        if (
            !trySwap(
                player,
                guard = { it?.type != Material.ELYTRA },
                find = { it?.type == Material.ELYTRA },
            )
        ) {
            return
        }

        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1.0f, 1.0f)
    }

    /**
     * Restores the previously equipped chestplate after the player stops gliding.
     *
     * @param player The [Player] who stopped gliding.
     */
    private fun stopGliding(player: Player) {
        if (
            !trySwap(
                player,
                guard = { it?.type == Material.ELYTRA },
                find = { it != null && Tag.ITEMS_CHEST_ARMOR.isTagged(it.type) },
            )
        ) {
            return
        }

        val sound =
            when (player.inventory.chestplate.type) {
                Material.LEATHER_CHESTPLATE -> Sound.ITEM_ARMOR_EQUIP_LEATHER
                Material.CHAINMAIL_CHESTPLATE -> Sound.ITEM_ARMOR_EQUIP_CHAIN
                Material.IRON_CHESTPLATE -> Sound.ITEM_ARMOR_EQUIP_IRON
                Material.GOLDEN_CHESTPLATE -> Sound.ITEM_ARMOR_EQUIP_GOLD
                Material.DIAMOND_CHESTPLATE -> Sound.ITEM_ARMOR_EQUIP_DIAMOND
                Material.NETHERITE_CHESTPLATE -> Sound.ITEM_ARMOR_EQUIP_NETHERITE
                else -> Sound.ITEM_ARMOR_EQUIP_GENERIC
            }

        player.playSound(player.location, sound, 1.0f, 1.0f)
    }

    /**
     * Swaps the player's chestplate with an item from their inventory that matches the given predicate.
     *
     * @param player The [Player] whose chestplate will be swapped.
     * @param guard A predicate that must pass for the currently equipped chestplate.
     * @param find A predicate used to locate the replacement item in the player's inventory.
     * @return `true` if a swap occurred, `false` otherwise.
     */
    private inline fun trySwap(
        player: Player,
        crossinline guard: (ItemStack?) -> Boolean,
        crossinline find: (ItemStack?) -> Boolean,
    ): Boolean {
        if (!guard(player.inventory.chestplate)) return false

        val slot = player.inventory.contents.indexOfFirst(find)

        if (slot == -1) return false

        val equipped = player.inventory.chestplate
        val target = player.inventory.getItem(slot) ?: return false

        player.inventory.setItem(slot, equipped)
        player.inventory.setChestplate(target)
        player.updateInventory()

        return true
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
