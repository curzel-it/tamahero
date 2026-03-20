package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val username: String, val password: String)

@Serializable
data class CreateHeroRequest(val name: String)

@Serializable
data class StartActionRequest(val actionType: ActionType)

@Serializable
data class ApiError(val error: String)
