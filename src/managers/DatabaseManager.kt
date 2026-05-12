package org.xodium.illyriaplus.managers

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.xodium.illyriaplus.IllyriaPlus.Companion.instance
import org.xodium.illyriaplus.tables.AnchorTable
import org.xodium.illyriaplus.tables.PlayerTable
import java.io.File

/**
 * Manages the lifecycle and connection of the SQLite database used by the plugin.
 *
 * Responsible for:
 * - Establishing SQLite connection
 * - Creating database schema if missing
 */
internal object DatabaseManager {
    /**
     * Active Exposed database connection.
     */
    lateinit var db: Database
        private set

    /**
     * Initializes the SQLite database connection and ensures schema exists.
     *
     * The database file is stored inside the plugin's data folder.
     */
    fun init() {
        db =
            Database.connect(
                url = "jdbc:sqlite:${
                    File(instance.dataFolder, "illyriaplus.db").apply { parentFile.mkdirs() }.absolutePath
                }",
                driver = "org.sqlite.JDBC",
            )

        transaction(db) {
            SchemaUtils.create(
                PlayerTable,
                AnchorTable,
            )
        }
    }
}
