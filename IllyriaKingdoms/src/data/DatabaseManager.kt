package org.xodium.illyriakingdoms.data

import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/**
 * Manages the SQLite database connection for the IllyriaKingdoms plugin.
 */
internal object DatabaseManager {
    private lateinit var connection: Connection

    /**
     * Opens the database connection and initializes the schema.
     *
     * @param plugin the plugin instance used to locate the data folder.
     */
    fun init(plugin: JavaPlugin) {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }
        val databasePath =
            plugin
                .dataFolder
                .toPath()
                .resolve("kingdoms.db")
                .toString()
        connection = DriverManager.getConnection("jdbc:sqlite:$databasePath")
        connection.autoCommit = true
        execute(
            """
            CREATE TABLE IF NOT EXISTS kingdoms (
                id    TEXT PRIMARY KEY,
                name  TEXT NOT NULL,
                owner TEXT NOT NULL UNIQUE
            )
            """.trimIndent(),
        )
        execute(
            """
            CREATE TABLE IF NOT EXISTS kingdom_members (
                kingdom_id TEXT NOT NULL,
                member_uuid TEXT NOT NULL,
                PRIMARY KEY (kingdom_id, member_uuid),
                FOREIGN KEY (kingdom_id) REFERENCES kingdoms(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        execute(
            """
            CREATE TABLE IF NOT EXISTS kingdom_npcs (
                kingdom_id TEXT NOT NULL,
                npc_uuid TEXT NOT NULL,
                PRIMARY KEY (kingdom_id, npc_uuid),
                FOREIGN KEY (kingdom_id) REFERENCES kingdoms(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        execute(
            """
            CREATE TABLE IF NOT EXISTS dead_npcs (
                uuid       TEXT PRIMARY KEY,
                world      TEXT NOT NULL,
                x          REAL NOT NULL,
                y          REAL NOT NULL,
                z          REAL NOT NULL,
                profession TEXT NOT NULL,
                type       TEXT NOT NULL,
                level      INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        // Create indexes for better performance
        execute("CREATE INDEX IF NOT EXISTS idx_kingdom_members_kingdom_id ON kingdom_members(kingdom_id)")
        execute("CREATE INDEX IF NOT EXISTS idx_kingdom_members_member_uuid ON kingdom_members(member_uuid)")
        execute("CREATE INDEX IF NOT EXISTS idx_kingdom_npcs_kingdom_id ON kingdom_npcs(kingdom_id)")
        execute("CREATE INDEX IF NOT EXISTS idx_kingdom_npcs_npc_uuid ON kingdom_npcs(npc_uuid)")
        execute("CREATE INDEX IF NOT EXISTS idx_kingdoms_owner ON kingdoms(owner)")

        // Enable foreign key constraints
        execute("PRAGMA foreign_keys = ON")
    }

    /** Closes the database connection if it is open. */
    fun close() {
        if (::connection.isInitialized && !connection.isClosed) {
            connection.close()
        }
    }

    /**
     * Executes an SQL statement (e.g. DDL or DML) with the given parameters.
     *
     * @param sql the SQL statement to execute.
     * @param params optional parameters to bind into the statement.
     */
    fun execute(
        sql: String,
        vararg params: Any,
    ) {
        connection.prepareStatement(sql).use { stmt ->
            params.forEachIndexed { i, param -> stmt.setObject(i + 1, param) }
            stmt.executeUpdate()
        }
    }

    /**
     * Executes a query and maps the [ResultSet] through [mapper].
     *
     * @param sql the SQL query to execute.
     * @param params optional parameters to bind into the query.
     * @param mapper mapping function that transforms the [ResultSet] into the result type [T].
     * @return a list of mapped results.
     */
    fun <T> query(
        sql: String,
        vararg params: Any,
        mapper: (ResultSet) -> T,
    ): List<T> {
        val results = mutableListOf<T>()
        connection.prepareStatement(sql).use { stmt ->
            params.forEachIndexed { i, param -> stmt.setObject(i + 1, param) }
            stmt.executeQuery().use { rs ->
                while (rs.next()) results.add(mapper(rs))
            }
        }
        return results
    }
}
