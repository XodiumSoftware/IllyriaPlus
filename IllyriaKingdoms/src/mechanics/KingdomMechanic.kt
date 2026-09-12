package org.xodium.illyriakingdoms.mechanics

import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.scheduler.BukkitTask
import org.xodium.illyriakingdoms.IllyriaKingdoms.Companion.instance
import org.xodium.illyriakingdoms.Utils
import org.xodium.illyriakingdoms.data.CommandData
import org.xodium.illyriakingdoms.data.DeadNpcData
import org.xodium.illyriakingdoms.data.KingdomData
import org.xodium.illyriakingdoms.gui.KingdomGui
import java.util.UUID

/** Represents a mechanic handling kingdom management within the system. */
internal object KingdomMechanic : MechanicInterface {
    /** Maps player UUID to their invite-mode task; non-null means the player is in invite mode. */
    private val inviteMode = mutableMapOf<UUID, BukkitTask>()

    override val cmds: Collection<CommandData> =
        listOf(
            CommandData(
                Commands
                    .literal("kingdom")
                    .requires {
                        it.sender.hasPermission(
                            "${instance.javaClass.simpleName}.command.kingdom".lowercase(),
                        )
                    }.executes { ctx ->
                        val player = ctx.source.sender as? Player
                        if (player == null) {
                            ctx.source.sender.sendActionBar(
                                Utils.MM.deserialize("<red>This command can only be used by players."),
                            )
                            return@executes 0
                        }
                        KingdomGui.open(player)
                        1
                    }.then(
                        Commands
                            .literal("create")
                            .requires {
                                it.sender.hasPermission(
                                    "${instance.javaClass.simpleName}.command.kingdom.admin".lowercase(),
                                )
                            }.then(
                                Commands
                                    .argument("owner", ArgumentTypes.player())
                                    .executes { ctx ->
                                        val resolver =
                                            ctx.getArgument("owner", PlayerSelectorArgumentResolver::class.java)
                                        val player = resolver.resolve(ctx.source).firstOrNull()
                                        if (player == null) {
                                            ctx.source.sender.sendActionBar(
                                                Utils.MM.deserialize("<red>Player not found."),
                                            )
                                            return@executes 0
                                        }
                                        val kingdomData =
                                            KingdomData(
                                                name = player.displayName().append(Utils.MM.deserialize("'s Kingdom")),
                                                owner = player.uniqueId,
                                            )
                                        KingdomData.saveKingdom(kingdomData)
                                        ctx.source.sender.sendActionBar(
                                            Utils.MM.deserialize("<green>Kingdom created for ${player.name}."),
                                        )
                                        1
                                    },
                            ),
                    ).then(
                        Commands
                            .literal("delete")
                            .requires {
                                it.sender.hasPermission(
                                    "${instance.javaClass.simpleName}.command.kingdom.admin".lowercase(),
                                )
                            }.then(
                                Commands
                                    .argument("owner", StringArgumentType.string())
                                    .suggests { ctx, builder ->
                                        KingdomData
                                            .getKingdoms()
                                            .map { it.owner.toString() }
                                            .filter { it.startsWith(builder.remaining, ignoreCase = true) }
                                            .forEach { builder.suggest(it) }
                                        builder.buildFuture()
                                    }.executes { ctx ->
                                        val input = ctx.getArgument("owner", String::class.java)
                                        val owner = runCatching { UUID.fromString(input) }.getOrNull()
                                        if (owner == null) {
                                            ctx.source.sender.sendActionBar(
                                                Utils.MM.deserialize("<red>Invalid UUID format."),
                                            )
                                            return@executes 0
                                        }
                                        if (KingdomData.getKingdom(owner) == null) {
                                            ctx.source.sender.sendActionBar(
                                                Utils.MM.deserialize("<red>No kingdom found for this owner."),
                                            )
                                            return@executes 0
                                        }
                                        KingdomData.deleteKingdom(owner)
                                        ctx.source.sender.sendActionBar(
                                            Utils.MM.deserialize("<red>Kingdom deleted."),
                                        )
                                        1
                                    },
                            ),
                    ).then(
                        Commands
                            .literal("invite")
                            .requires {
                                it.sender.hasPermission(
                                    "${instance.javaClass.simpleName}.command.kingdom".lowercase(),
                                )
                            }.executes { ctx ->
                                val player = ctx.source.sender as? Player
                                if (player == null) {
                                    ctx.source.sender.sendActionBar(
                                        Utils.MM.deserialize("<red>This command can only be used by players."),
                                    )
                                    return@executes 0
                                }
                                if (KingdomData.getKingdom(player.uniqueId) == null) {
                                    ctx.source.sender.sendActionBar(
                                        Utils.MM.deserialize("<red>You are not in a kingdom."),
                                    )
                                    return@executes 0
                                }
                                startInviteMode(player)
                                1
                            },
                    ),
                "Kingdom management command.",
                listOf("k"),
            ),
        )

    override val perms: List<Permission> =
        listOf(
            Permission(
                "${instance.javaClass.simpleName}.command.kingdom".lowercase(),
                "Allows using the /kingdom command.",
                PermissionDefault.TRUE,
            ),
            Permission(
                "${instance.javaClass.simpleName}.command.kingdom.admin".lowercase(),
                "Allows using /kingdom create <owner> and /kingdom delete <owner>.",
                PermissionDefault.OP,
            ),
        )

    @EventHandler(ignoreCancelled = true)
    fun on(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (player.uniqueId !in inviteMode) return

        inviteModeRemove(player.uniqueId)

        val uuid = event.rightClicked.uniqueId
        val owner = player.uniqueId

        when (val target = event.rightClicked) {
            is Player -> {
                KingdomData.addMember(owner, uuid)
                player.sendActionBar(
                    Utils.MM.deserialize("<green>${target.displayName()} joined your kingdom."),
                )
            }

            is Villager -> {
                KingdomData.addNpc(owner, uuid)
                player.sendActionBar(
                    Utils.MM.deserialize("<green>${target.name} joined your kingdom."),
                )
            }

            else -> return
        }
        event.isCancelled = true
    }

    @EventHandler
    fun on(event: EntityDeathEvent) {
        if (event.entityType != EntityType.VILLAGER) return
        val villager = event.entity as Villager
        val uuid = event.entity.uniqueId
        if (KingdomData.getKingdoms().any { it.npcs.contains(uuid) }) {
            DeadNpcData(
                uuid = uuid,
                location = event.entity.location,
                profession = villager.profession,
                type = villager.villagerType,
                level = villager.villagerLevel,
            ).also {
                DeadNpcData.registry[uuid] = it
                DeadNpcData.save(it)
            }
        }
    }

    /**
     * Starts invite mode for a player — for a limited time, right-clicking a player or villager adds them to the player's kingdom.
     *
     * @param player The player to start invite mode for.
     */
    private fun startInviteMode(player: Player) {
        inviteModeCancel(player.uniqueId)
        player.sendActionBar(
            Utils.MM.deserialize("<yellow>Invite mode active. Right-click a player or villager to invite them."),
        )
        inviteMode[player.uniqueId] =
            instance.server.scheduler.runTaskLater(
                instance,
                Runnable {
                    inviteModeRemove(player.uniqueId)
                    player.sendActionBar(
                        Utils.MM.deserialize("<red>Invite mode expired."),
                    )
                },
                200L,
            )
    }

    /**
     * Removes a player from invite mode and cancels their task.
     */
    private fun inviteModeRemove(uuid: UUID) {
        inviteMode.remove(uuid)?.cancel()
    }

    /** Loads dead NPC state from the database into memory. */
    fun onEnable() {
        DeadNpcData.loadAll()
    }

    /**
     * Cancels an existing invite-mode task if one is running.
     */
    private fun inviteModeCancel(uuid: UUID) {
        inviteMode[uuid]?.cancel()
    }

    override fun onDisable() {
        inviteMode.values.forEach { it.cancel() }
        inviteMode.clear()
    }
}
