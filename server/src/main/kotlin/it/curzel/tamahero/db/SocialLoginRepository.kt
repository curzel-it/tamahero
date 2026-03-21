package it.curzel.tamahero.db

data class SocialLoginInfo(
    val provider: String,
    val providerUserId: String,
    val email: String?,
    val displayName: String?,
)

object SocialLoginRepository {

    fun addSocialLogin(userId: Long, provider: String, providerUserId: String, email: String?, displayName: String?) {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            "INSERT OR IGNORE INTO social_logins (user_id, provider, provider_user_id, email, display_name, created_at) VALUES (?, ?, ?, ?, ?, ?)"
        )
        stmt.use {
            it.setLong(1, userId)
            it.setString(2, provider)
            it.setString(3, providerUserId)
            it.setString(4, email)
            it.setString(5, displayName)
            it.setLong(6, System.currentTimeMillis())
            it.executeUpdate()
        }
    }

    fun findBySocialLogin(provider: String, providerUserId: String): UserRecord? {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            """SELECT u.id, u.username, u.password_hash, u.email, u.is_admin, u.created_at, u.last_login_at, u.password_needs_rehash
               FROM users u JOIN social_logins s ON u.id = s.user_id
               WHERE s.provider = ? AND s.provider_user_id = ?"""
        )
        stmt.use {
            it.setString(1, provider)
            it.setString(2, providerUserId)
            val rs = it.executeQuery()
            return if (rs.next()) UserRecord(
                id = rs.getLong("id"),
                username = rs.getString("username"),
                passwordHash = rs.getString("password_hash"),
                email = rs.getString("email"),
                isAdmin = rs.getInt("is_admin") == 1,
                createdAt = rs.getLong("created_at"),
                lastLoginAt = rs.getLong("last_login_at").let { v -> if (v == 0L) null else v },
                passwordNeedsRehash = rs.getInt("password_needs_rehash") == 1,
            ) else null
        }
    }

    fun getSocialLogins(userId: Long): List<SocialLoginInfo> {
        val conn = Database.getConnection()
        val stmt = conn.prepareStatement(
            "SELECT provider, provider_user_id, email, display_name FROM social_logins WHERE user_id = ?"
        )
        stmt.use {
            it.setLong(1, userId)
            val rs = it.executeQuery()
            val result = mutableListOf<SocialLoginInfo>()
            while (rs.next()) {
                result.add(SocialLoginInfo(
                    provider = rs.getString("provider"),
                    providerUserId = rs.getString("provider_user_id"),
                    email = rs.getString("email"),
                    displayName = rs.getString("display_name"),
                ))
            }
            return result
        }
    }
}
