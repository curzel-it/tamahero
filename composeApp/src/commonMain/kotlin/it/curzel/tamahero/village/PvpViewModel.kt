package it.curzel.tamahero.village

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.curzel.tamahero.models.*
import it.curzel.tamahero.network.GameSocketClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PvpPhase {
    Idle,
    Searching,
    Scouting,
    Battling,
    Results,
}

class PvpViewModel : ViewModel() {

    private val _phase = MutableStateFlow(PvpPhase.Idle)
    val phase = _phase.asStateFlow()

    private val _match = MutableStateFlow<MatchmakingResult?>(null)
    val match = _match.asStateFlow()

    private val _battle = MutableStateFlow<PvpBattle?>(null)
    val battle = _battle.asStateFlow()

    private val _result = MutableStateFlow<PvpResult?>(null)
    val result = _result.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        viewModelScope.launch {
            GameSocketClient.events.collect { message ->
                when (message) {
                    is ServerMessage.OpponentFound -> {
                        _match.value = message.match
                        _phase.value = PvpPhase.Scouting
                        _error.value = null
                    }
                    is ServerMessage.NoOpponentFound -> {
                        _error.value = message.reason.ifEmpty { "No opponents found" }
                        _phase.value = PvpPhase.Idle
                    }
                    is ServerMessage.PvpBattleStarted -> {
                        _battle.value = message.battle
                        _phase.value = PvpPhase.Battling
                        _error.value = null
                    }
                    is ServerMessage.PvpBattleTick -> {
                        _battle.value = message.battle
                    }
                    is ServerMessage.PvpBattleEnded -> {
                        _result.value = message.result
                        _phase.value = PvpPhase.Results
                    }
                    is ServerMessage.Error -> {
                        if (_phase.value != PvpPhase.Idle) {
                            _error.value = message.reason
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun findOpponent() {
        _phase.value = PvpPhase.Searching
        _error.value = null
        GameSocketClient.findOpponent()
    }

    fun nextOpponent() {
        _phase.value = PvpPhase.Searching
        GameSocketClient.nextOpponent()
    }

    fun startBattle() {
        val target = _match.value ?: return
        GameSocketClient.startPvp(target.targetId)
    }

    fun deployTroop(troopType: TroopType, x: Float, y: Float) {
        GameSocketClient.deployTroop(troopType, x, y)
    }

    fun surrender() {
        GameSocketClient.endBattle()
    }

    fun dismiss() {
        _phase.value = PvpPhase.Idle
        _match.value = null
        _battle.value = null
        _result.value = null
        _error.value = null
    }
}
