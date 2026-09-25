package org.xodium.illyriacore.mechanics.player

import io.papermc.paper.command.brigadier.Commands
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriacore.IllyriaCore.Companion.instance
import org.xodium.illyriacore.Utils.Command.playerExecuted
import org.xodium.illyriacore.data.CommandData
import org.xodium.illyriacore.mechanics.MechanicInterface
import org.xodium.illyrialib.Utils.MM

/** Represents a mechanic handling item condensing within the system. */
internal object CondenseMechanic : MechanicInterface {
    private const val CONDENSE_AMOUNT: Int = 9
    private val CONDENSABLE: Map<Material, Material> =
        sortedMapOf(
            Material.AMETHYST_SHARD to Material.AMETHYST_BLOCK,
            Material.BONE_MEAL to Material.BONE_BLOCK,
            Material.COAL to Material.COAL_BLOCK,
            Material.COPPER_INGOT to Material.COPPER_BLOCK,
            Material.DIAMOND to Material.DIAMOND_BLOCK,
            Material.DRIED_KELP to Material.DRIED_KELP_BLOCK,
            Material.EMERALD to Material.EMERALD_BLOCK,
            Material.GOLD_INGOT to Material.GOLD_BLOCK,
            Material.GOLD_NUGGET to Material.GOLD_INGOT,
            Material.IRON_INGOT to Material.IRON_BLOCK,
            Material.IRON_NUGGET to Material.IRON_INGOT,
            Material.LAPIS_LAZULI to Material.LAPIS_BLOCK,
            Material.MELON_SLICE to Material.MELON,
            Material.NETHER_WART to Material.NETHER_WART_BLOCK,
            Material.NETHERITE_INGOT to Material.NETHERITE_BLOCK,
            Material.QUARTZ to Material.QUARTZ_BLOCK,
            Material.REDSTONE to Material.REDSTONE_BLOCK,
            Material.SLIME_BALL to Material.SLIME_BLOCK,
            Material.WHEAT to Material.HAY_BLOCK,
        )

    override val cmds: Collection<CommandData> =
        listOf(
            CommandData(
                Commands
                    .literal("condense")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> player.condense(reverse = false) },
                "Condenses all applicable items in your inventory into their block form",
                listOf("cn"),
            ),
            CommandData(
                Commands
                    .literal("uncondense")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .playerExecuted { player, _ -> player.condense(reverse = true) },
                "Uncondenses all applicable blocks in your inventory into their item form",
                listOf("ucn"),
            ),
        )

    override val perms: List<Permission> =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.condense".lowercase(),
                "Allows to condense and uncondense items",
                PermissionDefault.TRUE,
            ),
        )

    /**
     * Condenses or uncondenses all applicable items in the player's inventory.
     *
     * @param reverse If true, splits blocks back into their base items.
     */
    private fun Player.condense(reverse: Boolean) {
        val map = if (reverse) CONDENSABLE.entries.associate { it.value to it.key } else CONDENSABLE
        var total = 0

        inventory.contents.forEachIndexed { index, stack ->
            stack?.let { condense(index, it, map)?.also { count -> total += count } }
        }

        sendActionBar(
            MM.deserialize(
                if (total > 0) {
                    "<green>${if (reverse) "Uncondensed" else "Condensed"} <yellow>$total</yellow> item(s)"
                } else {
                    "<red>Nothing to ${if (reverse) "uncondense" else "condense"}"
                },
            ),
        )
    }

    /**
     * Condenses a single inventory slot.
     *
     * @param index The inventory slot index.
     * @param stack The [ItemStack] currently in the slot.
     * @param map The condensing map (base → block or block → base).
     * @return The number of items consumed/produced, or null if not condensable.
     */
    private fun Player.condense(
        index: Int,
        stack: ItemStack,
        map: Map<Material, Material>,
    ): Int? {
        val target = map[stack.type] ?: return null
        val amount = stack.amount / CONDENSE_AMOUNT
        if (amount < 1) return null

        stack.amount -= amount * CONDENSE_AMOUNT
        if (stack.amount <= 0) inventory.setItem(index, null)

        inventory
            .addItem(ItemStack.of(target, amount))
            .values
            .forEach { world.dropItemNaturally(location, it) }
        return amount
    }
}
