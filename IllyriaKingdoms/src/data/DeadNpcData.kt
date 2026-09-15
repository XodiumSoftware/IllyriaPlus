package org.xodium.illyriakingdoms.data

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Villager
import org.xodium.illyriakingdoms.IllyriaKingdoms.Companion.instance
import java.util.UUID

/**
 * Data captured from a villager NPC at the moment of death, used to restore it on resurrection.
 *
 * @property uuid the UUID of the dead villager.
 * @property location the location where the villager died.
 * @property profession the villager's profession.
 * @property type the villager's biome type.
 * @property level the villager's experience level (1–5).
 */
@Suppress("UnstableApiUsage")
internal data class DeadNpcData(
    val uuid: UUID,
    val location: Location,
    val profession: Villager.Profession,
    val type: Villager.Type,
    val level: Int,
) {
    companion object {
        /** In-memory registry of dead NPCs, keyed by their original villager UUID. */
        val registry = mutableMapOf<UUID, DeadNpcData>()

        /** Raw row data from the database, used to defer [Location] construction to the main thread. */
        private data class RawEntry(
            val uuid: UUID,
            val worldName: String,
            val x: Double,
            val y: Double,
            val z: Double,
            val professionKey: String,
            val typeKey: String,
            val level: Int,
        )

        /** Loads all dead NPC entries from the database into [registry] asynchronously. */
        fun loadAll() {
            DatabaseManager.asyncQuery(
                {
                    DatabaseManager.query("SELECT * FROM dead_npcs") { rs ->
                        RawEntry(
                            uuid = UUID.fromString(rs.getString("uuid")),
                            worldName = rs.getString("world"),
                            x = rs.getDouble("x"),
                            y = rs.getDouble("y"),
                            z = rs.getDouble("z"),
                            professionKey = rs.getString("profession"),
                            typeKey = rs.getString("type"),
                            level = rs.getInt("level"),
                        )
                    }
                },
            ) { rows ->
                rows.forEach { row ->
                    val world = instance.server.getWorld(row.worldName) ?: return@forEach
                    DeadNpcData(
                        uuid = row.uuid,
                        location = Location(world, row.x, row.y, row.z),
                        profession =
                            RegistryAccess
                                .registryAccess()
                                .getRegistry(RegistryKey.VILLAGER_PROFESSION)
                                .getOrThrow(NamespacedKey.minecraft(row.professionKey)),
                        type =
                            RegistryAccess
                                .registryAccess()
                                .getRegistry(RegistryKey.VILLAGER_TYPE)
                                .getOrThrow(NamespacedKey.minecraft(row.typeKey)),
                        level = row.level,
                    ).also { registry[it.uuid] = it }
                }
            }
        }

        /** Persists a single [DeadNpcData] entry to the database asynchronously. */
        fun save(entry: DeadNpcData) {
            val uuidString = entry.uuid.toString()
            val worldName = entry.location.world.name
            val x = entry.location.x
            val y = entry.location.y
            val z = entry.location.z
            val professionKey =
                RegistryAccess
                    .registryAccess()
                    .getRegistry(RegistryKey.VILLAGER_PROFESSION)
                    .getKey(entry.profession)
                    ?.value() ?: "NONE"
            val typeKey =
                RegistryAccess
                    .registryAccess()
                    .getRegistry(RegistryKey.VILLAGER_TYPE)
                    .getKey(entry.type)
                    ?.value() ?: "PLAINS"
            val level = entry.level
            DatabaseManager.asyncQuery<Unit>(
                {
                    DatabaseManager.execute(
                        "INSERT INTO dead_npcs (uuid, world, x, y, z, profession, type, level) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        uuidString,
                        worldName,
                        x,
                        y,
                        z,
                        professionKey,
                        typeKey,
                        level,
                    )
                },
            )
        }

        /** Removes a single dead NPC entry from the database asynchronously. */
        fun delete(uuid: UUID) {
            DatabaseManager.asyncQuery<Unit>(
                {
                    DatabaseManager.execute("DELETE FROM dead_npcs WHERE uuid = ?", uuid.toString())
                },
            )
        }
    }
}
