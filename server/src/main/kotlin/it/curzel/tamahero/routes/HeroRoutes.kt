package it.curzel.tamahero.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.curzel.tamahero.auth.UserPrincipal
import it.curzel.tamahero.models.ApiError
import it.curzel.tamahero.models.CreateHeroRequest
import it.curzel.tamahero.usecase.HeroResult
import it.curzel.tamahero.usecase.HeroUseCase

fun Route.heroRoutes() {
    authenticate("auth-basic") {
        get("/api/heroes") {
            val principal = call.principal<UserPrincipal>()!!
            val heroes = HeroUseCase.getHeroesByUser(principal.userId)
            call.respond(heroes)
        }

        post("/api/heroes") {
            val principal = call.principal<UserPrincipal>()!!
            val request = call.receive<CreateHeroRequest>()
            when (val result = HeroUseCase.createHero(principal.userId, request.name)) {
                is HeroResult.Success -> call.respond(HttpStatusCode.Created, result.hero)
                is HeroResult.Error -> call.respond(HttpStatusCode.BadRequest, ApiError(result.message))
            }
        }

        get("/api/heroes/{id}") {
            val principal = call.principal<UserPrincipal>()!!
            val heroId = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiError("Invalid hero ID"))
            when (val result = HeroUseCase.getHero(heroId, principal.userId)) {
                is HeroResult.Success -> call.respond(result.hero)
                is HeroResult.Error -> call.respond(HttpStatusCode.NotFound, ApiError(result.message))
            }
        }
    }
}
