package it.curzel.tamahero.usecase

import it.curzel.tamahero.auth.hashPassword
import it.curzel.tamahero.db.UserRepository

sealed class RegisterResult {
    data class Success(val userId: Long) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
}

object RegisterUseCase {

    fun register(username: String, password: String): RegisterResult {
        if (username.isBlank()) return RegisterResult.Error("Username cannot be blank")
        if (password.length < 6) return RegisterResult.Error("Password must be at least 6 characters")
        if (UserRepository.findByUsername(username) != null) {
            return RegisterResult.Error("Username already taken")
        }
        val userId = UserRepository.createUser(username, hashPassword(password))
        return RegisterResult.Success(userId)
    }
}
