package it.curzel.tamahero.auth

sealed class AuthState {
    data object LoggedOut : AuthState()
    data class Loading(val message: String = "Loading...") : AuthState()
    data class LoggedIn(val userId: Long, val username: String, val token: String) : AuthState() {
        override fun toString(): String = "LoggedIn(userId=$userId, username=$username, token=***)"
    }
    data class Error(val message: String) : AuthState()
}

enum class AuthScreen { LOGIN, SIGNUP }

sealed class LinkResult {
    data class Success(val provider: String) : LinkResult()
    data class Error(val message: String) : LinkResult()
}
