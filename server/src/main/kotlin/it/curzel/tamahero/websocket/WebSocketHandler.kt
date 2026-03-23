package it.curzel.tamahero.websocket

import io.ktor.websocket.*
import it.curzel.tamahero.auth.AuthService
import it.curzel.tamahero.models.ClientMessage
import it.curzel.tamahero.models.ProtocolJson
import it.curzel.tamahero.models.ServerMessage
import it.curzel.tamahero.db.DeviceTokenRepository
import it.curzel.tamahero.village.LeaderboardService
import it.curzel.tamahero.village.PvpService
import it.curzel.tamahero.village.VillageException
import it.curzel.tamahero.village.VillageService
import org.slf4j.LoggerFactory

object WebSocketHandler {

    private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)

    suspend fun handleConnection(session: WebSocketSession, token: String?) {
        val userId = AuthService.getUserIdFromToken(token)
        if (userId == null) {
            session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return
        }

        val connectionId = ConnectionManager.registerConnection(userId, session)
        val connectedText = ProtocolJson.encodeToString(ServerMessage.serializer(), ServerMessage.Connected(playerId = userId))
        session.send(Frame.Text(connectedText))

        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> handleMessage(userId, connectionId, frame.readText())
                    is Frame.Close -> break
                    else -> {}
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket error for player {}", userId, e)
        } finally {
            ConnectionManager.removeConnection(userId, connectionId)
        }
    }

    private suspend fun handleMessage(userId: Long, connectionId: Long, text: String) {
        try {
            val message = ProtocolJson.decodeFromString(ClientMessage.serializer(), text)
            when (message) {
                is ClientMessage.KeepAlive -> ConnectionManager.updateKeepAlive(userId, connectionId)
                is ClientMessage.GetVillage -> handleVillageAction(userId) { VillageService.getOrCreateVillage(userId) }
                is ClientMessage.Build -> handleVillageAction(userId) { VillageService.placeBuild(userId, message.buildingType, message.x, message.y) }
                is ClientMessage.Upgrade -> handleVillageAction(userId) { VillageService.upgradeBuilding(userId, message.buildingId) }
                is ClientMessage.Move -> handleVillageAction(userId) { VillageService.moveBuilding(userId, message.buildingId, message.x, message.y) }
                is ClientMessage.Collect -> handleVillageAction(userId) { VillageService.collectResources(userId, message.buildingId) }
                is ClientMessage.CollectAll -> handleVillageAction(userId) { VillageService.collectAll(userId) }
                is ClientMessage.Demolish -> handleVillageAction(userId) { VillageService.demolishBuilding(userId, message.buildingId) }
                is ClientMessage.CancelConstruction -> handleVillageAction(userId) { VillageService.cancelConstruction(userId, message.buildingId) }
                is ClientMessage.SpeedUp -> handleVillageAction(userId) { VillageService.speedUpConstruction(userId, message.buildingId) }
                is ClientMessage.Train -> handleVillageAction(userId) { VillageService.trainTroops(userId, message.troopType, message.count) }
                is ClientMessage.CancelTraining -> handleVillageAction(userId) { VillageService.cancelTraining(userId, message.index) }
                is ClientMessage.RearmTrap -> handleVillageAction(userId) { VillageService.rearmTrap(userId, message.buildingId) }
                is ClientMessage.RearmAllTraps -> handleVillageAction(userId) { VillageService.rearmAllTraps(userId) }
                is ClientMessage.CollectEventRewards -> handleVillageAction(userId) { VillageService.collectEventRewards(userId) }
                is ClientMessage.FeedHero -> handleVillageAction(userId) { VillageService.feedHero(userId) }
                is ClientMessage.TrainHero -> handleVillageAction(userId) { VillageService.trainHero(userId) }
                is ClientMessage.FindOpponent -> handlePvpAction(userId) { PvpService.findOpponent(userId) }
                is ClientMessage.NextOpponent -> handlePvpAction(userId) { PvpService.findOpponent(userId) }
                is ClientMessage.StartPvp -> handlePvpAction(userId) { PvpService.startBattle(userId, message.targetId) }
                is ClientMessage.DeployTroop -> handlePvpAction(userId) { PvpService.deployTroop(userId, message.troopType, message.x, message.y) }
                is ClientMessage.EndBattle -> handlePvpAction(userId) { PvpService.endBattle(userId) }
                is ClientMessage.GetLeaderboard -> handlePvpAction(userId) { LeaderboardService.getLeaderboard(userId) }
                is ClientMessage.RegisterDevice -> {
                    DeviceTokenRepository.saveToken(userId, message.token, message.platform)
                    ConnectionManager.sendToPlayer(userId, ServerMessage.GameStateUpdated(VillageService.getOrCreateVillage(userId)))
                }
                is ClientMessage.UnregisterDevice -> {
                    DeviceTokenRepository.removeToken(message.token)
                }
            }
        } catch (e: Exception) {
            logger.warn("Invalid message from player {}: {}", userId, e.message)
        }
    }

    private suspend fun handlePvpAction(userId: Long, action: () -> ServerMessage) {
        try {
            val message = action()
            ConnectionManager.sendToPlayer(userId, message)
        } catch (e: Exception) {
            ConnectionManager.sendToPlayer(userId, ServerMessage.Error(reason = e.message ?: "PvP action failed"))
        }
    }

    private suspend fun handleVillageAction(userId: Long, action: () -> it.curzel.tamahero.models.GameState) {
        try {
            val state = action()
            ConnectionManager.sendToPlayer(userId, ServerMessage.GameStateUpdated(state))
        } catch (e: VillageException) {
            ConnectionManager.sendToPlayer(userId, ServerMessage.Error(reason = e.message ?: "Action failed"))
        }
    }
}
