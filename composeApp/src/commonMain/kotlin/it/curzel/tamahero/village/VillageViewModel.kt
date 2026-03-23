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

    private val _hero = MutableStateFlow(Hero())
    val hero = _hero.asStateFlow()

    private val _floatingTexts = MutableStateFlow<List<FloatingText>>(emptyList())
    val floatingTexts = _floatingTexts.asStateFlow()

    private val _offlineSummary = MutableStateFlow<String?>(null)
    val offlineSummary = _offlineSummary.asStateFlow()

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
                        _hero.value = state.hero
                    }
                    is ServerMessage.ResourcesUpdated -> {
                        trackResourceChanges(message.resources)
                        _resources.value = message.resources
                    }
                    is ServerMessage.BuildingComplete,
                    is ServerMessage.TrainingComplete,
                    is ServerMessage.EventEnded,
                    is ServerMessage.HeroLevelUp -> {
                        GameSocketClient.getVillage()
                    }
                    else -> {}
                }
            }
        }
    }

    fun dismissOfflineSummary() {
        _offlineSummary.value = null
    }

    private fun trackResourceChanges(newResources: Resources) {
        val prev = previousResources
        previousResources = newResources
        if (prev == null) return
        val diffs = mutableListOf<FloatingText>()
        val dGold = newResources.gold - prev.gold
        val dWood = newResources.wood - prev.wood
        val dMetal = newResources.metal - prev.metal
        val dMana = newResources.mana - prev.mana
        if (dGold != 0L) diffs.add(FloatingText("${if (dGold > 0) "+" else ""}${dGold} gold", androidx.compose.ui.graphics.Color(0xFFFFD700)))
        if (dWood != 0L) diffs.add(FloatingText("${if (dWood > 0) "+" else ""}${dWood} wood", androidx.compose.ui.graphics.Color(0xFF4CAF50)))
        if (dMetal != 0L) diffs.add(FloatingText("${if (dMetal > 0) "+" else ""}${dMetal} metal", androidx.compose.ui.graphics.Color(0xFF9E9E9E)))
        if (dMana != 0L) diffs.add(FloatingText("${if (dMana > 0) "+" else ""}${dMana} mana", androidx.compose.ui.graphics.Color(0xFF7B1FA2)))
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
            if (state.resources.gold > 0 || state.resources.wood > 0) {
                appendLine("Resources accumulated")
            }
            val completed = state.village.buildings.count { it.constructionStartedAt == null && it.hp > 0 }
            appendLine("$completed buildings standing")
            if (state.army.totalCount > 0) {
                appendLine("Army: ${state.army.totalCount} troops ready")
            }
            appendLine("Hero: Lv${state.hero.level}, hunger ${state.hero.hunger}%, happy ${state.hero.happiness}%")
        }
        _offlineSummary.value = summary
    }
}
