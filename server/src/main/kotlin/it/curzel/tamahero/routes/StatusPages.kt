package it.curzel.tamahero.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import it.curzel.tamahero.models.ApiError

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(cause.message ?: "Unknown error")
            )
        }
    }
}
