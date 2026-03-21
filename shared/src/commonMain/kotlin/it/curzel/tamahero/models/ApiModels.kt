package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val username: String, val password: String, val email: String? = null)

@Serializable
data class ApiError(val error: String)
