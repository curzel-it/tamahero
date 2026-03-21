package it.curzel.tamahero.db

import java.sql.Connection
import java.sql.DriverManager

object Database {
    private var connection: Connection? = null

    fun getConnection(): Connection =
        connection ?: throw IllegalStateException("Database not initialized")

    fun init(dbPath: String = "tamahero.db") {
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        createTables()
        DatabaseMigrations.run(getConnection())
    }

    @Synchronized
    fun initInMemory() {
        connection?.close()
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        createTables()
    }

    @Synchronized
    fun close() {
        connection?.close()
        connection = null
    }

    private fun createTables() {
        val conn = getConnection()
        conn.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT,
                    email TEXT,
                    is_admin INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0,
                    last_login_at INTEGER,
                    password_needs_rehash INTEGER NOT NULL DEFAULT 0
                )
            """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS heroes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL REFERENCES users(id),
                    name TEXT NOT NULL,
                    level INTEGER NOT NULL DEFAULT 1,
                    experience INTEGER NOT NULL DEFAULT 0,
                    strength INTEGER NOT NULL DEFAULT 10,
                    agility INTEGER NOT NULL DEFAULT 10,
                    intelligence INTEGER NOT NULL DEFAULT 10,
                    endurance INTEGER NOT NULL DEFAULT 10,
                    created_at INTEGER NOT NULL
                )
            """)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS actions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    hero_id INTEGER NOT NULL REFERENCES heroes(id),
                    type TEXT NOT NULL,
                    started_at INTEGER NOT NULL,
                    completes_at INTEGER NOT NULL,
                    completed INTEGER NOT NULL DEFAULT 0
                )
            """)
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
    }
}
