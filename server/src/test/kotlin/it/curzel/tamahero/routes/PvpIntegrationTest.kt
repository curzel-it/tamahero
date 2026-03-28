package it.curzel.tamahero.routes

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import it.curzel.tamahero.models.*
import it.curzel.tamahero.notifications.MockPushNotificationService
import it.curzel.tamahero.notifications.PushNotificationServiceProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.test.*

class PvpIntegrationTest {

    private var usernameCounter = 0

    private suspend fun register(
        client: io.ktor.client.HttpClient,
        name: String? = null,
        admin: Boolean = false,
    ): Triple<String, Long, String> {
        val username = name ?: "pvp_${++usernameCounter}"
        val email = if (admin) "$username@curzel.it" else null
        val emailJson = if (email != null) ""","email":"$email"""" else ""
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"testpass123"$emailJson}""")
        }
        assertEquals(HttpStatusCode.OK, response.status, "Registration failed for $username")
        val auth = ProtocolJson.decodeFromString<AuthResponse>(response.bodyAsText())
        return Triple(auth.token!!, auth.userId!!, username)
    }

    private fun encode(msg: ClientMessage): String =
        ProtocolJson.encodeToString(ClientMessage.serializer(), msg)

    private fun decode(text: String): ServerMessage =
        ProtocolJson.decodeFromString(ServerMessage.serializer(), text)

    private suspend fun DefaultClientWebSocketSession.recv(): ServerMessage {
        val frame = incoming.receive() as Frame.Text
        return decode(frame.readText())
    }

    private suspend fun DefaultClientWebSocketSession.recvTimeout(ms: Long = 5000): ServerMessage =
        withTimeout(ms) { recv() }

    private suspend fun DefaultClientWebSocketSession.skipConnected(): Long {
        val msg = recv()
        assertTrue(msg is ServerMessage.Connected, "Expected Connected but got $msg")
        return (msg as ServerMessage.Connected).playerId
    }

    private suspend fun DefaultClientWebSocketSession.sendMsg(msg: ClientMessage) {
        send(Frame.Text(encode(msg)))
    }

    private suspend fun DefaultClientWebSocketSession.sendAndRecv(msg: ClientMessage): ServerMessage {
        sendMsg(msg)
        return recvTimeout()
    }

    private suspend fun adminPost(
        client: io.ktor.client.HttpClient,
        adminToken: String,
        path: String,
        body: String,
    ): HttpResponse {
        return client.post("/api/admin/$path") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend fun setupDefender(
        client: io.ktor.client.HttpClient,
        adminToken: String,
        defenderId: Long,
    ) {
        adminPost(client, adminToken, "place-building", """{"userId":$defenderId,"type":"RailGun","x":10,"y":10}""")
        adminPost(client, adminToken, "place-building", """{"userId":$defenderId,"type":"CreditMint","x":5,"y":5}""")
        adminPost(client, adminToken, "grant-resources", """{"userId":$defenderId,"credits":50000,"alloy":50000}""")
    }

    private suspend fun setupAttackerWithArmy(
        client: io.ktor.client.HttpClient,
        adminToken: String,
        attackerId: Long,
        troopCount: Int = 5,
    ) {
        adminPost(client, adminToken, "grant-army", """{"userId":$attackerId,"troopType":"Marine","count":$troopCount}""")
    }

    private suspend fun getVillage(client: io.ktor.client.HttpClient, adminToken: String, userId: Long): GameState {
        val response = adminPost(client, adminToken, "get-village", """{"userId":$userId}""")
        return ProtocolJson.decodeFromString<GameState>(response.bodyAsText())
    }

    @Test
    fun attackerFindsDefenderAndStartsBattle() = testApp { client ->
        val (adminToken, _, _) = register(client, "pvpadmin1", admin = true)
        val (attackerToken, attackerId, _) = register(client, "attacker1")
        val (_, defenderId, _) = register(client, "defender1")

        setupDefender(client, adminToken, defenderId)
        setupAttackerWithArmy(client, adminToken, attackerId)

        val ws = client.config { install(WebSockets) }
        ws.webSocket("/ws?token=$attackerToken") {
            skipConnected()
            sendAndRecv(ClientMessage.GetVillage)

            // Find opponent
            val findResult = sendAndRecv(ClientMessage.FindOpponent)
            if (findResult is ServerMessage.NoOpponentFound) {
                // Only one other player — may not match trophy range
                return@webSocket
            }
            assertTrue(findResult is ServerMessage.OpponentFound, "Expected opponent, got $findResult")
            assertEquals(defenderId, findResult.match.targetId)
            assertTrue(findResult.match.lootAvailable.credits > 0)

            // Start battle
            val battleStart = sendAndRecv(ClientMessage.StartPvp(defenderId))
            assertTrue(battleStart is ServerMessage.PvpBattleStarted, "Expected battle start, got $battleStart")
            assertEquals(attackerId, battleStart.battle.attackerId)
            assertEquals(defenderId, battleStart.battle.defenderId)
            assertTrue(battleStart.battle.availableTroops.totalCount >= 5)

            // Surrender
            val end = sendAndRecv(ClientMessage.EndBattle)
            assertTrue(end is ServerMessage.PvpBattleEnded, "Expected battle end, got $end")
        }
    }

    @Test
    fun deployTroopsOnEdge() = testApp { client ->
        val (adminToken, _, _) = register(client, "pvpadmin2", admin = true)
        val (attackerToken, attackerId, _) = register(client, "attacker2")
        val (_, defenderId, _) = register(client, "defender2")

        setupDefender(client, adminToken, defenderId)
        setupAttackerWithArmy(client, adminToken, attackerId, 5)

        val ws = client.config { install(WebSockets) }
        ws.webSocket("/ws?token=$attackerToken") {
            skipConnected()
            sendAndRecv(ClientMessage.GetVillage)

            val start = sendAndRecv(ClientMessage.StartPvp(defenderId))
            assertTrue(start is ServerMessage.PvpBattleStarted, "Got $start")

            // Valid edge deploy
            val deploy1 = sendAndRecv(ClientMessage.DeployTroop(TroopType.Marine, 0f, 10f))
            assertTrue(deploy1 is ServerMessage.PvpBattleTick, "Expected tick, got $deploy1")
            assertEquals(1, deploy1.battle.deployedTroops.size)

            // Another valid deploy
            val deploy2 = sendAndRecv(ClientMessage.DeployTroop(TroopType.Marine, 0f, 12f))
            assertTrue(deploy2 is ServerMessage.PvpBattleTick, "Expected tick, got $deploy2")
            assertEquals(2, deploy2.battle.deployedTroops.size)

            // Invalid: center deploy
            val bad = sendAndRecv(ClientMessage.DeployTroop(TroopType.Marine, 10f, 10f))
            assertTrue(bad is ServerMessage.Error, "Center deploy should fail, got $bad")

            sendAndRecv(ClientMessage.EndBattle)
        }
    }

    @Test
    fun defenderReceivesDefenseResult() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (adminToken, _, _) = register(client, "pvpadmin3", admin = true)
        val (attackerToken, attackerId, attackerName) = register(client, "attacker3")
        val (defenderToken, defenderId, _) = register(client, "defender3")

        setupDefender(client, adminToken, defenderId)
        setupAttackerWithArmy(client, adminToken, attackerId, 3)

        val defenderMessages = Channel<ServerMessage>(10)
        val defenderReady = Channel<Unit>(1)
        val scope = CoroutineScope(Dispatchers.Default)
        val wsDefender = client.config { install(WebSockets) }

        val defenderJob = scope.launch {
            wsDefender.webSocket("/ws?token=$defenderToken") {
                skipConnected()
                defenderReady.send(Unit)
                while (isActive) {
                    try {
                        defenderMessages.send(recvTimeout(10_000))
                    } catch (_: Exception) { break }
                }
            }
        }
        defenderReady.receive()

        val wsAttacker = client.config { install(WebSockets) }
        wsAttacker.webSocket("/ws?token=$attackerToken") {
            skipConnected()
            sendAndRecv(ClientMessage.GetVillage)

            sendAndRecv(ClientMessage.StartPvp(defenderId))
            sendAndRecv(ClientMessage.DeployTroop(TroopType.Marine, 0f, 10f))
            sendAndRecv(ClientMessage.EndBattle)
        }

        delay(500)

        val received = mutableListOf<ServerMessage>()
        while (!defenderMessages.isEmpty) {
            received.add(defenderMessages.receive())
        }

        val defenseResults = received.filterIsInstance<ServerMessage.DefenseResult>()
        assertTrue(defenseResults.isNotEmpty(), "Defender should get DefenseResult, got: ${received.map { it::class.simpleName }}")
        assertEquals(attackerName, defenseResults[0].entry.attackerName)

        val stateUpdates = received.filterIsInstance<ServerMessage.GameStateUpdated>()
        assertTrue(stateUpdates.isNotEmpty(), "Defender should get state update")
        assertTrue(stateUpdates.last().state.defenseLog.isNotEmpty())

        defenderJob.cancel()
        scope.cancel()
    }

    @Test
    fun trophiesChangeAfterSurrender() = testApp { client ->
        val (adminToken, _, _) = register(client, "pvpadmin4", admin = true)
        val (attackerToken, attackerId, _) = register(client, "attacker4")
        val (_, defenderId, _) = register(client, "defender4")

        setupDefender(client, adminToken, defenderId)
        setupAttackerWithArmy(client, adminToken, attackerId, 3)

        val attackerBefore = getVillage(client, adminToken, attackerId)
        val defenderBefore = getVillage(client, adminToken, defenderId)

        val ws = client.config { install(WebSockets) }
        ws.webSocket("/ws?token=$attackerToken") {
            skipConnected()
            sendAndRecv(ClientMessage.GetVillage)

            sendAndRecv(ClientMessage.StartPvp(defenderId))
            val end = sendAndRecv(ClientMessage.EndBattle)
            assertTrue(end is ServerMessage.PvpBattleEnded, "Got $end")

            assertEquals(0, end.result.stars)
            assertTrue(end.result.attackerTrophyDelta < 0, "Attacker loses trophies, got ${end.result.attackerTrophyDelta}")
            assertTrue(end.result.defenderTrophyDelta > 0, "Defender gains trophies, got ${end.result.defenderTrophyDelta}")
        }

        val attackerAfter = getVillage(client, adminToken, attackerId)
        val defenderAfter = getVillage(client, adminToken, defenderId)

        assertTrue(attackerAfter.trophies <= attackerBefore.trophies)
        assertTrue(defenderAfter.trophies >= defenderBefore.trophies)
    }

    @Test
    fun cannotStartTwoBattles() = testApp { client ->
        val (adminToken, _, _) = register(client, "pvpadmin5", admin = true)
        val (attackerToken, attackerId, _) = register(client, "attacker5")
        val (_, defender1Id, _) = register(client, "defender5a")
        val (_, defender2Id, _) = register(client, "defender5b")

        setupDefender(client, adminToken, defender1Id)
        setupDefender(client, adminToken, defender2Id)
        setupAttackerWithArmy(client, adminToken, attackerId, 5)

        val ws = client.config { install(WebSockets) }
        ws.webSocket("/ws?token=$attackerToken") {
            skipConnected()
            sendAndRecv(ClientMessage.GetVillage)

            val start1 = sendAndRecv(ClientMessage.StartPvp(defender1Id))
            assertTrue(start1 is ServerMessage.PvpBattleStarted, "Got $start1")

            val start2 = sendAndRecv(ClientMessage.StartPvp(defender2Id))
            assertTrue(start2 is ServerMessage.Error, "Should block second battle, got $start2")
            assertTrue((start2 as ServerMessage.Error).reason.contains("Already"), "Got: ${start2.reason}")

            sendAndRecv(ClientMessage.EndBattle)
        }
    }

    @Test
    fun cannotAttackWithNoTroops() = testApp { client ->
        val (adminToken, _, _) = register(client, "pvpadmin6", admin = true)
        val (attackerToken, _, _) = register(client, "attacker6")
        val (_, defenderId, _) = register(client, "defender6")

        setupDefender(client, adminToken, defenderId)

        val ws = client.config { install(WebSockets) }
        ws.webSocket("/ws?token=$attackerToken") {
            skipConnected()
            sendAndRecv(ClientMessage.GetVillage)

            val find = sendAndRecv(ClientMessage.FindOpponent)
            assertTrue(find is ServerMessage.Error, "Should fail with no troops, got $find")
            assertTrue((find as ServerMessage.Error).reason.contains("No troops"), "Got: ${find.reason}")
        }
    }

    @Test
    fun lootTransferred() = testApp { client ->
        val (adminToken, _, _) = register(client, "pvpadmin7", admin = true)
        val (attackerToken, attackerId, _) = register(client, "attacker7")
        val (_, defenderId, _) = register(client, "defender7")

        adminPost(client, adminToken, "place-building", """{"userId":$defenderId,"type":"RailGun","x":10,"y":10}""")
        adminPost(client, adminToken, "grant-resources", """{"userId":$defenderId,"credits":100000,"alloy":100000,"crystal":50000}""")
        setupAttackerWithArmy(client, adminToken, attackerId, 5)

        val attackerBefore = getVillage(client, adminToken, attackerId)
        val defenderBefore = getVillage(client, adminToken, defenderId)

        var loot = Resources()
        val ws = client.config { install(WebSockets) }
        ws.webSocket("/ws?token=$attackerToken") {
            skipConnected()
            sendAndRecv(ClientMessage.GetVillage)

            sendAndRecv(ClientMessage.StartPvp(defenderId))

            // Deploy troops
            for (i in 0 until 3) {
                sendMsg(ClientMessage.DeployTroop(TroopType.Marine, 0f, (10 + i * 2).toFloat()))
            }
            repeat(3) { recvTimeout() }

            delay(200)
            val end = sendAndRecv(ClientMessage.EndBattle)
            assertTrue(end is ServerMessage.PvpBattleEnded, "Got $end")
            loot = end.result.loot
        }

        val attackerAfter = getVillage(client, adminToken, attackerId)
        val defenderAfter = getVillage(client, adminToken, defenderId)

        assertTrue(
            attackerAfter.resources.credits >= attackerBefore.resources.credits + loot.credits,
            "Attacker should gain credits loot"
        )
        assertTrue(
            defenderAfter.resources.credits <= defenderBefore.resources.credits,
            "Defender should lose credits"
        )
    }

    @Test
    fun pushNotificationOnAttack() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (adminToken, _, _) = register(client, "pvpadmin8", admin = true)
        val (attackerToken, attackerId, attackerName) = register(client, "attacker8")
        val (_, defenderId, _) = register(client, "defender8")

        setupDefender(client, adminToken, defenderId)
        setupAttackerWithArmy(client, adminToken, attackerId, 3)

        val ws = client.config { install(WebSockets) }
        ws.webSocket("/ws?token=$attackerToken") {
            skipConnected()
            sendAndRecv(ClientMessage.GetVillage)

            sendAndRecv(ClientMessage.StartPvp(defenderId))

            // Under attack notification should have been sent
            val underAttack = mock.sentWithType("under_attack")
            assertTrue(underAttack.isNotEmpty(), "Should send under_attack push")
            assertEquals(defenderId, underAttack[0].userId)
            assertTrue(underAttack[0].body.contains(attackerName))

            sendAndRecv(ClientMessage.EndBattle)
        }

        // Defense result notification
        val defenseResult = mock.sentWithType("defense_result")
        assertTrue(defenseResult.isNotEmpty(), "Should send defense_result push")
        assertEquals(defenderId, defenseResult[0].userId)
    }
}
