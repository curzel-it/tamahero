package it.curzel.tamahero.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.curzel.tamahero.auth.AuthService
import it.curzel.tamahero.models.*
import it.curzel.tamahero.village.VillageService
import it.curzel.tamahero.websocket.ConnectionManager
import kotlinx.serialization.Serializable

@Serializable
data class TriggerEventRequest(
    val userId: Long,
    val eventType: String,
    val troops: List<TriggerEventTroop>? = null,
)

@Serializable
data class TriggerEventTroop(
    val type: String,
    val count: Int = 1,
    val level: Int = 1,
)

@Serializable
data class GrantResourcesRequest(
    val userId: Long,
    val gold: Long = 0,
    val wood: Long = 0,
    val metal: Long = 0,
    val mana: Long = 0,
)

@Serializable
data class PlaceBuildingRequest(
    val userId: Long,
    val type: String,
    val x: Int,
    val y: Int,
    val level: Int = 1,
)

@Serializable
data class AdvanceTimeRequest(
    val userId: Long,
    val deltaMs: Long,
)

@Serializable
data class GetVillageRequest(val userId: Long)

@Serializable
data class ResetVillageRequest(val userId: Long)

@Serializable
data class AdminResponse(val success: Boolean, val error: String? = null)

fun Route.adminRoutes() {
    route("/api/admin") {
        post("/trigger-event") {
            val adminId = requireAdmin(call) ?: return@post
            val request = call.receive<TriggerEventRequest>()
            val eventType = try {
                EventType.valueOf(request.eventType)
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, AdminResponse(false, "Unknown event type: ${request.eventType}. Options: ${EventType.entries.joinToString()}"))
                return@post
            }
            val customWave = request.troops?.let { troops ->
                try {
                    EventWave(troops.map { t ->
                        EventTroop(type = TroopType.valueOf(t.type), count = t.count, level = t.level)
                    })
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, AdminResponse(false, "Invalid troop type"))
                    return@post
                }
            }
            try {
                val state = VillageService.triggerEvent(request.userId, eventType, customWave)
                ConnectionManager.sendToPlayer(request.userId, ServerMessage.EventStarted(eventType))
                ConnectionManager.sendToPlayer(request.userId, ServerMessage.GameStateUpdated(state))
                call.respond(AdminResponse(true))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, AdminResponse(false, e.message))
            }
        }

        post("/grant-resources") {
            val adminId = requireAdmin(call) ?: return@post
            val request = call.receive<GrantResourcesRequest>()
            val resources = Resources(gold = request.gold, wood = request.wood, metal = request.metal, mana = request.mana)
            try {
                val state = VillageService.grantResources(request.userId, resources)
                ConnectionManager.sendToPlayer(request.userId, ServerMessage.ResourcesUpdated(state.resources))
                ConnectionManager.sendToPlayer(request.userId, ServerMessage.GameStateUpdated(state))
                call.respond(AdminResponse(true))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, AdminResponse(false, e.message))
            }
        }

        post("/reset-village") {
            val adminId = requireAdmin(call) ?: return@post
            val request = call.receive<ResetVillageRequest>()
            try {
                val state = VillageService.resetVillage(request.userId)
                ConnectionManager.sendToPlayer(request.userId, ServerMessage.GameStateUpdated(state))
                call.respond(AdminResponse(true))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, AdminResponse(false, e.message))
            }
        }

        post("/place-building") {
            val adminId = requireAdmin(call) ?: return@post
            val request = call.receive<PlaceBuildingRequest>()
            val buildingType = try {
                BuildingType.valueOf(request.type)
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, AdminResponse(false, "Unknown building type: ${request.type}"))
                return@post
            }
            try {
                val state = VillageService.adminPlaceBuilding(request.userId, buildingType, request.x, request.y, request.level)
                call.respond(AdminResponse(true))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, AdminResponse(false, e.message))
            }
        }

        post("/advance-time") {
            val adminId = requireAdmin(call) ?: return@post
            val request = call.receive<AdvanceTimeRequest>()
            try {
                val state = VillageService.advanceTime(request.userId, request.deltaMs)
                call.respond(AdminResponse(true))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, AdminResponse(false, e.message))
            }
        }

        post("/get-village") {
            val adminId = requireAdmin(call) ?: return@post
            val request = call.receive<GetVillageRequest>()
            try {
                val state = VillageService.getOrCreateVillage(request.userId)
                call.respond(state)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, AdminResponse(false, e.message))
            }
        }
    }
}

private suspend fun requireAdmin(call: io.ktor.server.application.ApplicationCall): Long? {
    val token = call.request.header("Authorization")?.removePrefix("Bearer ")
    val adminId = AuthService.getAdminUserId(token)
    if (adminId == null) {
        call.respond(HttpStatusCode.Forbidden, AdminResponse(false, "Admin access required"))
    }
    return adminId
}
