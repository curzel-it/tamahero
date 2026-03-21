package it.curzel.tamahero.auth

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.curzel.tamahero.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.URI
import java.net.URLEncoder
import java.util.UUID

@Serializable
private data class CodeExchangeRequest(val code: String, val redirectUri: String, val platform: String = "desktop")

@Serializable
private data class CodeExchangeResponse(
    val success: Boolean,
    val userId: Long? = null,
    val token: String? = null,
    val username: String? = null,
    val error: String? = null,
)

class SocialAuthDesktop : SocialAuthProvider {
    private val baseUrl = ServerConfig.BASE_URL
    private val json = Json { ignoreUnknownKeys = true }
    private val googleClientId = "605490977919-6usrddjci2spn5vjibjt9f4njj4lo6tg.apps.googleusercontent.com"

    override suspend fun signInWithGoogle(): SocialAuthResult? {
        if (!isGoogleAvailable()) return null
        return withContext(Dispatchers.IO) {
            try {
                val serverSocket = ServerSocket(0)
                val port = serverSocket.localPort
                val redirectUri = "http://127.0.0.1:$port/callback"
                val state = UUID.randomUUID().toString()

                val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=${URLEncoder.encode(googleClientId, "UTF-8")}" +
                    "&redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}" +
                    "&response_type=code" +
                    "&scope=${URLEncoder.encode("openid email profile", "UTF-8")}" +
                    "&access_type=offline" +
                    "&state=${URLEncoder.encode(state, "UTF-8")}"

                Desktop.getDesktop().browse(URI(authUrl))

                serverSocket.soTimeout = 120_000
                val socket = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val requestLine = reader.readLine() ?: ""

                val returnedState = Regex("state=([^&\\s]+)").find(requestLine)?.groupValues?.get(1)
                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                val code = Regex("code=([^&\\s]+)").find(requestLine)?.groupValues?.get(1)
                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

                val writer = PrintWriter(socket.getOutputStream(), true)
                writer.println("HTTP/1.1 200 OK")
                writer.println("Content-Type: text/html")
                writer.println()
                writer.println("<html><body><h2>Sign in successful!</h2><p>You can close this window.</p></body></html>")
                writer.flush()
                socket.close()
                serverSocket.close()

                if (code == null || returnedState != state) return@withContext null

                val httpClient = HttpClient()
                val result = try {
                    val response = httpClient.post("$baseUrl/api/auth/google-code-exchange") {
                        contentType(ContentType.Application.Json)
                        setBody(json.encodeToString(CodeExchangeRequest.serializer(), CodeExchangeRequest(code, redirectUri)))
                    }
                    json.decodeFromString(CodeExchangeResponse.serializer(), response.bodyAsText())
                } finally {
                    httpClient.close()
                }
                if (result.success && result.token != null && result.userId != null) {
                    PreExchangedCredentialsHolder.set(PreExchangedCredentials(result.userId, result.token, result.username ?: "user"))
                    SocialAuthResult(idToken = "__desktop_exchanged__", provider = "google")
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun signInWithApple(): SocialAuthResult? {
        return withContext(Dispatchers.IO) {
            try {
                val state = UUID.randomUUID().toString()
                val appleServiceId = System.getenv("APPLE_SERVICE_ID") ?: return@withContext null
                val redirectUri = "$baseUrl/auth/callback/apple"

                val authUrl = "https://appleid.apple.com/auth/authorize?" +
                    "client_id=${URLEncoder.encode(appleServiceId, "UTF-8")}" +
                    "&redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}" +
                    "&response_type=code%20id_token" +
                    "&scope=name%20email" +
                    "&response_mode=form_post" +
                    "&state=$state"

                Desktop.getDesktop().browse(URI(authUrl))

                val httpClient = HttpClient()
                try {
                    var attempts = 0
                    while (attempts < 12) {
                        delay(5000)
                        attempts++
                        val response = httpClient.get("$baseUrl/api/auth/poll") { parameter("state", state) }
                        if (response.status == HttpStatusCode.OK) {
                            val result = json.decodeFromString(CodeExchangeResponse.serializer(), response.bodyAsText())
                            if (result.success && result.token != null && result.userId != null) {
                                PreExchangedCredentialsHolder.set(PreExchangedCredentials(result.userId, result.token, result.username ?: "user"))
                                return@withContext SocialAuthResult(idToken = "__desktop_exchanged__", provider = "apple")
                            }
                            return@withContext null
                        }
                    }
                    null
                } finally {
                    httpClient.close()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun isGoogleAvailable(): Boolean = googleClientId.isNotEmpty()
    override fun isAppleAvailable(): Boolean = (System.getenv("APPLE_SERVICE_ID") ?: "").isNotEmpty()
}
