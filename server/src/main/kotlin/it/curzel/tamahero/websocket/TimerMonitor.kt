package it.curzel.tamahero.websocket

import it.curzel.tamahero.db.VillageRepository
import it.curzel.tamahero.models.*
import it.curzel.tamahero.notifications.PushNotificationServiceProvider
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
                PushNotificationServiceProvider.instance.notifyBuildingComplete(userId, building.type.name, building.level)
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
                PushNotificationServiceProvider.instance.notifyTrainingComplete(userId, armyTroop.type.name, armyTroop.level)
            }
        }

        // Push resource updates periodically
        val lastPush = lastResourcePush[userId] ?: 0
        if (now - lastPush >= RESOURCE_PUSH_INTERVAL_MS && updated.resources != state.resources) {
            ConnectionManager.sendToPlayer(userId, ServerMessage.ResourcesUpdated(updated.resources))
            lastResourcePush[userId] = now
        }

        // Detect event state changes
        val oldEvent = state.activeEvent
        val newEvent = updated.activeEvent
        if (oldEvent == null && newEvent != null) {
            ConnectionManager.sendToPlayer(userId, ServerMessage.EventStarted(newEvent.type))
        }
        if (oldEvent?.completed != true && newEvent?.completed == true) {
            val success = newEvent.pendingRewards == newEvent.rewards.success
            ConnectionManager.sendToPlayer(userId, ServerMessage.EventEnded(
                eventType = newEvent.type,
                success = success,
                rewards = newEvent.pendingRewards ?: Resources(),
            ))
            PushNotificationServiceProvider.instance.notifyEventEnded(userId, newEvent.type.name, success)
        }

        // Trigger random events
        var finalState = updated
        if (finalState.activeEvent == null && shouldTriggerEvent(finalState, now)) {
            val thLevel = finalState.village.buildings
                .filter { it.type == BuildingType.CommandCenter && it.constructionStartedAt == null }
                .maxOfOrNull { it.level } ?: 1
            val eligible = PveEventConfig.eligibleEvents(thLevel)
            if (eligible.isNotEmpty()) {
                val eventType = eligible[(now % eligible.size).toInt()]
                finalState = PveEventUpdateUseCase.startEvent(finalState, eventType, now)
                ConnectionManager.sendToPlayer(userId, ServerMessage.EventStarted(eventType))
                PushNotificationServiceProvider.instance.notifyEventStarted(userId, eventType.name)
            }
        }

        // Save if anything changed
        if (finalState != state) {
            VillageRepository.saveVillage(userId, finalState)
        }
    }

    private fun shouldTriggerEvent(state: GameState, now: Long): Boolean {
        if (state.troops.isNotEmpty()) return false
        if (state.shieldExpiresAt > now) return false
        // Villages without defenses are immune to PvE events
        val hasDefenses = state.village.buildings.any {
            it.type.isDefense && it.constructionStartedAt == null
        }
        if (!hasDefenses) return false
        // Treat lastEventAt=0 (never had an event) as "just now" to avoid instant trigger
        val lastEvent = if (state.lastEventAt == 0L) state.lastUpdatedAt else state.lastEventAt
        val timeSinceLastEvent = now - lastEvent
        if (timeSinceLastEvent < PveEventConfig.MIN_EVENT_INTERVAL_MS) return false
        val probability = (timeSinceLastEvent - PveEventConfig.MIN_EVENT_INTERVAL_MS).toDouble() /
            (PveEventConfig.MAX_EVENT_INTERVAL_MS - PveEventConfig.MIN_EVENT_INTERVAL_MS)
        val roll = ((now * 31 + state.playerId * 17) % 1000) / 1000.0
        return roll < probability.coerceIn(0.0, 1.0)
    }
}
