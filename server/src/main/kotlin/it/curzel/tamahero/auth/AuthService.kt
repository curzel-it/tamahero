package it.curzel.tamahero.auth

import it.curzel.tamahero.db.*
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.security.SecureRandom

sealed class AuthResult {
    data class Success(val userId: Long, val token: String, val username: String = "") : AuthResult()
    data class Error(val message: String) : AuthResult()
}

object AuthService {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private const val TOKEN_LENGTH = 64
    private const val TOKEN_VALIDITY_MS = 90L * 24 * 60 * 60 * 1000
    private const val MIN_USERNAME_LENGTH = 3
    private const val MAX_USERNAME_LENGTH = 20
    private const val MIN_PASSWORD_LENGTH = 6
    private const val MAX_PASSWORD_LENGTH = 128
    private const val PASSWORD_RESET_TTL_MS = 60L * 60 * 1000

    private val secureRandom = SecureRandom()
    private val tokenChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    fun register(username: String, password: String, email: String? = null): AuthResult {
        val trimmedUsername = username.trim()
        val trimmedEmail = email?.trim()?.lowercase()?.ifEmpty { null }

        if (trimmedUsername.length < MIN_USERNAME_LENGTH)
            return AuthResult.Error("Username must be at least $MIN_USERNAME_LENGTH characters")
        if (trimmedUsername.length > MAX_USERNAME_LENGTH)
            return AuthResult.Error("Username must be at most $MAX_USERNAME_LENGTH characters")
        if (!trimmedUsername.matches(Regex("^[a-zA-Z0-9_]+$")))
            return AuthResult.Error("Username can only contain letters, numbers, and underscores")
        if (password.length < MIN_PASSWORD_LENGTH)
            return AuthResult.Error("Password must be at least $MIN_PASSWORD_LENGTH characters")
        if (password.length > MAX_PASSWORD_LENGTH)
            return AuthResult.Error("Password must be at most $MAX_PASSWORD_LENGTH characters")
        if (trimmedEmail != null && !trimmedEmail.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")))
            return AuthResult.Error("Invalid email address")
        if (UserRepository.usernameExists(trimmedUsername))
            return AuthResult.Error("Username already taken")

        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12))
        return try {
            val userId = UserRepository.createUser(trimmedUsername, passwordHash, trimmedEmail)
            storeTokenForUser(userId, trimmedUsername)
        } catch (e: Exception) {
            AuthResult.Error("Username already taken")
        }
    }

    fun login(username: String, password: String): AuthResult {
        val input = username.trim()
        val user = UserRepository.findByUsername(input)
            ?: UserRepository.findByEmail(input)
            ?: return AuthResult.Error("Invalid username or password")

        if (user.passwordHash == null)
            return AuthResult.Error("This account uses social login. Please sign in with Google or Apple.")

        val passwordValid = if (user.passwordNeedsRehash) {
            if (verifyPasswordSha256(password, user.passwordHash)) {
                val newHash = BCrypt.hashpw(password, BCrypt.gensalt(12))
                UserRepository.updatePasswordHash(user.id, newHash)
                logger.info("Rehashed password for user {}", user.username)
                true
            } else false
        } else {
            verifyPassword(password, user.passwordHash)
        }

        if (!passwordValid) return AuthResult.Error("Invalid username or password")
        UserRepository.updateLastLogin(user.id)
        return storeTokenForUser(user.id, user.username)
    }

    fun socialLogin(idToken: String, provider: String): AuthResult {
        val socialInfo = verifySocialToken(idToken, provider)
        if (socialInfo == null) {
            logger.warn("Social login failed: {} token verification returned null", provider)
            return AuthResult.Error("Invalid token")
        }

        val existingUser = SocialLoginRepository.findBySocialLogin(socialInfo.provider, socialInfo.providerUserId)
        if (existingUser != null) {
            UserRepository.updateLastLogin(existingUser.id)
            return storeTokenForUser(existingUser.id, existingUser.username)
        }

        if (socialInfo.email != null) {
            val emailUser = UserRepository.findByEmail(socialInfo.email)
            if (emailUser != null) {
                SocialLoginRepository.addSocialLogin(emailUser.id, socialInfo.provider, socialInfo.providerUserId, socialInfo.email, socialInfo.displayName)
                UserRepository.updateLastLogin(emailUser.id)
                return storeTokenForUser(emailUser.id, emailUser.username)
            }
        }

        val generatedUsername = UserRepository.generateUniqueUsername(socialInfo.displayName ?: "user")
        val userId = UserRepository.createSocialUser(generatedUsername, socialInfo.email)
            ?: return AuthResult.Error("Failed to create account")
        SocialLoginRepository.addSocialLogin(userId, socialInfo.provider, socialInfo.providerUserId, socialInfo.email, socialInfo.displayName)
        return storeTokenForUser(userId, generatedUsername)
    }

    fun linkSocialAccount(userId: Long, idToken: String, provider: String): AuthResult {
        val socialInfo = verifySocialToken(idToken, provider) ?: return AuthResult.Error("Invalid token")
        val existingUser = SocialLoginRepository.findBySocialLogin(socialInfo.provider, socialInfo.providerUserId)
        if (existingUser != null && existingUser.id != userId)
            return AuthResult.Error("This $provider account is already linked to another user")
        if (existingUser != null && existingUser.id == userId)
            return AuthResult.Error("This $provider account is already linked")

        SocialLoginRepository.addSocialLogin(userId, socialInfo.provider, socialInfo.providerUserId, socialInfo.email, socialInfo.displayName)
        if (socialInfo.email != null) {
            val user = UserRepository.findById(userId)
            if (user?.email == null) UserRepository.setEmail(userId, socialInfo.email)
        }
        return AuthResult.Success(userId, "")
    }

    private fun verifySocialToken(idToken: String, provider: String): SocialUserInfo? =
        when (provider) {
            "google" -> GoogleTokenVerifier.verify(idToken)
            "apple" -> AppleTokenVerifier.verify(idToken)
            else -> null
        }

    suspend fun requestPasswordReset(email: String) {
        val trimmed = email.trim().lowercase()
        val user = UserRepository.findByEmail(trimmed) ?: return
        val token = generateToken()
        val now = System.currentTimeMillis()
        PasswordResetTokenRepository.storeToken(token, user.id, now, now + PASSWORD_RESET_TTL_MS)
        EmailService.sendPasswordResetEmail(trimmed, token)
    }

    fun resetPassword(token: String, newPassword: String): AuthResult {
        if (newPassword.length < MIN_PASSWORD_LENGTH)
            return AuthResult.Error("Password must be at least $MIN_PASSWORD_LENGTH characters")
        if (newPassword.length > MAX_PASSWORD_LENGTH)
            return AuthResult.Error("Password must be at most $MAX_PASSWORD_LENGTH characters")

        val userId = PasswordResetTokenRepository.findValidToken(token)
            ?: return AuthResult.Error("Invalid or expired reset token")
        val hash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12))
        UserRepository.updatePasswordHash(userId, hash)
        PasswordResetTokenRepository.deleteByUser(userId)
        return AuthResult.Success(userId, "", "")
    }

    fun cleanupExpiredTokens() {
        AuthTokenRepository.cleanupExpired()
        PasswordResetTokenRepository.cleanupExpired()
    }

    fun deleteAccount(userId: Long) {
        UserRepository.deleteUser(userId)
    }

    fun validateToken(token: String): Long? = AuthTokenRepository.validateToken(token)

    fun logout(token: String): Boolean {
        AuthTokenRepository.deleteToken(token)
        return true
    }

    fun getUserIdFromToken(token: String?): Long? {
        if (token.isNullOrBlank()) return null
        return validateToken(token)
    }

    private fun storeTokenForUser(userId: Long, username: String): AuthResult {
        val token = generateToken()
        val now = System.currentTimeMillis()
        AuthTokenRepository.storeToken(token, userId, now, now + TOKEN_VALIDITY_MS)
        return AuthResult.Success(userId, token, username)
    }

    private fun generateToken(): String =
        (1..TOKEN_LENGTH).map { tokenChars[secureRandom.nextInt(tokenChars.length)] }.joinToString("")
}
