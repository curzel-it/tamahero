package it.curzel.tamahero.websocket

import io.ktor.websocket.*
import it.curzel.tamahero.auth.AuthService
import it.curzel.tamahero.models.ClientMessage
import it.curzel.tamahero.models.ServerMessage
import it.curzel.tamahero.village.VillageException
import it.curzel.tamahero.village.VillageService
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

object WebSocketHandler {

    private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun handleConnection(session: WebSocketSession, token: String?) {
        val userId = AuthService.getUserIdFromToken(token)
        if (userId == null) {
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return
        }

        ConnectionManager.registerConnection(userId, session)
        ConnectionManager.sendToPlayer(userId, ServerMessage.Connected(playerId = userId))

        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> handleMessage(userId, frame.readText())
                    is Frame.Close -> break
                    else -> {}
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket error for player {}", userId, e)
        } finally {
            ConnectionManager.removeConnection(userId)
        }
    }

    private suspend fun handleMessage(userId: Long, text: String) {
        try {
            val message = json.decodeFromString(ClientMessage.serializer(), text)
            when (message) {
                is ClientMessage.KeepAlive -> ConnectionManager.updateKeepAlive(userId)
                is ClientMessage.GetVillage -> handleGetVillage(userId)
                is ClientMessage.Build -> handleBuild(userId, message)
                is ClientMessage.Upgrade -> handleUpgrade(userId, message)
                is ClientMessage.Move -> handleMove(userId, message)
                is ClientMessage.Collect -> handleCollect(userId, message)
            }
        } catch (e: Exception) {
            logger.warn("Invalid message from player {}: {}", userId, e.message)
        }
    }

    private suspend fun handleGetVillage(userId: Long) {
        val state = VillageService.getOrCreateVillage(userId)
        ConnectionManager.sendToPlayer(userId, ServerMessage.GameStateUpdated(state))
    }

    private suspend fun handleBuild(userId: Long, msg: ClientMessage.Build) {
        try {
            val state = VillageService.placeBuild(userId, msg.buildingType, msg.x, msg.y)
            ConnectionManager.sendToPlayer(userId, ServerMessage.GameStateUpdated(state))
        } catch (e: VillageException) {
            ConnectionManager.sendToPlayer(userId, ServerMessage.Error(reason = e.message ?: "Build failed"))
        }
    }

    private suspend fun handleUpgrade(userId: Long, msg: ClientMessage.Upgrade) {
        try {
            val state = VillageService.upgradeBuilding(userId, msg.buildingId)
            ConnectionManager.sendToPlayer(userId, ServerMessage.GameStateUpdated(state))
        } catch (e: VillageException) {
            ConnectionManager.sendToPlayer(userId, ServerMessage.Error(reason = e.message ?: "Upgrade failed"))
        }
    }

    private suspend fun handleMove(userId: Long, msg: ClientMessage.Move) {
        try {
            val state = VillageService.moveBuilding(userId, msg.buildingId, msg.x, msg.y)
            ConnectionManager.sendToPlayer(userId, ServerMessage.GameStateUpdated(state))
        } catch (e: VillageException) {
            ConnectionManager.sendToPlayer(userId, ServerMessage.Error(reason = e.message ?: "Move failed"))
        }
    }

    private suspend fun handleCollect(userId: Long, msg: ClientMessage.Collect) {
        try {
            val state = VillageService.collectResources(userId, msg.buildingId)
            ConnectionManager.sendToPlayer(userId, ServerMessage.GameStateUpdated(state))
        } catch (e: VillageException) {
            ConnectionManager.sendToPlayer(userId, ServerMessage.Error(reason = e.message ?: "Collect failed"))
        }
    }
}
