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
                    "SELECT * FROM kingdoms WHERE owner = ?",
                    owner.toString(),
                ) { rs ->
                    KingdomData(
                        id = UUID.fromString(rs.getString("id")),
                        name = MM.deserialize(rs.getString("name")),
                        owner = UUID.fromString(rs.getString("owner")),
                        members = rs.getString("members")?.let { parseUuidSet(it) } ?: emptySet(),
                        npcs = rs.getString("npcs")?.let { parseUuidSet(it) } ?: emptySet(),
                    )
                }.firstOrNull()

        /**
         * Retrieves all [KingdomData] from the database.
         *
         * @return a list of all kingdoms.
         */
        fun getKingdoms(): List<KingdomData> =
            DatabaseManager.query("SELECT * FROM kingdoms") { rs ->
                KingdomData(
                    id = UUID.fromString(rs.getString("id")),
                    name = MM.deserialize(rs.getString("name")),
                    owner = UUID.fromString(rs.getString("owner")),
                    members = rs.getString("members")?.let { parseUuidSet(it) } ?: emptySet(),
                    npcs = rs.getString("npcs")?.let { parseUuidSet(it) } ?: emptySet(),
                )
            }

        /**
         * Persists a [KingdomData] to the database.
         *
         * @param kingdomData the kingdom data to save.
         */
        fun saveKingdom(kingdomData: KingdomData) {
            DatabaseManager.execute(
                "INSERT INTO kingdoms (id, name, owner, members, npcs) VALUES (?, ?, ?, ?, ?)",
                kingdomData.id.toString(),
                MM.serialize(kingdomData.name),
                kingdomData.owner.toString(),
                formatUuidSet(kingdomData.members),
                formatUuidSet(kingdomData.npcs),
            )
        }

        /**
         * Deletes the [KingdomData] associated with the given owner from the database.
         *
         * @param owner the UUID of the kingdom owner.
         */
        fun deleteKingdom(owner: UUID) {
            DatabaseManager.execute("DELETE FROM kingdoms WHERE owner = ?", owner.toString())
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
            val updated = kingdom.members + member
            DatabaseManager.execute(
                "UPDATE kingdoms SET members = ? WHERE owner = ?",
                formatUuidSet(updated),
                owner.toString(),
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
            val updated = kingdom.npcs + npc
            DatabaseManager.execute(
                "UPDATE kingdoms SET npcs = ? WHERE owner = ?",
                formatUuidSet(updated),
                owner.toString(),
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
            val updated = kingdom.members - member
            DatabaseManager.execute(
                "UPDATE kingdoms SET members = ? WHERE owner = ?",
                formatUuidSet(updated),
                owner.toString(),
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
            val updated = kingdom.npcs - npc
            DatabaseManager.execute(
                "UPDATE kingdoms SET npcs = ? WHERE owner = ?",
                formatUuidSet(updated),
                owner.toString(),
            )
        }

        /**
         * Serializes a set of UUIDs to a comma-separated string for database storage.
         */
        private fun formatUuidSet(set: Set<UUID>): String = set.joinToString(",") { it.toString() }

        /**
         * Deserializes a comma-separated string of UUIDs from the database into a set.
         */
        private fun parseUuidSet(string: String): Set<UUID> =
            string.split(",").mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }.toSet()

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
