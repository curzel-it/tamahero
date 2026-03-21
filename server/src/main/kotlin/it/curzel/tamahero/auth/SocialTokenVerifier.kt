package it.curzel.tamahero.auth

import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.slf4j.LoggerFactory
import java.net.URI
import java.security.interfaces.RSAPublicKey

data class SocialUserInfo(
    val provider: String,
    val providerUserId: String,
    val email: String?,
    val displayName: String?,
)

object GoogleTokenVerifier {
    private val logger = LoggerFactory.getLogger(GoogleTokenVerifier::class.java)
    private val jwkProvider = UrlJwkProvider(
        URI.create("https://www.googleapis.com/oauth2/v3/certs").toURL(),
        5000,
        5000,
    )

    fun verify(idToken: String): SocialUserInfo? {
        return try {
            val decoded = JWT.decode(idToken)
            val jwk = jwkProvider.get(decoded.keyId)
            val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)
            val verifier = JWT.require(algorithm)
                .withIssuer("accounts.google.com", "https://accounts.google.com")
                .build()
            val verified = verifier.verify(idToken)
            val audience = verified.audience?.firstOrNull()
            if (audience == null || audience !in OAuthConfig.allGoogleClientIds) {
                logger.warn("Google token audience mismatch: got={}", audience)
                return null
            }
            val emailVerified = verified.getClaim("email_verified").asBoolean() ?: false
            val email = if (emailVerified) verified.getClaim("email").asString() else null
            SocialUserInfo(
                provider = "google",
                providerUserId = verified.subject,
                email = email,
                displayName = verified.getClaim("name").asString(),
            )
        } catch (e: Exception) {
            logger.warn("Google token verification failed: {}", e.message)
            null
        }
    }
}

object AppleTokenVerifier {
    private val logger = LoggerFactory.getLogger(AppleTokenVerifier::class.java)
    private val jwkProvider = UrlJwkProvider(
        URI.create("https://appleid.apple.com/auth/keys").toURL(),
        5000,
        5000,
    )

    fun verify(idToken: String): SocialUserInfo? {
        return try {
            val decoded = JWT.decode(idToken)
            val jwk = jwkProvider.get(decoded.keyId)
            val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)
            val verifier = JWT.require(algorithm)
                .withIssuer("https://appleid.apple.com")
                .build()
            val verified = verifier.verify(idToken)
            val audience = verified.audience?.firstOrNull()
            if (audience == null || audience !in OAuthConfig.allAppleAudiences) {
                logger.warn("Apple token audience mismatch: got={}", audience)
                return null
            }
            SocialUserInfo(
                provider = "apple",
                providerUserId = verified.subject,
                email = verified.getClaim("email").asString(),
                displayName = null,
            )
        } catch (e: Exception) {
            logger.warn("Apple token verification failed: {}", e.message)
            null
        }
    }
}
