package it.curzel.tamahero.auth

import android.content.Context
import android.content.SharedPreferences

class TokenStorageAndroid(context: Context) : TokenStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "tamahero_auth"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_TOKEN = "token"
        private const val KEY_SAVED_AT = "saved_at"
    }

    override fun saveCredentials(userId: Long, username: String, token: String) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_TOKEN, token)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    override fun loadCredentials(): AuthCredentials? {
        val userId = prefs.getLong(KEY_USER_ID, -1)
        if (userId == -1L) return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        return AuthCredentials(userId = userId, username = username, token = token, savedAt = savedAt)
    }

    override fun clearCredentials() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_TOKEN)
            .remove(KEY_SAVED_AT)
            .apply()
    }
}
