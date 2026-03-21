package it.curzel.tamahero.auth

data class AuthCredentials(
    val userId: Long,
    val username: String,
    val token: String,
    val savedAt: Long,
)

interface TokenStorage {
    fun saveCredentials(userId: Long, username: String, token: String)
    fun loadCredentials(): AuthCredentials?
    fun clearCredentials()
}

object TokenStorageProvider {
    private var _instance: TokenStorage? = null

    val instance: TokenStorage
        get() = _instance ?: throw IllegalStateException("TokenStorageProvider not initialized")

    fun setProvider(storage: TokenStorage) {
        _instance = storage
    }

    fun isInitialized(): Boolean = _instance != null
}
