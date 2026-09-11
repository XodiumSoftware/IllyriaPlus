package org.xodium.illyriakingdoms.data

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
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
                    name = GsonComponentSerializer.gson().deserialize(rs.getString("name")),
                    owner = UUID.fromString(rs.getString("owner")),
                )
            }.firstOrNull()

        /**
         * Persists a [KingdomData] to the database.
         *
         * @param kingdomData the kingdom data to save.
         */
        fun saveKingdom(kingdomData: KingdomData) {
            DatabaseManager.execute(
                "INSERT INTO kingdoms (id, name, owner) VALUES (?, ?, ?)",
                kingdomData.id.toString(),
                GsonComponentSerializer.gson().serialize(kingdomData.name),
                kingdomData.owner.toString(),
            )
        }
    }
}
