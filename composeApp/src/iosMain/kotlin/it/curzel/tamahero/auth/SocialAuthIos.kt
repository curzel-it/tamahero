package it.curzel.tamahero.auth

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.curzel.tamahero.ServerConfig
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.AuthenticationServices.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume

private const val GOOGLE_CLIENT_ID_IOS = "605490977919-2qr3tki4f3enbol76l91o0moa09g4qn1.apps.googleusercontent.com"
private const val GOOGLE_CALLBACK_SCHEME = "com.googleusercontent.apps.605490977919-2qr3tki4f3enbol76l91o0moa09g4qn1"
private val BASE_URL = ServerConfig.BASE_URL

@Serializable
private data class GoogleTokenResponse(
    val id_token: String? = null,
    val access_token: String? = null,
    val error: String? = null,
    val error_description: String? = null,
)

class SocialAuthIos : SocialAuthProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private var appleSignInDelegate: AppleSignInDelegate? = null

    override suspend fun signInWithGoogle(): SocialAuthResult? {
        if (!WebAuthProviderHolder.isInitialized()) return null
        val redirectUri = "$GOOGLE_CALLBACK_SCHEME:/oauth2redirect"
        val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
            "client_id=${GOOGLE_CLIENT_ID_IOS.urlEncode()}" +
            "&redirect_uri=${redirectUri.urlEncode()}" +
            "&response_type=code" +
            "&scope=${("openid email profile").urlEncode()}"

        val callbackUrl = suspendCancellableCoroutine<String?> { continuation ->
            WebAuthProviderHolder.instance.startAuth(
                url = authUrl,
                callbackScheme = GOOGLE_CALLBACK_SCHEME,
                completion = { result -> continuation.resume(result) },
            )
        } ?: return null

        val rawCode = extractQueryParam(callbackUrl, "code") ?: return null
        val code = rawCode.urlDecode()
        return exchangeCodeForIdToken(code, redirectUri)
    }

    private suspend fun exchangeCodeForIdToken(code: String, redirectUri: String): SocialAuthResult? {
        return withContext(Dispatchers.IO) {
            val httpClient = HttpClient()
            try {
                val tokenResponse = httpClient.post("https://oauth2.googleapis.com/token") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(listOf(
                        "code" to code,
                        "client_id" to GOOGLE_CLIENT_ID_IOS,
                        "redirect_uri" to redirectUri,
                        "grant_type" to "authorization_code",
                    ).formUrlEncode())
                }
                val responseText = tokenResponse.bodyAsText()
                if (responseText.isBlank()) return@withContext null
                val body = json.decodeFromString(GoogleTokenResponse.serializer(), responseText)
                val idToken = body.id_token
                if (idToken != null) SocialAuthResult(idToken = idToken, provider = "google") else null
            } catch (e: Exception) {
                null
            } finally {
                httpClient.close()
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun signInWithApple(): SocialAuthResult? = suspendCancellableCoroutine { continuation ->
        val provider = ASAuthorizationAppleIDProvider()
        val request = provider.createRequest()
        request.requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)

        val delegate = AppleSignInDelegate { result ->
            appleSignInDelegate = null
            continuation.resume(result)
        }
        appleSignInDelegate = delegate

        val controller = ASAuthorizationController(authorizationRequests = listOf(request))
        controller.delegate = delegate
        controller.performRequests()
    }

    override fun isGoogleAvailable(): Boolean = WebAuthProviderHolder.isInitialized()
    override fun isAppleAvailable(): Boolean = true
}

private fun extractQueryParam(url: String, param: String): String? {
    val queryStart = url.indexOf('?').takeIf { it >= 0 } ?: url.indexOf('#').takeIf { it >= 0 } ?: return null
    val query = url.substring(queryStart + 1)
    return query.split('&').map { it.split('=', limit = 2) }.firstOrNull { it.size == 2 && it[0] == param }?.get(1)
}

@OptIn(BetaInteropApi::class)
private fun String.urlEncode(): String =
    NSString.create(string = this).stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet) ?: this

@OptIn(BetaInteropApi::class)
private fun String.urlDecode(): String =
    NSString.create(string = this).stringByRemovingPercentEncoding ?: this

@OptIn(ExperimentalForeignApi::class)
private class AppleSignInDelegate(
    private val onComplete: (SocialAuthResult?) -> Unit,
) : NSObject(), ASAuthorizationControllerDelegateProtocol {
    override fun authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization: ASAuthorization) {
        val credential = didCompleteWithAuthorization.credential
        if (credential is ASAuthorizationAppleIDCredential) {
            val tokenData = credential.identityToken
            if (tokenData != null) {
                @OptIn(BetaInteropApi::class)
                val nsString = NSString.create(data = tokenData, encoding = NSUTF8StringEncoding)
                val idToken = nsString?.toString()
                if (idToken != null) { onComplete(SocialAuthResult(idToken = idToken, provider = "apple")); return }
            }
        }
        onComplete(null)
    }

    override fun authorizationController(controller: ASAuthorizationController, didCompleteWithError: NSError) {
        onComplete(null)
    }
}
