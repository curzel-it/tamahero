package it.curzel.tamahero.auth

interface WebAuthProvider {
    fun startAuth(url: String, callbackScheme: String, completion: (String?) -> Unit)
}

object WebAuthProviderHolder {
    private var _instance: WebAuthProvider? = null

    val instance: WebAuthProvider
        get() = _instance ?: throw IllegalStateException("WebAuthProviderHolder not initialized")

    fun setProvider(provider: WebAuthProvider) {
        _instance = provider
    }

    fun isInitialized(): Boolean = _instance != null
}
