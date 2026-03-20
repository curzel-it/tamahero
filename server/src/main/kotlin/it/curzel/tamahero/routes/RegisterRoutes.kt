package it.curzel.tamahero.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.curzel.tamahero.models.ApiError
import it.curzel.tamahero.models.RegisterRequest
import it.curzel.tamahero.usecase.RegisterResult
import it.curzel.tamahero.usecase.RegisterUseCase
import kotlinx.serialization.Serializable

@Serializable
data class RegisterResponse(val userId: Long)

fun Route.registerRoutes() {
    post("/api/register") {
        val request = call.receive<RegisterRequest>()
        when (val result = RegisterUseCase.register(request.username, request.password)) {
            is RegisterResult.Success -> call.respond(HttpStatusCode.Created, RegisterResponse(result.userId))
            is RegisterResult.Error -> call.respond(HttpStatusCode.BadRequest, ApiError(result.message))
        }
    }
}
