package it.curzel.tamahero.village

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.curzel.tamahero.models.*
import it.curzel.tamahero.network.GameSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FloatingText(
    val text: String,
    val color: androidx.compose.ui.graphics.Color,
    val createdAt: Long = System.currentTimeMillis(),
)

class VillageViewModel : ViewModel() {

    private val _buildings = MutableStateFlow<List<PlacedBuilding>>(emptyList())
    val buildings = _buildings.asStateFlow()

    private val _resources = MutableStateFlow(Resources())
    val resources = _resources.asStateFlow()

    private val _army = MutableStateFlow(Army())
    val army = _army.asStateFlow()

    private val _trainingQueue = MutableStateFlow(TrainingQueue())
    val trainingQueue = _trainingQueue.asStateFlow()

    private val _troops = MutableStateFlow<List<Troop>>(emptyList())
    val troops = _troops.asStateFlow()

    private val _activeEvent = MutableStateFlow<ActiveEvent?>(null)
    val activeEvent = _activeEvent.asStateFlow()

    private val _eventResult = MutableStateFlow<ServerMessage.EventEnded?>(null)
    val eventResult = _eventResult.asStateFlow()

    private val _floatingTexts = MutableStateFlow<List<FloatingText>>(emptyList())
    val floatingTexts = _floatingTexts.asStateFlow()

    private val _offlineSummary = MutableStateFlow<String?>(null)
    val offlineSummary = _offlineSummary.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var firstUpdate = true
    private var previousResources: Resources? = null

    init {
        viewModelScope.launch {
            GameSocketClient.events.collect { message ->
                when (message) {
                    is ServerMessage.GameStateUpdated -> {
                        val state = message.state
                        if (firstUpdate) {
                            firstUpdate = false
                            checkOfflineProgress(state)
                        }
                        trackResourceChanges(state.resources)
                        _buildings.value = state.village.buildings
                        _resources.value = state.resources
                        _army.value = state.army
                        _trainingQueue.value = state.trainingQueue
                        _troops.value = state.troops
                        _activeEvent.value = state.activeEvent
                    }
                    is ServerMessage.ResourcesUpdated -> {
                        trackResourceChanges(message.resources)
                        _resources.value = message.resources
                    }
                    is ServerMessage.BuildingComplete,
                    is ServerMessage.TrainingComplete -> {
                        GameSocketClient.getVillage()
                    }
                    is ServerMessage.EventEnded -> {
                        _eventResult.value = message
                        GameSocketClient.getVillage()
                    }
                    is ServerMessage.Error -> {
                        _errorMessage.value = message.reason
                    }
                    else -> {}
                }
            }
        }
    }

    fun dismissOfflineSummary() {
        _offlineSummary.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun collectEventRewards() {
        GameSocketClient.collectEventRewards()
        _eventResult.value = null
    }

    fun dismissEventResult() {
        _eventResult.value = null
    }

    private fun trackResourceChanges(newResources: Resources) {
        val prev = previousResources
        previousResources = newResources
        if (prev == null) return
        val diffs = mutableListOf<FloatingText>()
        val dCredits = newResources.credits - prev.credits
        val dAlloy = newResources.alloy - prev.alloy
        val dCrystal = newResources.crystal - prev.crystal
        val dPlasma = newResources.plasma - prev.plasma
        if (dCredits != 0L) diffs.add(FloatingText("${if (dCredits > 0) "+" else ""}${dCredits} credits", androidx.compose.ui.graphics.Color(0xFFFFD700)))
        if (dAlloy != 0L) diffs.add(FloatingText("${if (dAlloy > 0) "+" else ""}${dAlloy} alloy", androidx.compose.ui.graphics.Color(0xFF4CAF50)))
        if (dCrystal != 0L) diffs.add(FloatingText("${if (dCrystal > 0) "+" else ""}${dCrystal} crystal", androidx.compose.ui.graphics.Color(0xFF9E9E9E)))
        if (dPlasma != 0L) diffs.add(FloatingText("${if (dPlasma > 0) "+" else ""}${dPlasma} plasma", androidx.compose.ui.graphics.Color(0xFF7B1FA2)))
        if (diffs.isNotEmpty()) {
            _floatingTexts.value = (_floatingTexts.value + diffs).takeLast(10)
        }
    }

    private fun checkOfflineProgress(state: GameState) {
        val now = System.currentTimeMillis()
        val gap = now - state.lastUpdatedAt
        if (gap < 5 * 60 * 1000) return
        val minutes = gap / 60_000
        val summary = buildString {
            appendLine("While you were away (${minutes}m):")
            if (state.resources.credits > 0 || state.resources.alloy > 0) {
                appendLine("Resources accumulated")
            }
            val completed = state.village.buildings.count { it.constructionStartedAt == null && it.hp > 0 }
            appendLine("$completed buildings standing")
            if (state.army.totalCount > 0) {
                appendLine("Army: ${state.army.totalCount} troops ready")
            }
        }
        _offlineSummary.value = summary
    }
}
