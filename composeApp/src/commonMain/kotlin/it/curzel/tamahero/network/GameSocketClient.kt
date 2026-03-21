package it.curzel.tamahero.network

import it.curzel.tamahero.ServerConfig
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.ClientMessage
import it.curzel.tamahero.models.ServerMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json

object GameSocketClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var keepAliveJob: Job? = null

    private val _events = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    val events: SharedFlow<ServerMessage> = _events

    private var connected = false

    fun connect(token: String) {
        val wsUrl = ServerConfig.BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/ws?token=$token"

        GameSocketManager.connect(
            url = wsUrl,
            onMessage = { text -> scope.launch { handleMessage(text) } },
            onError = { connected = false },
            onClose = {
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

    private fun send(message: ClientMessage) {
        val text = json.encodeToString(ClientMessage.serializer(), message)
        GameSocketManager.send(text)
    }

    private suspend fun handleMessage(text: String) {
        try {
            val message = json.decodeFromString(ServerMessage.serializer(), text)
            if (message is ServerMessage.Connected) {
                connected = true
                startKeepAlive()
            }
            _events.emit(message)
        } catch (_: Exception) {}
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
