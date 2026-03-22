package it.curzel.tamahero.auth

import java.util.prefs.Preferences

class TokenStorageDesktop : TokenStorage {
    private val prefs = Preferences.userNodeForPackage(TokenStorageDesktop::class.java)

    companion object {
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_SAVED_AT = "auth_saved_at"
    }

    override fun saveCredentials(userId: Long, username: String, token: String) {
        prefs.putLong(KEY_USER_ID, userId)
        prefs.put(KEY_USERNAME, username)
        prefs.put(KEY_TOKEN, token)
        prefs.putLong(KEY_SAVED_AT, System.currentTimeMillis())
        prefs.flush()
        println("[AUTH] Token for $username (id=$userId): $token")
    }

    override fun loadCredentials(): AuthCredentials? {
        val userId = prefs.getLong(KEY_USER_ID, -1)
        if (userId == -1L) return null
        val username = prefs.get(KEY_USERNAME, null) ?: return null
        val token = prefs.get(KEY_TOKEN, null) ?: return null
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        println("[AUTH] Loaded token for $username (id=$userId): $token")
        return AuthCredentials(userId = userId, username = username, token = token, savedAt = savedAt)
    }

    override fun clearCredentials() {
        prefs.remove(KEY_USER_ID)
        prefs.remove(KEY_USERNAME)
        prefs.remove(KEY_TOKEN)
        prefs.remove(KEY_SAVED_AT)
        prefs.flush()
    }
}
