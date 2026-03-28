package it.curzel.tamahero.auth

import kotlinx.browser.window
import kotlinx.datetime.Clock

class TokenStorageWasm : TokenStorage {
    private val storage get() = window.localStorage

    override fun saveCredentials(userId: Long, username: String, token: String) {
        storage.setItem("auth_user_id", userId.toString())
        storage.setItem("auth_username", username)
        storage.setItem("auth_token", token)
        storage.setItem("auth_saved_at", Clock.System.now().toEpochMilliseconds().toString())
    }

    override fun loadCredentials(): AuthCredentials? {
        val userId = storage.getItem("auth_user_id")?.toLongOrNull() ?: return null
        val username = storage.getItem("auth_username") ?: return null
        val token = storage.getItem("auth_token") ?: return null
        val savedAt = storage.getItem("auth_saved_at")?.toLongOrNull() ?: 0L
        return AuthCredentials(userId, username, token, savedAt)
    }

    override fun clearCredentials() {
        storage.removeItem("auth_user_id")
        storage.removeItem("auth_username")
        storage.removeItem("auth_token")
        storage.removeItem("auth_saved_at")
    }
}
