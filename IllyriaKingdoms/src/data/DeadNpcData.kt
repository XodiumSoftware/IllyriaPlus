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

        /** Loads all dead NPC entries from the database into [registry]. */
        fun loadAll() {
            DatabaseManager.query("SELECT * FROM dead_npcs") { rs ->
                val world = instance.server.getWorld(rs.getString("world"))
                if (world != null) {
                    DeadNpcData(
                        uuid = UUID.fromString(rs.getString("uuid")),
                        location =
                            Location(
                                world,
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                            ),
                        profession =
                            RegistryAccess
                                .registryAccess()
                                .getRegistry(RegistryKey.VILLAGER_PROFESSION)
                                .getOrThrow(NamespacedKey.minecraft(rs.getString("profession"))),
                        type =
                            RegistryAccess
                                .registryAccess()
                                .getRegistry(RegistryKey.VILLAGER_TYPE)
                                .getOrThrow(NamespacedKey.minecraft(rs.getString("type"))),
                        level = rs.getInt("level"),
                    ).also { registry[it.uuid] = it }
                }
            }
        }

        /** Persists a single [DeadNpcData] entry to the database. */
        fun save(entry: DeadNpcData) {
            DatabaseManager.execute(
                "INSERT INTO dead_npcs (uuid, world, x, y, z, profession, type, level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                entry.uuid.toString(),
                entry.location.world.name,
                entry.location.x,
                entry.location.y,
                entry.location.z,
                RegistryAccess
                    .registryAccess()
                    .getRegistry(RegistryKey.VILLAGER_PROFESSION)
                    .getKey(entry.profession)
                    ?.value() ?: "NONE",
                RegistryAccess
                    .registryAccess()
                    .getRegistry(RegistryKey.VILLAGER_TYPE)
                    .getKey(entry.type)
                    ?.value() ?: "PLAINS",
                entry.level,
            )
        }

        /** Removes a single dead NPC entry from the database. */
        fun delete(uuid: UUID) {
            DatabaseManager.execute("DELETE FROM dead_npcs WHERE uuid = ?", uuid.toString())
        }
    }
}
