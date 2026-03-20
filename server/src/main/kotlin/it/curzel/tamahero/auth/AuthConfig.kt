package it.curzel.tamahero.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import it.curzel.tamahero.db.UserRepository

data class UserPrincipal(val userId: Long, val username: String) : @Suppress("DEPRECATION") Principal

fun Application.configureAuth() {
    install(Authentication) {
        basic("auth-basic") {
            validate { credentials ->
                val user = UserRepository.findByUsername(credentials.name)
                if (user != null && verifyPassword(credentials.password, user.passwordHash)) {
                    UserPrincipal(user.id, user.username)
                } else null
            }
        }
    }
}
