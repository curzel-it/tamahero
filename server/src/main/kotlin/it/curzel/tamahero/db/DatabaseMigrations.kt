package it.curzel.tamahero.db

import org.slf4j.LoggerFactory
import java.sql.Connection

object DatabaseMigrations {
    private val logger = LoggerFactory.getLogger(DatabaseMigrations::class.java)

    fun run(conn: Connection) {
        ensureVersionTable(conn)
        val currentVersion = getVersion(conn)
        logger.info("Database schema version: {}", currentVersion)

        if (currentVersion < 1) migrateV1(conn)
        if (currentVersion < 2) migrateV2(conn)
    }

    private fun ensureVersionTable(conn: Connection) {
        conn.createStatement().use { stmt ->
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)"
            )
            val rs = stmt.executeQuery("SELECT COUNT(*) FROM schema_version")
            rs.next()
            if (rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO schema_version (version) VALUES (0)")
            }
        }
    }

    private fun getVersion(conn: Connection): Int {
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT version FROM schema_version")
            rs.next()
            return rs.getInt(1)
        }
    }

    private fun setVersion(conn: Connection, version: Int) {
        conn.prepareStatement("UPDATE schema_version SET version = ?").use {
            it.setInt(1, version)
            it.executeUpdate()
        }
    }

    private fun tableExists(conn: Connection, name: String): Boolean {
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$name'"
            )
            rs.next()
            return rs.getInt(1) > 0
        }
    }

    private fun columnExists(conn: Connection, table: String, column: String): Boolean {
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("PRAGMA table_info($table)")
            while (rs.next()) {
                if (rs.getString("name") == column) return true
            }
            return false
        }
    }

    private fun migrateV1(conn: Connection) {
        logger.info("Running migration v1: auth system tables")

        if (tableExists(conn, "users")) {
            if (!columnExists(conn, "users", "email")) {
                conn.createStatement().use { it.executeUpdate("ALTER TABLE users ADD COLUMN email TEXT") }
            }
            if (!columnExists(conn, "users", "is_admin")) {
                conn.createStatement().use { it.executeUpdate("ALTER TABLE users ADD COLUMN is_admin INTEGER NOT NULL DEFAULT 0") }
            }
            if (!columnExists(conn, "users", "created_at")) {
                conn.createStatement().use { it.executeUpdate("ALTER TABLE users ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0") }
            }
            if (!columnExists(conn, "users", "last_login_at")) {
                conn.createStatement().use { it.executeUpdate("ALTER TABLE users ADD COLUMN last_login_at INTEGER") }
            }
            if (!columnExists(conn, "users", "password_needs_rehash")) {
                conn.createStatement().use { it.executeUpdate("ALTER TABLE users ADD COLUMN password_needs_rehash INTEGER NOT NULL DEFAULT 1") }
            }
        }

        conn.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS social_logins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL REFERENCES users(id),
                    provider TEXT NOT NULL,
                    provider_user_id TEXT NOT NULL,
                    email TEXT,
                    display_name TEXT,
                    created_at INTEGER NOT NULL,
                    UNIQUE(provider, provider_user_id)
                )
            """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auth_tokens (
                    token TEXT PRIMARY KEY,
                    user_id INTEGER NOT NULL REFERENCES users(id),
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
            """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS password_reset_tokens (
                    token TEXT PRIMARY KEY,
                    user_id INTEGER NOT NULL REFERENCES users(id),
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
            """)
        }

        setVersion(conn, 1)
        logger.info("Migration v1 complete")
    }

    private fun migrateV2(conn: Connection) {
        logger.info("Running migration v2: villages table")

        conn.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS villages (
                    user_id INTEGER PRIMARY KEY REFERENCES users(id),
                    state_json TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """)
        }

        setVersion(conn, 2)
        logger.info("Migration v2 complete")
    }
}
