package it.curzel.tamahero.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import it.curzel.tamahero.db.UserRepository

data class UserPrincipal(val userId: Long, val username: String) : @Suppress("DEPRECATION") Principal

fun Application.configureAuth() {
    install(Authentication) {
        bearer("auth-bearer") {
            authenticate { tokenCredential ->
                val userId = AuthService.validateToken(tokenCredential.token) ?: return@authenticate null
                val user = UserRepository.findById(userId) ?: return@authenticate null
                UserPrincipal(user.id, user.username)
            }
        }
    }
}
