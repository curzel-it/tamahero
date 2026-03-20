package it.curzel.tamahero.db

data class UserRecord(val id: Long, val username: String, val passwordHash: String)

object UserRepository {

    fun createUser(username: String, passwordHash: String): Long {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            "INSERT INTO users (username, password_hash) VALUES (?, ?)",
            java.sql.Statement.RETURN_GENERATED_KEYS
        )
        stmt.use {
            it.setString(1, username)
            it.setString(2, passwordHash)
            it.executeUpdate()
            val keys = it.generatedKeys
            keys.next()
            return keys.getLong(1)
        }
    }

    fun findByUsername(username: String): UserRecord? {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement("SELECT id, username, password_hash FROM users WHERE username = ?")
        stmt.use {
            it.setString(1, username)
            val rs = it.executeQuery()
            return if (rs.next()) {
                UserRecord(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"))
            } else null
        }
    }

    fun findById(id: Long): UserRecord? {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement("SELECT id, username, password_hash FROM users WHERE id = ?")
        stmt.use {
            it.setLong(1, id)
            val rs = it.executeQuery()
            return if (rs.next()) {
                UserRecord(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"))
            } else null
        }
    }
}
