package it.curzel.tamahero.village

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.models.Resources
import it.curzel.tamahero.models.ServerMessage
import it.curzel.tamahero.network.GameSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VillageViewModel : ViewModel() {

    private val _buildings = MutableStateFlow<List<PlacedBuilding>>(emptyList())
    val buildings = _buildings.asStateFlow()

    private val _resources = MutableStateFlow(Resources())
    val resources = _resources.asStateFlow()

    init {
        viewModelScope.launch {
            GameSocketClient.events.collect { message ->
                when (message) {
                    is ServerMessage.GameStateUpdated -> {
                        _buildings.value = message.state.village.buildings
                        _resources.value = message.state.resources
                    }
                    is ServerMessage.ResourcesUpdated -> {
                        _resources.value = message.resources
                    }
                    is ServerMessage.BuildingComplete,
                    is ServerMessage.TrainingComplete,
                    is ServerMessage.EventEnded -> {
                        GameSocketClient.getVillage()
                    }
                    else -> {}
                }
            }
        }
    }
}
