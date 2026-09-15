package org.xodium.illyriakingdoms.data

import net.kyori.adventure.text.Component
import org.xodium.illyriakingdoms.Utils.MM
import java.util.UUID

/**
 * Represents a kingdom.
 *
 * @property id the unique identifier of the kingdom.
 * @property name the display name of the kingdom.
 * @property owner the unique identifier of the kingdom's owner.
 * @property members the set of member UUIDs belonging to the kingdom.
 * @property npcs the set of NPC UUIDs belonging to the kingdom.
 */
internal data class KingdomData(
    val id: UUID = UUID.randomUUID(),
    val name: Component,
    val owner: UUID,
    val members: Set<UUID> = emptySet(),
    val npcs: Set<UUID> = emptySet(),
) {
    companion object {
        /**
         * Retrieves a [KingdomData] by its owner from the database asynchronously.
         *
         * @param owner the UUID of the kingdom owner.
         * @param callback invoked on the main thread with the [KingdomData] if found, or `null`.
         */
        fun getKingdom(
            owner: UUID,
            callback: (KingdomData?) -> Unit = {},
        ) {
            DatabaseManager.asyncQuery(
                {
                    DatabaseManager
                        .query(
                            """
                            SELECT k.*,
                                   GROUP_CONCAT(DISTINCT km.member_uuid) as member_uuids,
                                   GROUP_CONCAT(DISTINCT kn.npc_uuid) as npc_uuids
                            FROM kingdoms k
                            LEFT JOIN kingdom_members km ON k.id = km.kingdom_id
                            LEFT JOIN kingdom_npcs kn ON k.id = kn.kingdom_id
                            WHERE k.owner = ?
                            GROUP BY k.id
                            """.trimIndent(),
                            owner.toString(),
                        ) { rs ->
                            KingdomData(
                                id = UUID.fromString(rs.getString("id")),
                                name = MM.deserialize(rs.getString("name")),
                                owner = UUID.fromString(rs.getString("owner")),
                                members = rs.getString("member_uuids")?.let { parseUuidSet(it) } ?: emptySet(),
                                npcs = rs.getString("npc_uuids")?.let { parseUuidSet(it) } ?: emptySet(),
                            )
                        }.firstOrNull()
                },
                callback,
            )
        }

        /**
         * Retrieves all [KingdomData] from the database asynchronously.
         *
         * @param callback invoked on the main thread with the list of all kingdoms.
         */
        fun getKingdoms(callback: (List<KingdomData>) -> Unit = {}) {
            DatabaseManager.asyncQuery(
                {
                    DatabaseManager.query(
                        """
                        SELECT k.*,
                               GROUP_CONCAT(DISTINCT km.member_uuid) as member_uuids,
                               GROUP_CONCAT(DISTINCT kn.npc_uuid) as npc_uuids
                        FROM kingdoms k
                        LEFT JOIN kingdom_members km ON k.id = km.kingdom_id
                        LEFT JOIN kingdom_npcs kn ON k.id = kn.kingdom_id
                        GROUP BY k.id
                        """.trimIndent(),
                    ) { rs ->
                        KingdomData(
                            id = UUID.fromString(rs.getString("id")),
                            name = MM.deserialize(rs.getString("name")),
                            owner = UUID.fromString(rs.getString("owner")),
                            members = rs.getString("member_uuids")?.let { parseUuidSet(it) } ?: emptySet(),
                            npcs = rs.getString("npc_uuids")?.let { parseUuidSet(it) } ?: emptySet(),
                        )
                    }
                },
                callback,
            )
        }

        /**
         * Retrieves the [KingdomData] that the given player belongs to,
         * whether they are the owner or a member.
         *
         * @param player the UUID of the player to look up.
         * @param callback invoked on the main thread with the [KingdomData] if found, or `null`.
         */
        fun getKingdomByPlayer(
            player: UUID,
            callback: (KingdomData?) -> Unit = {},
        ) {
            DatabaseManager.asyncQuery(
                {
                    DatabaseManager
                        .query(
                            """
                            SELECT k.*,
                                   GROUP_CONCAT(DISTINCT km.member_uuid) as member_uuids,
                                   GROUP_CONCAT(DISTINCT kn.npc_uuid) as npc_uuids
                            FROM kingdoms k
                            LEFT JOIN kingdom_members km ON k.id = km.kingdom_id
                            LEFT JOIN kingdom_npcs kn ON k.id = kn.kingdom_id
                            WHERE k.owner = ?
                               OR k.id IN (SELECT kingdom_id FROM kingdom_members WHERE member_uuid = ?)
                            GROUP BY k.id
                            """.trimIndent(),
                            player.toString(),
                            player.toString(),
                        ) { rs ->
                            KingdomData(
                                id = UUID.fromString(rs.getString("id")),
                                name = MM.deserialize(rs.getString("name")),
                                owner = UUID.fromString(rs.getString("owner")),
                                members = rs.getString("member_uuids")?.let { parseUuidSet(it) } ?: emptySet(),
                                npcs = rs.getString("npc_uuids")?.let { parseUuidSet(it) } ?: emptySet(),
                            )
                        }.firstOrNull()
                },
                callback,
            )
        }

        /**
         * Persists a [KingdomData] to the database asynchronously.
         *
         * @param kingdomData the kingdom data to save.
         * @param onComplete invoked on the main thread after the operation completes.
         */
        fun saveKingdom(
            kingdomData: KingdomData,
            onComplete: () -> Unit = {},
        ) {
            DatabaseManager.asyncQuery<Unit>(
                {
                    DatabaseManager.execute(
                        "INSERT INTO kingdoms (id, name, owner) VALUES (?, ?, ?)",
                        kingdomData.id.toString(),
                        MM.serialize(kingdomData.name),
                        kingdomData.owner.toString(),
                    )

                    kingdomData.members.forEach { member ->
                        DatabaseManager.execute(
                            "INSERT INTO kingdom_members (kingdom_id, member_uuid) VALUES (?, ?)",
                            kingdomData.id.toString(),
                            member.toString(),
                        )
                    }

                    kingdomData.npcs.forEach { npc ->
                        DatabaseManager.execute(
                            "INSERT INTO kingdom_npcs (kingdom_id, npc_uuid) VALUES (?, ?)",
                            kingdomData.id.toString(),
                            npc.toString(),
                        )
                    }
                },
                { onComplete() },
            )
        }

        /**
         * Deletes the [KingdomData] associated with the given owner from the database asynchronously.
         *
         * @param owner the UUID of the kingdom owner.
         * @param onComplete invoked on the main thread after the operation completes.
         */
        fun deleteKingdom(
            owner: UUID,
            onComplete: () -> Unit = {},
        ) {
            DatabaseManager.asyncQuery<Unit>(
                {
                    val kingdom = getKingdomBlocking(owner) ?: return@asyncQuery
                    DatabaseManager.execute("DELETE FROM kingdoms WHERE id = ?", kingdom.id.toString())
                },
                { onComplete() },
            )
        }

        /**
         * Adds a member to a kingdom owned by the given owner asynchronously.
         *
         * @param owner the UUID of the kingdom owner.
         * @param member the UUID of the member to add.
         * @param onComplete invoked on the main thread after the operation completes.
         */
        fun addMember(
            owner: UUID,
            member: UUID,
            onComplete: () -> Unit = {},
        ) {
            DatabaseManager.asyncQuery<Unit>(
                {
                    val kingdom = getKingdomBlocking(owner) ?: return@asyncQuery
                    DatabaseManager.execute(
                        "INSERT OR IGNORE INTO kingdom_members (kingdom_id, member_uuid) VALUES (?, ?)",
                        kingdom.id.toString(),
                        member.toString(),
                    )
                },
                { onComplete() },
            )
        }

        /**
         * Adds an NPC to a kingdom owned by the given owner asynchronously.
         *
         * @param owner the UUID of the kingdom owner.
         * @param npc the UUID of the NPC to add.
         * @param onComplete invoked on the main thread after the operation completes.
         */
        fun addNpc(
            owner: UUID,
            npc: UUID,
            onComplete: () -> Unit = {},
        ) {
            DatabaseManager.asyncQuery<Unit>(
                {
                    val kingdom = getKingdomBlocking(owner) ?: return@asyncQuery
                    DatabaseManager.execute(
                        "INSERT OR IGNORE INTO kingdom_npcs (kingdom_id, npc_uuid) VALUES (?, ?)",
                        kingdom.id.toString(),
                        npc.toString(),
                    )
                },
                { onComplete() },
            )
        }

        /**
         * Removes a member from a kingdom asynchronously.
         *
         * @param owner the UUID of the kingdom owner.
         * @param member the UUID of the member to remove.
         * @param onComplete invoked on the main thread after the operation completes.
         */
        fun kickMember(
            owner: UUID,
            member: UUID,
            onComplete: () -> Unit = {},
        ) {
            DatabaseManager.asyncQuery<Unit>(
                {
                    val kingdom = getKingdomBlocking(owner) ?: return@asyncQuery
                    DatabaseManager.execute(
                        "DELETE FROM kingdom_members WHERE kingdom_id = ? AND member_uuid = ?",
                        kingdom.id.toString(),
                        member.toString(),
                    )
                },
                { onComplete() },
            )
        }

        /**
         * Removes an NPC from a kingdom asynchronously.
         *
         * @param owner the UUID of the kingdom owner.
         * @param npc the UUID of the NPC to remove.
         * @param onComplete invoked on the main thread after the operation completes.
         */
        fun kickNpc(
            owner: UUID,
            npc: UUID,
            onComplete: () -> Unit = {},
        ) {
            DatabaseManager.asyncQuery<Unit>(
                {
                    val kingdom = getKingdomBlocking(owner) ?: return@asyncQuery
                    DatabaseManager.execute(
                        "DELETE FROM kingdom_npcs WHERE kingdom_id = ? AND npc_uuid = ?",
                        kingdom.id.toString(),
                        npc.toString(),
                    )
                },
                { onComplete() },
            )
        }

        /**
         * Atomically replaces an NPC's UUID in a kingdom (used when an NPC is resurrected
         * and receives a new entity UUID). Both operations run in a single async task
         * to avoid races between separate calls.
         *
         * @param owner the UUID of the kingdom owner.
         * @param oldNpc the UUID of the NPC to remove.
         * @param newNpc the UUID of the NPC to add.
         * @param onComplete invoked on the main thread after the operation completes.
         */
        fun replaceNpc(
            owner: UUID,
            oldNpc: UUID,
            newNpc: UUID,
            onComplete: () -> Unit = {},
        ) {
            DatabaseManager.asyncQuery<Unit>(
                {
                    val kingdom = getKingdomBlocking(owner) ?: return@asyncQuery
                    DatabaseManager.execute(
                        "DELETE FROM kingdom_npcs WHERE kingdom_id = ? AND npc_uuid = ?",
                        kingdom.id.toString(),
                        oldNpc.toString(),
                    )
                    DatabaseManager.execute(
                        "INSERT OR IGNORE INTO kingdom_npcs (kingdom_id, npc_uuid) VALUES (?, ?)",
                        kingdom.id.toString(),
                        newNpc.toString(),
                    )
                },
                { onComplete() },
            )
        }

        /**
         * Deserializes a comma-separated string of UUIDs from the database into a set.
         */
        private fun parseUuidSet(string: String): Set<UUID> =
            string.split(",").mapNotNull { runCatching { UUID.fromString(it.trim()) }.getOrNull() }.toSet()

        /**
         * Updates the name of an existing [KingdomData] in the database asynchronously.
         *
         * @param owner the UUID of the kingdom owner.
         * @param name the new display name of the kingdom.
         * @param onComplete invoked on the main thread after the operation completes.
         */
        fun renameKingdom(
            owner: UUID,
            name: Component,
            onComplete: () -> Unit = {},
        ) {
            DatabaseManager.asyncQuery<Unit>(
                {
                    DatabaseManager.execute(
                        "UPDATE kingdoms SET name = ? WHERE owner = ?",
                        MM.serialize(name),
                        owner.toString(),
                    )
                },
                { onComplete() },
            )
        }

        /**
         * Blocking version of [getKingdom] for internal use within async tasks only.
         */
        private fun getKingdomBlocking(owner: UUID): KingdomData? =
            DatabaseManager
                .query(
                    """
                    SELECT k.*,
                           GROUP_CONCAT(DISTINCT km.member_uuid) as member_uuids,
                           GROUP_CONCAT(DISTINCT kn.npc_uuid) as npc_uuids
                    FROM kingdoms k
                    LEFT JOIN kingdom_members km ON k.id = km.kingdom_id
                    LEFT JOIN kingdom_npcs kn ON k.id = kn.kingdom_id
                    WHERE k.owner = ?
                    GROUP BY k.id
                    """.trimIndent(),
                    owner.toString(),
                ) { rs ->
                    KingdomData(
                        id = UUID.fromString(rs.getString("id")),
                        name = MM.deserialize(rs.getString("name")),
                        owner = UUID.fromString(rs.getString("owner")),
                        members = rs.getString("member_uuids")?.let { parseUuidSet(it) } ?: emptySet(),
                        npcs = rs.getString("npc_uuids")?.let { parseUuidSet(it) } ?: emptySet(),
                    )
                }.firstOrNull()
    }
}
