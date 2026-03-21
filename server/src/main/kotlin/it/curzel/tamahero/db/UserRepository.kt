package it.curzel.tamahero.db

data class UserRecord(
    val id: Long,
    val username: String,
    val passwordHash: String?,
    val email: String? = null,
    val isAdmin: Boolean = false,
    val createdAt: Long = 0,
    val lastLoginAt: Long? = null,
    val passwordNeedsRehash: Boolean = false,
)

object UserRepository {

    private val adminEmails = setOf(
        "federico@curzel.it",
        "saffo.montesi@gmail.com",
    )

    private fun isAdminEmail(email: String?): Boolean =
        email != null && (email.lowercase() in adminEmails || email.lowercase().endsWith("@curzel.it"))

    fun createUser(username: String, passwordHash: String, email: String? = null): Long {
        val conn = Database.getConnection()
        val now = System.currentTimeMillis()
        val stmt = conn.prepareStatement(
            "INSERT INTO users (username, password_hash, email, is_admin, created_at, password_needs_rehash) VALUES (?, ?, ?, ?, ?, 0)",
            java.sql.Statement.RETURN_GENERATED_KEYS
        )
        stmt.use {
            it.setString(1, username)
            it.setString(2, passwordHash)
            it.setString(3, email)
            it.setInt(4, if (isAdminEmail(email)) 1 else 0)
            it.setLong(5, now)
            it.executeUpdate()
            val keys = it.generatedKeys
            keys.next()
            return keys.getLong(1)
        }
    }

    fun createSocialUser(username: String, email: String?): Long? {
        val conn = Database.getConnection()
        val now = System.currentTimeMillis()
        val stmt = conn.prepareStatement(
            "INSERT INTO users (username, email, is_admin, created_at, password_needs_rehash) VALUES (?, ?, ?, ?, 0)",
            java.sql.Statement.RETURN_GENERATED_KEYS
        )
        return try {
            stmt.use {
                it.setString(1, username)
                it.setString(2, email)
                it.setInt(3, if (isAdminEmail(email)) 1 else 0)
                it.setLong(4, now)
                it.executeUpdate()
                val keys = it.generatedKeys
                if (keys.next()) keys.getLong(1) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun findByUsername(username: String): UserRecord? {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            "SELECT id, username, password_hash, email, is_admin, created_at, last_login_at, password_needs_rehash FROM users WHERE username = ?"
        )
        stmt.use {
            it.setString(1, username)
            val rs = it.executeQuery()
            return if (rs.next()) rowToUser(rs) else null
        }
    }

    fun findByEmail(email: String): UserRecord? {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            "SELECT id, username, password_hash, email, is_admin, created_at, last_login_at, password_needs_rehash FROM users WHERE LOWER(email) = LOWER(?)"
        )
        stmt.use {
            it.setString(1, email)
            val rs = it.executeQuery()
            return if (rs.next()) rowToUser(rs) else null
        }
    }

    fun findById(id: Long): UserRecord? {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            "SELECT id, username, password_hash, email, is_admin, created_at, last_login_at, password_needs_rehash FROM users WHERE id = ?"
        )
        stmt.use {
            it.setLong(1, id)
            val rs = it.executeQuery()
            return if (rs.next()) rowToUser(rs) else null
        }
    }

    fun usernameExists(username: String): Boolean {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")
        stmt.use {
            it.setString(1, username)
            val rs = it.executeQuery()
            rs.next()
            return rs.getInt(1) > 0
        }
    }

    fun updateLastLogin(userId: Long) {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement("UPDATE users SET last_login_at = ? WHERE id = ?")
        stmt.use {
            it.setLong(1, System.currentTimeMillis())
            it.setLong(2, userId)
            it.executeUpdate()
        }
    }

    fun updatePasswordHash(userId: Long, hash: String) {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement("UPDATE users SET password_hash = ?, password_needs_rehash = 0 WHERE id = ?")
        stmt.use {
            it.setString(1, hash)
            it.setLong(2, userId)
            it.executeUpdate()
        }
    }

    fun setEmail(userId: Long, email: String) {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement("UPDATE users SET email = ? WHERE id = ?")
        stmt.use {
            it.setString(1, email)
            it.setLong(2, userId)
            it.executeUpdate()
        }
    }

    fun generateUniqueUsername(displayName: String): String {
        val base = displayName
            .replace(Regex("[^a-zA-Z0-9_]"), "")
            .take(15)
            .ifEmpty { "user" }
        if (!usernameExists(base)) return base
        for (i in 1..999) {
            val candidate = "${base}_$i"
            if (!usernameExists(candidate)) return candidate
        }
        return "${base}_${System.currentTimeMillis() % 100000}"
    }

    fun deleteUser(userId: Long) {
        val conn = Database.getConnection()
        conn.prepareStatement("DELETE FROM villages WHERE user_id = ?").use { it.setLong(1, userId); it.executeUpdate() }
        conn.prepareStatement("DELETE FROM auth_tokens WHERE user_id = ?").use { it.setLong(1, userId); it.executeUpdate() }
        conn.prepareStatement("DELETE FROM password_reset_tokens WHERE user_id = ?").use { it.setLong(1, userId); it.executeUpdate() }
        conn.prepareStatement("DELETE FROM social_logins WHERE user_id = ?").use { it.setLong(1, userId); it.executeUpdate() }
        conn.prepareStatement("DELETE FROM actions WHERE hero_id IN (SELECT id FROM heroes WHERE user_id = ?)").use { it.setLong(1, userId); it.executeUpdate() }
        conn.prepareStatement("DELETE FROM heroes WHERE user_id = ?").use { it.setLong(1, userId); it.executeUpdate() }
        conn.prepareStatement("DELETE FROM users WHERE id = ?").use { it.setLong(1, userId); it.executeUpdate() }
    }

    private fun rowToUser(rs: java.sql.ResultSet): UserRecord = UserRecord(
        id = rs.getLong("id"),
        username = rs.getString("username"),
        passwordHash = rs.getString("password_hash"),
        email = rs.getString("email"),
        isAdmin = rs.getInt("is_admin") == 1,
        createdAt = rs.getLong("created_at"),
        lastLoginAt = rs.getLong("last_login_at").let { if (it == 0L) null else it },
        passwordNeedsRehash = rs.getInt("password_needs_rehash") == 1,
    )
}
