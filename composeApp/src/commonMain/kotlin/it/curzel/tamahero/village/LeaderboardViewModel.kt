package it.curzel.tamahero.village

import it.curzel.tamahero.models.LeaderboardEntry
import it.curzel.tamahero.models.ServerMessage
import it.curzel.tamahero.network.GameSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    private val _yourRank = MutableStateFlow(0)
    val yourRank = _yourRank.asStateFlow()

    init {
        scope.launch {
            GameSocketClient.events.collect { message ->
                if (message is ServerMessage.Leaderboard) {
                    _entries.value = message.entries
                    _yourRank.value = message.yourRank
                }
            }
        }
    }

    fun refresh() = GameSocketClient.getLeaderboard()
}
