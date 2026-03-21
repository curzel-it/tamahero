package it.curzel.tamahero.auth

import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970

class TokenStorageIos : TokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    companion object {
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USERNAME = "auth_username"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_SAVED_AT = "auth_saved_at"
    }

    override fun saveCredentials(userId: Long, username: String, token: String) {
        defaults.setInteger(userId, KEY_USER_ID)
        defaults.setObject(username, KEY_USERNAME)
        defaults.setObject(token, KEY_TOKEN)
        defaults.setDouble(currentTimeMillis().toDouble(), KEY_SAVED_AT)
        defaults.synchronize()
    }

    override fun loadCredentials(): AuthCredentials? {
        val userId = defaults.integerForKey(KEY_USER_ID)
        if (userId == 0L) return null
        val username = defaults.stringForKey(KEY_USERNAME) ?: return null
        val token = defaults.stringForKey(KEY_TOKEN) ?: return null
        val savedAt = defaults.doubleForKey(KEY_SAVED_AT).toLong()
        return AuthCredentials(userId = userId, username = username, token = token, savedAt = savedAt)
    }

    override fun clearCredentials() {
        defaults.removeObjectForKey(KEY_USER_ID)
        defaults.removeObjectForKey(KEY_USERNAME)
        defaults.removeObjectForKey(KEY_TOKEN)
        defaults.removeObjectForKey(KEY_SAVED_AT)
        defaults.synchronize()
    }

    private fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
}
