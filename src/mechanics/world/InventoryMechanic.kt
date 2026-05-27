
package org.xodium.illyriaplus.mechanics.world

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Container
import org.bukkit.block.Lidded
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.Utils
import org.xodium.illyriaplus.Utils.BlockUtils.center
import org.xodium.illyriaplus.Utils.CommandUtils.playerExecuted
import org.xodium.illyriaplus.Utils.MM
import org.xodium.illyriaplus.Utils.PlayerUtils.getContainersAround
import org.xodium.illyriaplus.data.CommandData
import org.xodium.illyriaplus.data.FaqCategory
import org.xodium.illyriaplus.interfaces.MechanicInterface
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder

/** Represents a mechanic handling inventory interactions within the system. */
internal object InventoryMechanic : MechanicInterface {
    private const val NO_MATERIAL_SPECIFIED_MSG: String =
        "<firewatch>You must specify a valid material or hold something in your hand</gradient>"
    private const val NO_MATCHING_ITEMS_MSG: String =
        "<firewatch>No containers contain <rose><b><material></b></gradient></gradient>"
    private const val FOUND_ITEMS_IN_CHESTS_MSG: String =
        "<mango>Found <rose><b><material></b></gradient> in container(s), follow trail(s)</gradient>"
    private const val NO_CONTAINERS_FOUND_MSG: String = "<firewatch>No containers found nearby</gradient>"

    private val SEARCH_SUCCESSFUL_SOUND: Sound =
        Sound.sound(Key.key("entity.player.levelup"), Sound.Source.PLAYER, 1.0f, 1.0f)
    private val SEARCH_FAILED_SOUND: Sound =
        Sound.sound(Key.key("block.anvil.land"), Sound.Source.PLAYER, 1.0f, 1.0f)
    private val UNLOAD_SUCCESSFUL_SOUND: Sound =
        Sound.sound(Key.key("entity.player.levelup"), Sound.Source.PLAYER, 1.0f, 1.0f)
    private val UNLOAD_FAILED_SOUND: Sound =
        Sound.sound(Key.key("block.anvil.land"), Sound.Source.PLAYER, 1.0f, 1.0f)

    override val faqItem =
        Item.simple(
            ItemBuilder(Material.CHEST)
                .setName(MM.deserialize("<mango>Inventory Mechanics</gradient>"))
                .addLoreLines(
                    MM.deserialize(""),
                    MM.deserialize(
                        "<gray>cmd:</gray> <yellow>/search</yellow> <firewatch>></gradient> " +
                            "<white>Find items in nearby chests</white>",
                    ),
                    MM.deserialize(
                        "<gray>cmd:</gray> <yellow>/unload</yellow> <firewatch>></gradient> " +
                            "<white>Dump inventory into nearby chests</white>",
                    ),
                ),
        )

    override val faqCategory = FaqCategory.WORLD

    override val cmds =
        listOf(
            CommandData(
                Commands
                    .literal("search")
                    .requires { it.sender.hasPermission(perms[0]) }
                    .then(
                        Commands
                            .argument("material", ArgumentTypes.itemStack())
                            .playerExecuted { player, ctx ->
                                searchContainer(player, ctx.getArgument("material", ItemStack::class.java).type)
                            },
                    ).playerExecuted { player, _ -> searchContainer(player, player.inventory.itemInMainHand.type) },
                "Search nearby chests for specific items",
                listOf("invsearch", "searchinv", "invs", "sinv", "s"),
            ),
            CommandData(
                Commands
                    .literal("unload")
                    .requires { it.sender.hasPermission(perms[1]) }
                    .playerExecuted { player, _ -> unloadInventory(player) },
                "Unload inventory into nearby chests",
                listOf("invunload", "unloadinv", "invu", "uinv", "u"),
            ),
        )

    override val perms =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.invsearch".lowercase(),
                "Allows use of the invsearch command",
                PermissionDefault.TRUE,
            ),
            Permission(
                "${instance.javaClass.simpleName}.invunload".lowercase(),
                "Allows use of the invunload command",
                PermissionDefault.TRUE,
            ),
        )

    /**
     * Searches for chests within the specified radius of the player that contain the specified material.
     *
     * @param player The player who initiated the search.
     * @param material The material to search for in the chests.
     */
    private fun searchContainer(
        player: Player,
        material: Material,
    ) {
        if (material == Material.AIR) {
            player.sendActionBar(MM.deserialize(NO_MATERIAL_SPECIFIED_MSG))
            player.playSound(SEARCH_FAILED_SOUND)
            return
        }

        val containers = player.getContainersAround().filter { it.inventory.contains(material) }

        if (containers.isEmpty()) {
            player.sendActionBar(
                MM.deserialize(
                    NO_MATCHING_ITEMS_MSG,
                    Placeholder.component("material", MM.deserialize(material.name)),
                ),
            )
            player.playSound(SEARCH_FAILED_SOUND)
            return
        }

        player.sendActionBar(
            MM.deserialize(
                FOUND_ITEMS_IN_CHESTS_MSG,
                Placeholder.component("material", MM.deserialize(material.name)),
            ),
        )

        player.playSound(SEARCH_SUCCESSFUL_SOUND)

        Utils.ScheduleUtils.schedule(duration = 200L) {
            containers.forEach {
                Particle.TRAIL
                    .builder()
                    .location(player.location)
                    .data(Particle.Trail(it.block.center(), Color.MAROON, 40))
                    .receivers(player)
                    .spawn()
                Particle.DUST
                    .builder()
                    .location(it.block.center())
                    .count(10)
                    .data(Particle.DustOptions(Color.MAROON, 5.0f))
                    .receivers(player)
                    .spawn()
            }
        }
    }

    /**
     * Moves items from the player's inventory into nearby containers.
     *
     * @param player The player whose inventory items are to be unloaded into nearby containers.
     */
    private fun unloadInventory(player: Player) {
        val containers =
            player
                .getContainersAround()
                .filter { it.block.state is Lidded }
                .filter { container ->
                    container.inventory.storageContents.any { it == null || it.amount < it.maxStackSize }
                }

        if (containers.isEmpty()) {
            player.sendActionBar(MM.deserialize(NO_CONTAINERS_FOUND_MSG))
            player.playSound(UNLOAD_FAILED_SOUND)
            return
        }

        val usedContainers = mutableSetOf<Container>()

        for (slot in 9..35) {
            val itemStack = player.inventory.getItem(slot) ?: continue

            if (itemStack.type.isAir) continue

            var remaining = itemStack

            val sortedContainers =
                containers
                    .filter { it.inventory.contains(remaining.type) }
                    .sortedByDescending { container ->
                        container.inventory.storageContents
                            .filterNotNull()
                            .filter { it.type == remaining.type }
                            .sumOf { it.amount }
                    }

            for (container in sortedContainers) {
                if (remaining.amount <= 0) break

                val before = remaining.amount
                val leftovers = container.inventory.addItem(remaining.clone())

                remaining = leftovers.values.firstOrNull() ?: ItemStack.of(Material.AIR)

                if (remaining.amount < before) usedContainers.add(container)
            }

            if (remaining.type.isAir) {
                player.inventory.setItem(slot, null)
            } else if (remaining != itemStack) {
                player.inventory.setItem(slot, remaining)
            }
        }

        player.playSound(if (usedContainers.isEmpty()) UNLOAD_FAILED_SOUND else UNLOAD_SUCCESSFUL_SOUND)

        if (usedContainers.isEmpty()) return

        Utils.ScheduleUtils.schedule(duration = 40L) {
            usedContainers.forEach {
                Particle.CRIT
                    .builder()
                    .location(it.block.center())
                    .count(10)
                    .receivers(player)
                    .spawn()
            }
        }
    }
}
