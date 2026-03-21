package it.curzel.tamahero.db

import org.slf4j.LoggerFactory

object PasswordResetTokenRepository {
    private val logger = LoggerFactory.getLogger(PasswordResetTokenRepository::class.java)

    fun storeToken(token: String, userId: Long, createdAt: Long, expiresAt: Long) {
        val conn = Database.getConnection()
        conn.prepareStatement("DELETE FROM password_reset_tokens WHERE user_id = ?").use {
            it.setLong(1, userId)
            it.executeUpdate()
        }
        conn.prepareStatement(
            "INSERT INTO password_reset_tokens (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)"
        ).use {
            it.setString(1, token)
            it.setLong(2, userId)
            it.setLong(3, createdAt)
            it.setLong(4, expiresAt)
            it.executeUpdate()
        }
    }

    fun findValidToken(token: String): Long? {
        val conn = Database.getConnection()
        val now = System.currentTimeMillis()
        conn.prepareStatement("SELECT user_id, expires_at FROM password_reset_tokens WHERE token = ?").use {
            it.setString(1, token)
            val rs = it.executeQuery()
            if (!rs.next()) return null
            val expiresAt = rs.getLong("expires_at")
            return if (expiresAt > now) rs.getLong("user_id") else null
        }
    }

    fun deleteByUser(userId: Long) {
        val conn = Database.getConnection()
        conn.prepareStatement("DELETE FROM password_reset_tokens WHERE user_id = ?").use {
            it.setLong(1, userId)
            it.executeUpdate()
        }
    }

    fun cleanupExpired() {
        val conn = Database.getConnection()
        val now = System.currentTimeMillis()
        conn.prepareStatement("DELETE FROM password_reset_tokens WHERE expires_at < ?").use {
            it.setLong(1, now)
            val deleted = it.executeUpdate()
            if (deleted > 0) logger.info("Cleaned up {} expired password reset tokens", deleted)
        }
    }
}
