package it.curzel.tamahero.cli

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import it.curzel.tamahero.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class CliClient(private val baseUrl: String) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(ProtocolJson) }
        install(WebSockets)
    }

    private var session: WebSocketSession? = null
    private var token: String? = null
    private var playerId: Long? = null
    private var lastState: GameState? = null
    private val responseChannel = Channel<ServerMessage>(Channel.BUFFERED)

    suspend fun register(username: String, password: String): Boolean {
        val response = httpClient.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }
        return handleAuthResponse(response)
    }

    suspend fun login(username: String, password: String): Boolean {
        val response = httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }
        return handleAuthResponse(response)
    }

    private suspend fun handleAuthResponse(response: HttpResponse): Boolean {
        val body = response.bodyAsText()
        val auth = ProtocolJson.decodeFromString<AuthResponse>(body)
        if (auth.success && auth.token != null) {
            token = auth.token
            println("Authenticated as ${auth.username} (id: ${auth.userId})")
            return true
        }
        println("Auth failed: ${auth.error}")
        return false
    }

    suspend fun connect() {
        val connectedSignal = CompletableDeferred<Unit>()
        startWebSocket(connectedSignal, forwardResponses = false)
        connectedSignal.await()
    }

    suspend fun connectInBackground(): Job {
        val connectedSignal = CompletableDeferred<Unit>()
        val job = startWebSocket(connectedSignal, forwardResponses = true)
        connectedSignal.await()
        return job
    }

    private fun startWebSocket(connectedSignal: CompletableDeferred<Unit>, forwardResponses: Boolean): Job {
        val t = token ?: throw IllegalStateException("Not authenticated. Login first.")
        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws?token=$t"

        return CoroutineScope(Dispatchers.Default).launch {
            httpClient.webSocket(wsUrl) {
                session = this

                launch {
                    while (isActive) {
                        delay(10_000)
                        send(Frame.Text(ProtocolJson.encodeToString(ClientMessage.serializer(), ClientMessage.KeepAlive)))
                    }
                }

                for (frame in this.incoming) {
                    if (frame is Frame.Text) {
                        val msg = ProtocolJson.decodeFromString(ServerMessage.serializer(), frame.readText())
                        handleServerMessage(msg)
                        if (msg is ServerMessage.Connected) {
                            connectedSignal.complete(Unit)
                        } else if (forwardResponses) {
                            responseChannel.send(msg)
                        }
                    }
                }
            }
        }
    }

    fun demolish(buildingId: Long) = sendAsync(ClientMessage.Demolish(buildingId))

    fun cancelConstruction(buildingId: Long) = sendAsync(ClientMessage.CancelConstruction(buildingId))

    fun speedUp(buildingId: Long) = sendAsync(ClientMessage.SpeedUp(buildingId))

    fun collectAll() = sendAsync(ClientMessage.CollectAll)

    private fun sendAsync(message: ClientMessage) {
        val text = ProtocolJson.encodeToString(ClientMessage.serializer(), message)
        val s = session ?: run { println("Not connected."); return }
        CoroutineScope(Dispatchers.Default).launch {
            s.send(Frame.Text(text))
        }
    }

    suspend fun send(message: ClientMessage) {
        val s = session ?: run { println("Not connected."); return }
        s.send(Frame.Text(ProtocolJson.encodeToString(ClientMessage.serializer(), message)))
    }

    suspend fun sendAndReceive(message: ClientMessage): ServerMessage? {
        send(message)
        return withTimeoutOrNull(5000) { responseChannel.receive() }
    }

    fun getLastState(): GameState? = lastState

    private fun handleServerMessage(msg: ServerMessage) {
        when (msg) {
            is ServerMessage.Connected -> {
                playerId = msg.playerId
                println("Connected (playerId: ${msg.playerId}, protocol: v${msg.protocolVersion})")
            }
            is ServerMessage.GameStateUpdated -> {
                lastState = msg.state
                printGameState(msg.state)
            }
            is ServerMessage.TrainingComplete -> {
                println("Training complete: ${msg.troopType} level ${msg.level}")
            }
            is ServerMessage.BuildingComplete -> {
                println("Building complete: ${msg.buildingType} level ${msg.level} (id: ${msg.buildingId})")
            }
            is ServerMessage.ResourcesUpdated -> {
                println("Resources: gold=${msg.resources.gold} wood=${msg.resources.wood} metal=${msg.resources.metal} mana=${msg.resources.mana}")
            }
            is ServerMessage.Error -> {
                println("Error: ${msg.reason}")
                if (msg.details.isNotEmpty()) println("  Details: ${msg.details}")
            }
        }
    }

    private fun printGameState(state: GameState) {
        println()
        println("=== Village (player ${state.playerId}) ===")
        println("Resources: gold=${state.resources.gold} wood=${state.resources.wood} metal=${state.resources.metal} mana=${state.resources.mana}")
        println("Buildings (${state.village.buildings.size}):")
        for (b in state.village.buildings) {
            val status = if (b.constructionStartedAt != null) " [BUILDING]" else ""
            println("  [${b.id}] ${b.type} lv${b.level} at (${b.x},${b.y}) hp=${b.hp}$status")
        }
        if (state.army.troops.isNotEmpty()) {
            println("Army (${state.army.totalCount} troops):")
            for (t in state.army.troops) {
                println("  ${t.type} lv${t.level} x${t.count}")
            }
        }
        if (state.trainingQueue.entries.isNotEmpty()) {
            println("Training queue (${state.trainingQueue.entries.size}):")
            for ((i, e) in state.trainingQueue.entries.withIndex()) {
                val status = if (e.startedAt != null) " [TRAINING]" else ""
                println("  [$i] ${e.troopType} lv${e.level}$status")
            }
        }
        if (state.troops.isNotEmpty()) {
            println("Battle troops (${state.troops.size}):")
            for (t in state.troops) {
                println("  [${t.id}] ${t.type} lv${t.level} hp=${t.hp} at (${t.x},${t.y})")
            }
        }
        println()
    }

    fun close() {
        httpClient.close()
    }
}

@kotlinx.serialization.Serializable
private data class AuthResponse(
    val success: Boolean,
    val userId: Long? = null,
    val token: String? = null,
    val username: String? = null,
    val error: String? = null,
)
