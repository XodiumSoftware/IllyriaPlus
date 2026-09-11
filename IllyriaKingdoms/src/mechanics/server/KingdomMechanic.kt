package org.xodium.illyriakingdoms.mechanics.server

import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.xodium.illyriakingdoms.IllyriaKingdoms.Companion.instance
import org.xodium.illyriakingdoms.Utils
import org.xodium.illyriakingdoms.data.CommandData
import org.xodium.illyriakingdoms.data.KingdomData
import org.xodium.illyriakingdoms.gui.KingdomGui
import org.xodium.illyriakingdoms.mechanics.MechanicInterface
import java.util.UUID

/** Represents a mechanic handling kingdom management within the system. */
internal object KingdomMechanic : MechanicInterface {
    override val cmds: Collection<CommandData> =
        listOf(
            CommandData(
                Commands.literal("kingdom")
                    .requires { it.sender.hasPermission("${instance.javaClass.simpleName}.command.kingdom".lowercase()) }
                    .executes { ctx ->
                        val player = ctx.source.sender as? Player
                        if (player == null) {
                            ctx.source.sender.sendActionBar(
                                Utils.MM.deserialize("<red>This command can only be used by players."),
                            )
                            return@executes 0
                        }
                        KingdomGui.open(player)
                        1
                    }
                    .then(
                        Commands.literal("create")
                            .requires { it.sender.hasPermission("${instance.javaClass.simpleName}.command.kingdom.admin".lowercase()) }
                            .then(
                                Commands.argument("owner", ArgumentTypes.player())
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
                                        val kingdomData = KingdomData(
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
                    )
                    .then(
                        Commands.literal("delete")
                            .requires { it.sender.hasPermission("${instance.javaClass.simpleName}.command.kingdom.admin".lowercase()) }
                            .then(
                                Commands.argument("owner", StringArgumentType.string())
                                    .suggests { ctx, builder ->
                                        KingdomData.getKingdoms()
                                            .map { it.owner.toString() }
                                            .filter { it.startsWith(builder.remaining, ignoreCase = true) }
                                            .forEach { builder.suggest(it) }
                                        builder.buildFuture()
                                    }
                                    .executes { ctx ->
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
}
