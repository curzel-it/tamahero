package it.curzel.tamahero.network

import it.curzel.tamahero.ServerConfig
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.ClientMessage
import it.curzel.tamahero.models.ProtocolJson
import it.curzel.tamahero.models.ServerMessage
import it.curzel.tamahero.models.TroopType
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class WsLogEntry(val timestamp: Long, val direction: String, val text: String)

object GameSocketClient {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var keepAliveJob: Job? = null

    private val _events = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    val events: SharedFlow<ServerMessage> = _events

    private val _messageLog = MutableStateFlow<List<WsLogEntry>>(emptyList())
    val messageLog = _messageLog.asStateFlow()

    private var connected = false

    fun connect(token: String) {
        val wsUrl = ServerConfig.BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws?token=$token"

        println("[GameSocketClient] Connecting to $wsUrl")
        GameSocketManager.connect(
            url = wsUrl,
            onMessage = { text ->
                println("[GameSocketClient] Received: ${text.take(100)}")
                scope.launch { handleMessage(text) }
            },
            onError = {
                println("[GameSocketClient] Error: $it")
                connected = false
            },
            onClose = {
                println("[GameSocketClient] Connection closed")
                connected = false
                keepAliveJob?.cancel()
            },
        )
    }

    fun disconnect() {
        keepAliveJob?.cancel()
        GameSocketManager.close()
        connected = false
    }

    fun getVillage() = send(ClientMessage.GetVillage)

    fun build(type: BuildingType, x: Int, y: Int) = send(ClientMessage.Build(type, x, y))

    fun upgrade(buildingId: Long) = send(ClientMessage.Upgrade(buildingId))

    fun move(buildingId: Long, x: Int, y: Int) = send(ClientMessage.Move(buildingId, x, y))

    fun collect(buildingId: Long) = send(ClientMessage.Collect(buildingId))

    fun collectAll() = send(ClientMessage.CollectAll)

    fun demolish(buildingId: Long) = send(ClientMessage.Demolish(buildingId))

    fun cancelConstruction(buildingId: Long) = send(ClientMessage.CancelConstruction(buildingId))

    fun speedUp(buildingId: Long) = send(ClientMessage.SpeedUp(buildingId))

    fun train(troopType: TroopType, count: Int = 1) = send(ClientMessage.Train(troopType, count))

    fun cancelTraining(index: Int) = send(ClientMessage.CancelTraining(index))

    fun rearmTrap(buildingId: Long) = send(ClientMessage.RearmTrap(buildingId))

    fun rearmAllTraps() = send(ClientMessage.RearmAllTraps)

    fun collectEventRewards() = send(ClientMessage.CollectEventRewards)

    fun findOpponent() = send(ClientMessage.FindOpponent)

    fun nextOpponent() = send(ClientMessage.NextOpponent)

    fun startPvp(targetId: Long) = send(ClientMessage.StartPvp(targetId))

    fun deployTroop(troopType: TroopType, x: Float, y: Float) = send(ClientMessage.DeployTroop(troopType, x, y))

    fun endBattle() = send(ClientMessage.EndBattle)

    fun getLeaderboard() = send(ClientMessage.GetLeaderboard)

    private fun send(message: ClientMessage) {
        val text = ProtocolJson.encodeToString(ClientMessage.serializer(), message)
        addLog("SENT", text)
        GameSocketManager.send(text)
    }

    private suspend fun handleMessage(text: String) {
        try {
            addLog("RECV", text)
            val message = ProtocolJson.decodeFromString(ServerMessage.serializer(), text)
            if (message is ServerMessage.Connected) {
                connected = true
                startKeepAlive()
            }
            _events.emit(message)
        } catch (_: Exception) {}
    }

    @OptIn(ExperimentalTime::class)
    private fun addLog(direction: String, text: String) {
        val entry = WsLogEntry(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            direction = direction,
            text = text,
        )
        _messageLog.value = (_messageLog.value + entry).takeLast(200)
    }

    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                delay(10_000)
                try {
                    send(ClientMessage.KeepAlive)
                } catch (_: Exception) {}
            }
        }
    }
}
