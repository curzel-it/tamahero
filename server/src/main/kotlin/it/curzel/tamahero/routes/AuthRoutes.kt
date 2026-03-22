package it.curzel.tamahero.routes

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.curzel.tamahero.auth.*
import it.curzel.tamahero.db.SocialLoginRepository
import it.curzel.tamahero.db.UserRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AuthResponse(
    val success: Boolean,
    val userId: Long? = null,
    val token: String? = null,
    val username: String? = null,
    val error: String? = null,
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class SocialLoginRequest(val idToken: String, val provider: String)

@Serializable
data class LinkAccountRequest(val idToken: String, val provider: String)

@Serializable
data class LinkedAccountResponse(val provider: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val token: String, val newPassword: String)

@Serializable
data class GoogleCodeExchangeRequest(val code: String, val redirectUri: String, val platform: String? = null)

@Serializable
private data class GoogleTokenResponse(val id_token: String? = null, val access_token: String? = null)

private val lenientJson = Json { ignoreUnknownKeys = true }
private val oauthHttpClient = io.ktor.client.HttpClient(io.ktor.client.engine.cio.CIO) {
    engine { requestTimeout = 10_000 }
}

private data class PendingAuthResult(val authResponse: AuthResponse, val createdAt: Long = System.currentTimeMillis())
private val pendingAuthResults = java.util.concurrent.ConcurrentHashMap<String, PendingAuthResult>()
private const val PENDING_AUTH_TTL_MS = 5L * 60 * 1000
private const val MAX_PENDING_AUTH_RESULTS = 1000

private fun cleanupAndInsertPending(state: String, result: PendingAuthResult) {
    val now = System.currentTimeMillis()
    pendingAuthResults.entries.removeIf { now - it.value.createdAt > PENDING_AUTH_TTL_MS }
    if (pendingAuthResults.size >= MAX_PENDING_AUTH_RESULTS) {
        val oldest = pendingAuthResults.entries.minByOrNull { it.value.createdAt }
        if (oldest != null) pendingAuthResults.remove(oldest.key)
    }
    pendingAuthResults[state] = result
}

fun Route.authRoutes() {
    route("/api/auth") {
        post("/register") {
            val request = call.receive<it.curzel.tamahero.models.RegisterRequest>()
            when (val result = AuthService.register(request.username, request.password, request.email)) {
                is AuthResult.Success -> call.respond(AuthResponse(success = true, userId = result.userId, token = result.token, username = result.username))
                is AuthResult.Error -> call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = result.message))
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            when (val result = AuthService.login(request.username, request.password)) {
                is AuthResult.Success -> call.respond(AuthResponse(success = true, userId = result.userId, token = result.token, username = result.username))
                is AuthResult.Error -> call.respond(HttpStatusCode.Unauthorized, AuthResponse(success = false, error = result.message))
            }
        }

        post("/social-login") {
            val request = call.receive<SocialLoginRequest>()
            if (request.provider !in listOf("google", "apple")) {
                call.respond(AuthResponse(success = false, error = "Unsupported provider")); return@post
            }
            if (request.idToken.length > 10_000) {
                call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = "Token too large")); return@post
            }
            when (val result = AuthService.socialLogin(request.idToken, request.provider)) {
                is AuthResult.Success -> call.respond(AuthResponse(success = true, userId = result.userId, token = result.token, username = result.username))
                is AuthResult.Error -> call.respond(AuthResponse(success = false, error = result.message))
            }
        }

        post("/link-account") {
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")
            val userId = AuthService.getUserIdFromToken(token)
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, AuthResponse(success = false, error = "Invalid or expired token")); return@post
            }
            val request = call.receive<LinkAccountRequest>()
            if (request.provider !in listOf("google", "apple")) {
                call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = "Unsupported provider")); return@post
            }
            if (request.idToken.length > 10_000) {
                call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = "Token too large")); return@post
            }
            when (val result = AuthService.linkSocialAccount(userId, request.idToken, request.provider)) {
                is AuthResult.Success -> call.respond(AuthResponse(success = true, userId = result.userId))
                is AuthResult.Error -> call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = result.message))
            }
        }

        get("/linked-accounts") {
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")
            val userId = AuthService.getUserIdFromToken(token)
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, AuthResponse(success = false, error = "Invalid or expired token")); return@get
            }
            val logins = SocialLoginRepository.getSocialLogins(userId)
            call.respond(logins.map { LinkedAccountResponse(provider = it.provider) })
        }

        post("/logout") {
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")
            if (token != null) AuthService.logout(token)
            call.respond(AuthResponse(success = true))
        }

        get("/validate") {
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")
            val userId = AuthService.getUserIdFromToken(token)
            if (userId != null) {
                call.respond(AuthResponse(success = true, userId = userId))
            } else {
                call.respond(HttpStatusCode.Unauthorized, AuthResponse(success = false, error = "Invalid or expired token"))
            }
        }

        post("/renew") {
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")
            if (token.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, AuthResponse(success = false, error = "Missing token")); return@post
            }
            when (val result = AuthService.renewToken(token)) {
                is AuthResult.Success -> call.respond(AuthResponse(success = true, userId = result.userId, token = result.token, username = result.username))
                is AuthResult.Error -> call.respond(HttpStatusCode.Unauthorized, AuthResponse(success = false, error = result.message))
            }
        }

        delete("/account") {
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")
            val userId = AuthService.getUserIdFromToken(token)
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, AuthResponse(success = false, error = "Invalid or expired token")); return@delete
            }
            AuthService.deleteAccount(userId)
            call.respond(AuthResponse(success = true))
        }

        post("/forgot-password") {
            val request = call.receive<ForgotPasswordRequest>()
            AuthService.requestPasswordReset(request.email)
            call.respond(AuthResponse(success = true))
        }

        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            when (val result = AuthService.resetPassword(request.token, request.newPassword)) {
                is AuthResult.Success -> call.respond(AuthResponse(success = true))
                is AuthResult.Error -> call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = result.message))
            }
        }
    }

    post("/api/auth/google-code-exchange") {
        val request = call.receive<GoogleCodeExchangeRequest>()
        if (request.code.length > 2048 || request.redirectUri.length > 2048) {
            call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = "Invalid request")); return@post
        }
        val platform = request.platform ?: "desktop"
        val validRedirect = when (platform) {
            "ios" -> request.redirectUri.startsWith("com.googleusercontent.apps.")
            else -> request.redirectUri.startsWith("http://127.0.0.1:") || request.redirectUri.startsWith("http://localhost:")
        }
        if (!validRedirect) {
            call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = "Invalid redirect URI")); return@post
        }
        val clientId = when (platform) {
            "ios" -> OAuthConfig.googleClientIdIos
            "desktop" -> OAuthConfig.googleClientIdDesktop.ifEmpty { OAuthConfig.googleClientIdWeb }
            else -> OAuthConfig.googleClientIdWeb
        }
        val clientSecret = when (platform) {
            "ios" -> null
            "desktop" -> OAuthConfig.googleClientSecretDesktop.ifEmpty { OAuthConfig.googleClientSecret }
            else -> OAuthConfig.googleClientSecret
        }
        try {
            val tokenParams = mutableListOf(
                "code" to request.code,
                "client_id" to clientId,
                "redirect_uri" to request.redirectUri,
                "grant_type" to "authorization_code",
            )
            if (clientSecret != null) tokenParams.add("client_secret" to clientSecret)
            val tokenResponse = oauthHttpClient.post("https://oauth2.googleapis.com/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(tokenParams.formUrlEncode())
            }
            val body = lenientJson.decodeFromString<GoogleTokenResponse>(tokenResponse.bodyAsText())
            if (body.id_token == null) {
                call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = "No id_token in response")); return@post
            }
            when (val result = AuthService.socialLogin(body.id_token, "google")) {
                is AuthResult.Success -> call.respond(AuthResponse(success = true, userId = result.userId, token = result.token, username = result.username))
                is AuthResult.Error -> call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = result.message))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, AuthResponse(success = false, error = "Code exchange failed"))
        }
    }

    get("/auth/callback/google") {
        val code = call.request.queryParameters["code"]
        val state = call.request.queryParameters["state"]
        if (code == null || state == null || code.length > 2048 || !state.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))) {
            call.respondText("Missing or invalid parameters", ContentType.Text.Html, HttpStatusCode.BadRequest); return@get
        }
        try {
            val tokenResponse = oauthHttpClient.post("https://oauth2.googleapis.com/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(listOf(
                    "code" to code,
                    "client_id" to OAuthConfig.googleClientIdWeb,
                    "client_secret" to OAuthConfig.googleClientSecret,
                    "redirect_uri" to "https://tama.curzel.it/auth/callback/google",
                    "grant_type" to "authorization_code",
                ).formUrlEncode())
            }
            val body = lenientJson.decodeFromString<GoogleTokenResponse>(tokenResponse.bodyAsText())
            if (body.id_token == null) {
                call.respondText("<html><body><h2>Sign in failed</h2><p>No token received.</p></body></html>", ContentType.Text.Html); return@get
            }
            when (val result = AuthService.socialLogin(body.id_token, "google")) {
                is AuthResult.Success -> {
                    cleanupAndInsertPending(state, PendingAuthResult(AuthResponse(success = true, userId = result.userId, token = result.token, username = result.username)))
                    call.respondText("<html><body><h2>Sign in successful!</h2><p>You can close this window.</p><script>window.close()</script></body></html>", ContentType.Text.Html)
                }
                is AuthResult.Error -> call.respondText("<html><body><h2>Sign in failed</h2></body></html>", ContentType.Text.Html)
            }
        } catch (e: Exception) {
            call.respondText("<html><body><h2>Sign in failed</h2></body></html>", ContentType.Text.Html)
        }
    }

    post("/auth/callback/apple") {
        val params = call.receiveParameters()
        val idToken = params["id_token"]
        val state = params["state"]
        if (idToken == null || state == null || idToken.length > 10_000 || !state.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))) {
            call.respondText("Missing or invalid parameters", ContentType.Text.Html, HttpStatusCode.BadRequest); return@post
        }
        when (val result = AuthService.socialLogin(idToken, "apple")) {
            is AuthResult.Success -> {
                cleanupAndInsertPending(state, PendingAuthResult(AuthResponse(success = true, userId = result.userId, token = result.token, username = result.username)))
                call.respondText("<html><body><h2>Sign in successful!</h2><p>You can close this window.</p></body></html>", ContentType.Text.Html)
            }
            is AuthResult.Error -> {
                val safeMessage = result.message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                call.respondText("<html><body><h2>Sign in failed</h2><p>$safeMessage</p></body></html>", ContentType.Text.Html)
            }
        }
    }

    get("/api/auth/poll") {
        val state = call.request.queryParameters["state"]
        if (state == null || !state.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))) {
            call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, error = "Invalid state")); return@get
        }
        val now = System.currentTimeMillis()
        pendingAuthResults.entries.removeIf { now - it.value.createdAt > PENDING_AUTH_TTL_MS }
        val pending = pendingAuthResults.remove(state)
        if (pending != null) {
            call.respond(pending.authResponse)
        } else {
            call.respond(HttpStatusCode.NotFound, AuthResponse(success = false, error = "Pending"))
        }
    }
}
