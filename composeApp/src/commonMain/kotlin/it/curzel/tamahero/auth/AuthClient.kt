package it.curzel.tamahero.auth

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.curzel.tamahero.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class AuthRequest(val username: String, val password: String, val email: String? = null)

@Serializable
private data class ForgotPasswordApiRequest(val email: String)

@Serializable
private data class SocialLoginApiRequest(val idToken: String, val provider: String)

@Serializable
private data class LinkAccountApiRequest(val idToken: String, val provider: String)

@Serializable
private data class AuthResponse(
    val success: Boolean,
    val userId: Long? = null,
    val token: String? = null,
    val username: String? = null,
    val error: String? = null,
)

@Serializable
private data class LinkedAccountEntry(val provider: String)

class AuthClient(
    private val baseUrl: String = ServerConfig.BASE_URL,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val httpClient = HttpClient()
    private val rateLimitMessage = "Too many requests. Please wait a moment and try again."
    private val tokenValidityMs = 90L * 24 * 60 * 60 * 1000
    private val tokenRenewalThresholdMs = 30L * 24 * 60 * 60 * 1000

    private val _state = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        if (!TokenStorageProvider.isInitialized()) return
        val credentials = TokenStorageProvider.instance.loadCredentials() ?: return
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        if (now - credentials.savedAt > tokenValidityMs) {
            TokenStorageProvider.instance.clearCredentials()
            return
        }
        _state.value = AuthState.LoggedIn(
            userId = credentials.userId,
            username = credentials.username,
            token = credentials.token,
        )
        scope.launch { validateAndRenewIfNeeded(credentials) }
    }

    private suspend fun validateAndRenewIfNeeded(credentials: AuthCredentials) {
        try {
            val response = httpClient.get("$baseUrl/api/auth/validate") {
                header("Authorization", "Bearer ${credentials.token}")
            }
            if (response.status == HttpStatusCode.Unauthorized) {
                handleUnauthorized()
                return
            }
            if (response.status != HttpStatusCode.OK) return
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val tokenAge = now - credentials.savedAt
            if (tokenAge > tokenValidityMs - tokenRenewalThresholdMs) {
                renewToken(credentials)
            }
        } catch (_: Exception) {
            // Network error — keep using saved token
        }
    }

    private suspend fun renewToken(credentials: AuthCredentials) {
        try {
            val response = httpClient.post("$baseUrl/api/auth/renew") {
                header("Authorization", "Bearer ${credentials.token}")
            }
            if (response.status != HttpStatusCode.OK) return
            val authResponse = json.decodeFromString<AuthResponse>(response.bodyAsText())
            if (authResponse.success && authResponse.userId != null && authResponse.token != null) {
                val username = authResponse.username ?: credentials.username
                TokenStorageProvider.instance.saveCredentials(authResponse.userId, username, authResponse.token)
                _state.value = AuthState.LoggedIn(userId = authResponse.userId, username = username, token = authResponse.token)
            }
        } catch (_: Exception) {}
    }

    private fun handleUnauthorized() {
        if (TokenStorageProvider.isInitialized()) {
            TokenStorageProvider.instance.clearCredentials()
        }
        _state.value = AuthState.LoggedOut
    }

    suspend fun login(username: String, password: String) {
        _state.value = AuthState.Loading("Logging in...")
        try {
            val requestBody = json.encodeToString(AuthRequest(username = username.trim(), password = password))
            val response: HttpResponse = httpClient.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (response.status == HttpStatusCode.TooManyRequests) {
                _state.value = AuthState.Error(rateLimitMessage); return
            }
            val authResponse = json.decodeFromString<AuthResponse>(response.bodyAsText())
            if (authResponse.success && authResponse.userId != null && authResponse.token != null) {
                if (TokenStorageProvider.isInitialized()) {
                    TokenStorageProvider.instance.saveCredentials(authResponse.userId, username.trim(), authResponse.token)
                }
                _state.value = AuthState.LoggedIn(userId = authResponse.userId, username = username.trim(), token = authResponse.token)
            } else {
                _state.value = AuthState.Error(authResponse.error ?: "Login failed")
            }
        } catch (e: Exception) {
            _state.value = AuthState.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(username: String, password: String, email: String? = null) {
        _state.value = AuthState.Loading("Creating account...")
        try {
            val trimmedEmail = email?.trim()?.ifEmpty { null }
            val requestBody = json.encodeToString(AuthRequest(username = username.trim(), password = password, email = trimmedEmail))
            val response: HttpResponse = httpClient.post("$baseUrl/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (response.status == HttpStatusCode.TooManyRequests) {
                _state.value = AuthState.Error(rateLimitMessage); return
            }
            val authResponse = json.decodeFromString<AuthResponse>(response.bodyAsText())
            if (authResponse.success && authResponse.userId != null && authResponse.token != null) {
                if (TokenStorageProvider.isInitialized()) {
                    TokenStorageProvider.instance.saveCredentials(authResponse.userId, username.trim(), authResponse.token)
                }
                _state.value = AuthState.LoggedIn(userId = authResponse.userId, username = username.trim(), token = authResponse.token)
            } else {
                _state.value = AuthState.Error(authResponse.error ?: "Registration failed")
            }
        } catch (e: Exception) {
            _state.value = AuthState.Error(e.message ?: "Network error")
        }
    }

    suspend fun socialLogin(provider: String) {
        if (!SocialAuthProviderHolder.isInitialized()) {
            _state.value = AuthState.Error("Social login not available on this platform"); return
        }
        _state.value = AuthState.Loading("Signing in...")
        try {
            val socialResult = when (provider) {
                "google" -> SocialAuthProviderHolder.instance.signInWithGoogle()
                "apple" -> SocialAuthProviderHolder.instance.signInWithApple()
                else -> null
            }
            if (socialResult == null) { _state.value = AuthState.LoggedOut; return }

            if (socialResult.idToken == "__desktop_exchanged__") {
                val creds = PreExchangedCredentialsHolder.take()
                if (creds != null) {
                    if (TokenStorageProvider.isInitialized()) {
                        TokenStorageProvider.instance.saveCredentials(creds.userId, creds.username, creds.token)
                    }
                    _state.value = AuthState.LoggedIn(userId = creds.userId, username = creds.username, token = creds.token)
                } else {
                    _state.value = AuthState.Error("Desktop sign-in failed")
                }
                return
            }

            val requestBody = json.encodeToString(SocialLoginApiRequest(idToken = socialResult.idToken, provider = socialResult.provider))
            val response: HttpResponse = httpClient.post("$baseUrl/api/auth/social-login") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (response.status == HttpStatusCode.TooManyRequests) {
                _state.value = AuthState.Error(rateLimitMessage); return
            }
            val responseText = response.bodyAsText()
            if (responseText.isBlank()) {
                _state.value = AuthState.Error("Social login failed (${response.status.value})"); return
            }
            val authResponse = json.decodeFromString<AuthResponse>(responseText)
            if (authResponse.success && authResponse.userId != null && authResponse.token != null) {
                val uname = authResponse.username ?: "user"
                if (TokenStorageProvider.isInitialized()) {
                    TokenStorageProvider.instance.saveCredentials(authResponse.userId, uname, authResponse.token)
                }
                _state.value = AuthState.LoggedIn(userId = authResponse.userId, username = uname, token = authResponse.token)
            } else {
                _state.value = AuthState.Error(authResponse.error ?: "Social login failed")
            }
        } catch (e: Exception) {
            _state.value = AuthState.Error(e.message ?: "Network error")
        }
    }

    suspend fun linkSocialAccount(provider: String): LinkResult {
        if (!SocialAuthProviderHolder.isInitialized()) return LinkResult.Error("Social login not available")
        val token = getToken() ?: return LinkResult.Error("Not logged in")
        return try {
            val socialResult = when (provider) {
                "google" -> SocialAuthProviderHolder.instance.signInWithGoogle()
                "apple" -> SocialAuthProviderHolder.instance.signInWithApple()
                else -> null
            } ?: return LinkResult.Error("Sign-in cancelled")
            val requestBody = json.encodeToString(LinkAccountApiRequest(idToken = socialResult.idToken, provider = socialResult.provider))
            val response: HttpResponse = httpClient.post("$baseUrl/api/auth/link-account") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (response.status == HttpStatusCode.Unauthorized) { handleUnauthorized(); return LinkResult.Error("Session expired") }
            val authResponse = json.decodeFromString<AuthResponse>(response.bodyAsText())
            if (authResponse.success) LinkResult.Success(provider) else LinkResult.Error(authResponse.error ?: "Failed to link account")
        } catch (e: Exception) {
            LinkResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getLinkedAccounts(): List<String> {
        val token = getToken() ?: return emptyList()
        return try {
            val response: HttpResponse = httpClient.get("$baseUrl/api/auth/linked-accounts") {
                header("Authorization", "Bearer $token")
            }
            if (response.status == HttpStatusCode.Unauthorized) { handleUnauthorized(); return emptyList() }
            if (response.status == HttpStatusCode.OK) {
                json.decodeFromString<List<LinkedAccountEntry>>(response.bodyAsText()).map { it.provider }
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteAccount(): Boolean {
        val token = getToken() ?: return false
        return try {
            val response: HttpResponse = httpClient.delete("$baseUrl/api/auth/account") {
                header("Authorization", "Bearer $token")
            }
            if (response.status == HttpStatusCode.Unauthorized) { handleUnauthorized(); return false }
            val authResponse = json.decodeFromString<AuthResponse>(response.bodyAsText())
            if (authResponse.success) {
                if (TokenStorageProvider.isInitialized()) TokenStorageProvider.instance.clearCredentials()
                _state.value = AuthState.LoggedOut
                true
            } else false
        } catch (e: Exception) { false }
    }

    fun logout() {
        if (TokenStorageProvider.isInitialized()) TokenStorageProvider.instance.clearCredentials()
        _state.value = AuthState.LoggedOut
    }

    fun clearError() { _state.value = AuthState.LoggedOut }

    fun getToken(): String? = when (val currentState = _state.value) {
        is AuthState.LoggedIn -> currentState.token
        else -> null
    }

    suspend fun forgotPassword(email: String): Boolean {
        return try {
            val requestBody = json.encodeToString(ForgotPasswordApiRequest(email = email.trim()))
            val response: HttpResponse = httpClient.post("$baseUrl/api/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (response.status == HttpStatusCode.TooManyRequests) return false
            response.status == HttpStatusCode.OK
        } catch (_: Exception) { false }
    }

    fun isLoggedIn(): Boolean = _state.value is AuthState.LoggedIn
}
