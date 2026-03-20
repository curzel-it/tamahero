package it.curzel.tamahero.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.curzel.tamahero.auth.UserPrincipal
import it.curzel.tamahero.models.ApiError
import it.curzel.tamahero.models.StartActionRequest
import it.curzel.tamahero.usecase.ActionResult
import it.curzel.tamahero.usecase.ActionUseCase

fun Route.actionRoutes() {
    authenticate("auth-basic") {
        post("/api/heroes/{id}/action") {
            val principal = call.principal<UserPrincipal>()!!
            val heroId = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiError("Invalid hero ID"))
            val request = call.receive<StartActionRequest>()
            when (val result = ActionUseCase.startAction(heroId, principal.userId, request.actionType)) {
                is ActionResult.Started -> call.respond(HttpStatusCode.Created, result.action)
                is ActionResult.Error -> call.respond(HttpStatusCode.BadRequest, ApiError(result.message))
                else -> call.respond(HttpStatusCode.InternalServerError, ApiError("Unexpected result"))
            }
        }

        get("/api/heroes/{id}/action") {
            val principal = call.principal<UserPrincipal>()!!
            val heroId = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiError("Invalid hero ID"))
            when (val result = ActionUseCase.resolveAction(heroId, principal.userId)) {
                is ActionResult.InProgress -> call.respond(result.action)
                is ActionResult.Completed -> call.respond(result.hero)
                is ActionResult.Idle -> call.respond(HttpStatusCode.NoContent)
                is ActionResult.Error -> call.respond(HttpStatusCode.NotFound, ApiError(result.message))
                else -> call.respond(HttpStatusCode.InternalServerError, ApiError("Unexpected result"))
            }
        }
    }
}
