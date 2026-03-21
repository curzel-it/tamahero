package it.curzel.tamahero.db

import org.slf4j.LoggerFactory

object AuthTokenRepository {
    private val logger = LoggerFactory.getLogger(AuthTokenRepository::class.java)

    fun storeToken(token: String, userId: Long, createdAt: Long, expiresAt: Long) {
        val conn = Database.getConnection()
        conn.prepareStatement(
            "INSERT INTO auth_tokens (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)"
        ).use {
            it.setString(1, token)
            it.setLong(2, userId)
            it.setLong(3, createdAt)
            it.setLong(4, expiresAt)
            it.executeUpdate()
        }
    }

    fun validateToken(token: String): Long? {
        val conn = Database.getConnection()
        val now = System.currentTimeMillis()
        conn.prepareStatement("SELECT user_id, expires_at FROM auth_tokens WHERE token = ?").use {
            it.setString(1, token)
            val rs = it.executeQuery()
            if (!rs.next()) return null
            val expiresAt = rs.getLong("expires_at")
            val userId = rs.getLong("user_id")
            return if (expiresAt > now) {
                userId
            } else {
                deleteToken(token)
                null
            }
        }
    }

    fun deleteToken(token: String) {
        val conn = Database.getConnection()
        conn.prepareStatement("DELETE FROM auth_tokens WHERE token = ?").use {
            it.setString(1, token)
            it.executeUpdate()
        }
    }

    fun deleteTokensByUser(userId: Long) {
        val conn = Database.getConnection()
        conn.prepareStatement("DELETE FROM auth_tokens WHERE user_id = ?").use {
            it.setLong(1, userId)
            it.executeUpdate()
        }
    }

    fun cleanupExpired() {
        val conn = Database.getConnection()
        val now = System.currentTimeMillis()
        conn.prepareStatement("DELETE FROM auth_tokens WHERE expires_at < ?").use {
            it.setLong(1, now)
            val deleted = it.executeUpdate()
            if (deleted > 0) logger.info("Cleaned up {} expired auth tokens", deleted)
        }
    }
}
