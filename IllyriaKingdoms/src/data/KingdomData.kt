package org.xodium.illyriakingdoms.data

import org.xodium.illyriakingdoms.Utils.MM
import net.kyori.adventure.text.Component
import java.util.UUID

/**
 * Represents a kingdom.
 *
 * @property id the unique identifier of the kingdom.
 * @property name the display name of the kingdom.
 * @property owner the unique identifier of the kingdom's owner.
 * @property members the set of member UUIDs belonging to the kingdom.
 */
internal data class KingdomData(
    val id: UUID = UUID.randomUUID(),
    val name: Component,
    val owner: UUID,
    val members: Set<UUID> = emptySet(),
) {
    companion object {

        /**
         * Retrieves a [KingdomData] by its owner from the database.
         *
         * @param owner the UUID of the kingdom owner.
         * @return the [KingdomData] if found, or `null`.
         */
        fun getKingdom(owner: UUID): KingdomData? =
            DatabaseManager.query(
                "SELECT * FROM kingdoms WHERE owner = ?",
                owner.toString(),
            ) { rs ->
                KingdomData(
                    id = UUID.fromString(rs.getString("id")),
                    name = MM.deserialize(rs.getString("name")),
                    owner = UUID.fromString(rs.getString("owner")),
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
         * Updates the name of an existing [KingdomData] in the database.
         *
         * @param owner the UUID of the kingdom owner.
         * @param name the new display name of the kingdom.
         */
        fun renameKingdom(owner: UUID, name: Component) {
            DatabaseManager.execute(
                "UPDATE kingdoms SET name = ? WHERE owner = ?",
                MM.serialize(name),
                owner.toString(),
            )
        }
    }
}
