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

    private val credentialsFile = java.io.File(
        System.getProperty("user.home"), ".tamahero/token.json"
    )

    private var session: WebSocketSession? = null
    private var token: String? = null
    private var playerId: Long? = null
    private var lastState: GameState? = null
    private var lastMatchTarget: Long? = null
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

    fun loadSavedToken(): Boolean {
        if (!credentialsFile.exists()) return false
        try {
            val saved = ProtocolJson.decodeFromString<SavedCredentials>(credentialsFile.readText())
            if (saved.baseUrl != baseUrl) return false
            token = saved.token
            println("Loaded saved session for ${saved.username}")
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun clearSavedToken() {
        credentialsFile.delete()
        token = null
        println("Logged out.")
    }

    private suspend fun handleAuthResponse(response: HttpResponse): Boolean {
        val body = response.bodyAsText()
        val auth = ProtocolJson.decodeFromString<AuthResponse>(body)
        if (auth.success && auth.token != null) {
            token = auth.token
            saveToken(auth.token, auth.username ?: "", auth.userId ?: 0)
            println("Authenticated as ${auth.username} (id: ${auth.userId})")
            return true
        }
        println("Auth failed: ${auth.error}")
        return false
    }

    private fun saveToken(token: String, username: String, userId: Long) {
        credentialsFile.parentFile.mkdirs()
        val creds = SavedCredentials(baseUrl = baseUrl, token = token, username = username, userId = userId)
        credentialsFile.writeText(ProtocolJson.encodeToString(SavedCredentials.serializer(), creds))
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
    fun getLastMatchTarget(): Long? = lastMatchTarget

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
                println("Resources: credits=${msg.resources.credits} metal=${msg.resources.metal} crystal=${msg.resources.crystal} deuterium=${msg.resources.deuterium}")
            }
            is ServerMessage.EventStarted -> {
                println("EVENT: ${msg.eventType} has started!")
            }
            is ServerMessage.EventEnded -> {
                val outcome = if (msg.success) "SUCCESS" else "FAILURE"
                println("EVENT: ${msg.eventType} ended — $outcome")
                println("  Rewards: credits=${msg.rewards.credits} metal=${msg.rewards.metal} crystal=${msg.rewards.crystal} deuterium=${msg.rewards.deuterium}")
                println("  Use 'collectrewards' to claim.")
            }
            is ServerMessage.OpponentFound -> {
                val m = msg.match
                println()
                println("=== Opponent Found ===")
                println("  ${m.targetName} (trophies: ${m.targetTrophies}, CC${m.targetCommandCenterLevel})")
                println("  Buildings: ${m.targetBase.buildings.size}")
                println("  Loot available: credits=${m.lootAvailable.credits} metal=${m.lootAvailable.metal} crystal=${m.lootAvailable.crystal} deuterium=${m.lootAvailable.deuterium}")
                println("  Use 'go' to attack or 'next' for another opponent")
                lastMatchTarget = m.targetId
            }
            is ServerMessage.NoOpponentFound -> {
                println("No opponent found: ${msg.reason}")
            }
            is ServerMessage.PvpBattleStarted -> {
                println()
                println("=== PvP Battle Started ===")
                println("  vs ${msg.battle.defenderName} (${msg.battle.defenderTrophies} trophies)")
                println("  Time limit: ${msg.battle.timeLimitMs / 1000}s")
                println("  Available troops:")
                for (t in msg.battle.availableTroops.troops) {
                    println("    ${t.type} lv${t.level} x${t.count}")
                }
                println("  Deploy troops with: deploy <type> <x> <y>")
                println("  Troops must be placed on map edges (x/y near 0 or 39)")
            }
            is ServerMessage.Leaderboard -> {
                println()
                println("=== Leaderboard ===")
                for (e in msg.entries) {
                    println("  #${e.rank} ${e.playerName} — ${e.trophies} trophies (CC${e.commandCenterLevel})")
                }
                if (msg.yourRank > 0) {
                    println("  Your rank: #${msg.yourRank}")
                }
            }
            is ServerMessage.PvpBattleTick -> {
                // Don't spam — just update silently, show on request
            }
            is ServerMessage.PvpBattleEnded -> {
                val r = msg.result
                println()
                println("=== Battle Ended ===")
                println("  Stars: ${"*".repeat(r.stars)}${"_".repeat(3 - r.stars)} (${r.destructionPercent}% destruction)")
                println("  Loot: credits=${r.loot.credits} metal=${r.loot.metal} crystal=${r.loot.crystal} deuterium=${r.loot.deuterium}")
                println("  Trophies: ${if (r.attackerTrophyDelta >= 0) "+" else ""}${r.attackerTrophyDelta}")
            }
            is ServerMessage.DefenseResult -> {
                val e = msg.entry
                println()
                println("=== You Were Attacked! ===")
                println("  Attacker: ${e.attackerName}")
                println("  Stars: ${"*".repeat(e.stars)}${"_".repeat(3 - e.stars)}")
                println("  Lost: credits=${e.lootLost.credits} metal=${e.lootLost.metal} crystal=${e.lootLost.crystal}")
                println("  Trophies: ${e.trophyDelta}")
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
        println("Trophies: ${state.trophies}")
        println("Resources: credits=${state.resources.credits} metal=${state.resources.metal} crystal=${state.resources.crystal} deuterium=${state.resources.deuterium}")
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
        state.activeEvent?.let { event ->
            val status = if (event.completed) "COMPLETED" else "ACTIVE (wave ${event.currentWave + 1}/${event.totalWaves})"
            println("Event: ${event.type} — $status")
            if (event.pendingRewards != null) {
                println("  Pending rewards — use 'collectrewards' to claim")
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

@kotlinx.serialization.Serializable
private data class SavedCredentials(
    val baseUrl: String,
    val token: String,
    val username: String,
    val userId: Long,
)
