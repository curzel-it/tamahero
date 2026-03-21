package it.curzel.tamahero.websocket

import it.curzel.tamahero.db.VillageRepository
import it.curzel.tamahero.models.GameStateUpdateUseCase
import it.curzel.tamahero.models.ServerMessage
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

object TimerMonitor {

    private val logger = LoggerFactory.getLogger(TimerMonitor::class.java)
    private var job: Job? = null

    private const val CHECK_INTERVAL_MS = 1_000L
    private const val RESOURCE_PUSH_INTERVAL_MS = 30_000L

    private val lastResourcePush = mutableMapOf<Long, Long>()

    fun start() {
        job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            logger.info("TimerMonitor started")
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                checkTimers()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun checkTimers() {
        val now = System.currentTimeMillis()
        val playerIds = ConnectionManager.getConnectedPlayerIds()

        for (userId in playerIds) {
            try {
                checkPlayerTimers(userId, now)
            } catch (e: Exception) {
                logger.error("Error checking timers for player {}", userId, e)
            }
        }
    }

    private suspend fun checkPlayerTimers(userId: Long, now: Long) {
        val state = VillageRepository.getVillage(userId) ?: return
        val updated = GameStateUpdateUseCase.update(state, now)

        // Detect buildings that just completed construction
        for (building in updated.village.buildings) {
            val old = state.village.buildings.find { it.id == building.id }
            if (old != null && old.constructionStartedAt != null && building.constructionStartedAt == null) {
                ConnectionManager.sendToPlayer(userId, ServerMessage.BuildingComplete(
                    buildingId = building.id,
                    buildingType = building.type,
                    level = building.level,
                ))
            }
        }

        // Detect troops that just completed training
        for (armyTroop in updated.army.troops) {
            val oldCount = state.army.troops.find { it.type == armyTroop.type && it.level == armyTroop.level }?.count ?: 0
            if (armyTroop.count > oldCount) {
                ConnectionManager.sendToPlayer(userId, ServerMessage.TrainingComplete(
                    troopType = armyTroop.type,
                    level = armyTroop.level,
                ))
            }
        }

        // Push resource updates periodically
        val lastPush = lastResourcePush[userId] ?: 0
        if (now - lastPush >= RESOURCE_PUSH_INTERVAL_MS && updated.resources != state.resources) {
            ConnectionManager.sendToPlayer(userId, ServerMessage.ResourcesUpdated(updated.resources))
            lastResourcePush[userId] = now
        }

        // Save if anything changed
        if (updated != state) {
            VillageRepository.saveVillage(userId, updated)
        }
    }
}
