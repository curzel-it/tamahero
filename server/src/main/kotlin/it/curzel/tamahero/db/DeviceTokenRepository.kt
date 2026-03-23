package it.curzel.tamahero.db

object DeviceTokenRepository {

    fun saveToken(userId: Long, token: String, platform: String) {
        val conn = Database.getConnection()
        val now = System.currentTimeMillis()
        conn.prepareStatement(
            """INSERT INTO device_tokens (user_id, token, platform, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?)
               ON CONFLICT(token) DO UPDATE SET user_id = ?, platform = ?, updated_at = ?"""
        ).use { stmt ->
            stmt.setLong(1, userId)
            stmt.setString(2, token)
            stmt.setString(3, platform)
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.setLong(6, userId)
            stmt.setString(7, platform)
            stmt.setLong(8, now)
            stmt.executeUpdate()
        }
    }

    fun getTokensForUser(userId: Long): List<String> {
        val conn = Database.getConnection()
        conn.prepareStatement("SELECT token FROM device_tokens WHERE user_id = ?").use { stmt ->
            stmt.setLong(1, userId)
            val rs = stmt.executeQuery()
            val tokens = mutableListOf<String>()
            while (rs.next()) {
                tokens.add(rs.getString("token"))
            }
            return tokens
        }
    }

    fun removeToken(token: String) {
        val conn = Database.getConnection()
        conn.prepareStatement("DELETE FROM device_tokens WHERE token = ?").use { stmt ->
            stmt.setString(1, token)
            stmt.executeUpdate()
        }
    }

    fun removeAllTokensForUser(userId: Long) {
        val conn = Database.getConnection()
        conn.prepareStatement("DELETE FROM device_tokens WHERE user_id = ?").use { stmt ->
            stmt.setLong(1, userId)
            stmt.executeUpdate()
        }
    }
}
