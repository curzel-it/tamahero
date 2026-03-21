package it.curzel.tamahero.auth

data class SocialAuthResult(
    val idToken: String,
    val provider: String,
)

data class PreExchangedCredentials(
    val userId: Long,
    val token: String,
    val username: String,
)

object PreExchangedCredentialsHolder {
    private var _credentials: PreExchangedCredentials? = null

    fun set(credentials: PreExchangedCredentials) {
        _credentials = credentials
    }

    fun take(): PreExchangedCredentials? {
        val creds = _credentials
        _credentials = null
        return creds
    }
}

interface SocialAuthProvider {
    suspend fun signInWithGoogle(): SocialAuthResult?
    suspend fun signInWithApple(): SocialAuthResult?
    fun isGoogleAvailable(): Boolean
    fun isAppleAvailable(): Boolean
}

object SocialAuthProviderHolder {
    private var _instance: SocialAuthProvider? = null

    val instance: SocialAuthProvider
        get() = _instance ?: throw IllegalStateException("SocialAuthProviderHolder not initialized")

    fun setProvider(provider: SocialAuthProvider) {
        _instance = provider
    }

    fun isInitialized(): Boolean = _instance != null
}
