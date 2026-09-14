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
         * Retrieves a [KingdomData] by its owner from the database.
         *
         * @param owner the UUID of the kingdom owner.
         * @return the [KingdomData] if found, or `null`.
         */
        fun getKingdom(owner: UUID): KingdomData? =
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

        /**
         * Retrieves all [KingdomData] from the database.
         *
         * @return a list of all kingdoms.
         */
        fun getKingdoms(): List<KingdomData> =
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

        /**
         * Persists a [KingdomData] to the database.
         *
         * @param kingdomData the kingdom data to save.
         */
        fun saveKingdom(kingdomData: KingdomData) {
            DatabaseManager.execute(
                "INSERT INTO kingdoms (id, name, owner) VALUES (?, ?, ?)",
                kingdomData.id.toString(),
                MM.serialize(kingdomData.name),
                kingdomData.owner.toString(),
            )

            // Insert members
            kingdomData.members.forEach { member ->
                DatabaseManager.execute(
                    "INSERT INTO kingdom_members (kingdom_id, member_uuid) VALUES (?, ?)",
                    kingdomData.id.toString(),
                    member.toString(),
                )
            }

            // Insert NPCs
            kingdomData.npcs.forEach { npc ->
                DatabaseManager.execute(
                    "INSERT INTO kingdom_npcs (kingdom_id, npc_uuid) VALUES (?, ?)",
                    kingdomData.id.toString(),
                    npc.toString(),
                )
            }
        }

        /**
         * Deletes the [KingdomData] associated with the given owner from the database.
         *
         * @param owner the UUID of the kingdom owner.
         */
        fun deleteKingdom(owner: UUID) {
            val kingdom = getKingdom(owner) ?: return
            DatabaseManager.execute("DELETE FROM kingdoms WHERE id = ?", kingdom.id.toString())
        }

        /**
         * Adds a member to a kingdom owned by the given owner.
         *
         * @param owner the UUID of the kingdom owner.
         * @param member the UUID of the member to add.
         */
        fun addMember(
            owner: UUID,
            member: UUID,
        ) {
            val kingdom = getKingdom(owner) ?: return
            DatabaseManager.execute(
                "INSERT OR IGNORE INTO kingdom_members (kingdom_id, member_uuid) VALUES (?, ?)",
                kingdom.id.toString(),
                member.toString(),
            )
        }

        /**
         * Adds an NPC to a kingdom owned by the given owner.
         *
         * @param owner the UUID of the kingdom owner.
         * @param npc the UUID of the NPC to add.
         */
        fun addNpc(
            owner: UUID,
            npc: UUID,
        ) {
            val kingdom = getKingdom(owner) ?: return
            DatabaseManager.execute(
                "INSERT OR IGNORE INTO kingdom_npcs (kingdom_id, npc_uuid) VALUES (?, ?)",
                kingdom.id.toString(),
                npc.toString(),
            )
        }

        /**
         * Removes a member from a kingdom.
         *
         * @param owner the UUID of the kingdom owner.
         * @param member the UUID of the member to remove.
         */
        fun kickMember(
            owner: UUID,
            member: UUID,
        ) {
            val kingdom = getKingdom(owner) ?: return
            DatabaseManager.execute(
                "DELETE FROM kingdom_members WHERE kingdom_id = ? AND member_uuid = ?",
                kingdom.id.toString(),
                member.toString(),
            )
        }

        /**
         * Removes an NPC from a kingdom.
         *
         * @param owner the UUID of the kingdom owner.
         * @param npc the UUID of the NPC to remove.
         */
        fun kickNpc(
            owner: UUID,
            npc: UUID,
        ) {
            val kingdom = getKingdom(owner) ?: return
            DatabaseManager.execute(
                "DELETE FROM kingdom_npcs WHERE kingdom_id = ? AND npc_uuid = ?",
                kingdom.id.toString(),
                npc.toString(),
            )
        }

        /**
         * Deserializes a comma-separated string of UUIDs from the database into a set.
         */
        private fun parseUuidSet(string: String): Set<UUID> =
            string.split(",").mapNotNull { runCatching { UUID.fromString(it.trim()) }.getOrNull() }.toSet()

        /**
         * Updates the name of an existing [KingdomData] in the database.
         *
         * @param owner the UUID of the kingdom owner.
         * @param name the new display name of the kingdom.
         */
        fun renameKingdom(
            owner: UUID,
            name: Component,
        ) {
            DatabaseManager.execute(
                "UPDATE kingdoms SET name = ? WHERE owner = ?",
                MM.serialize(name),
                owner.toString(),
            )
        }
    }
}
