package it.curzel.tamahero.routes

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import it.curzel.tamahero.models.*
import kotlin.test.*

class VillageWebSocketTest {

    private var usernameCounter = 0

    private suspend fun registerAndGetToken(client: io.ktor.client.HttpClient): String {
        val username = "testuser_${++usernameCounter}"
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"testpass123"}""")
        }
        val body = response.bodyAsText()
        val authResponse = ProtocolJson.decodeFromString<AuthResponse>(body)
        assertNotNull(authResponse.token, "Registration failed: $body")
        return authResponse.token!!
    }

    private fun sendMessage(msg: ClientMessage): String =
        ProtocolJson.encodeToString(ClientMessage.serializer(), msg)

    private fun parseServerMessage(text: String): ServerMessage =
        ProtocolJson.decodeFromString(ServerMessage.serializer(), text)

    private suspend fun DefaultClientWebSocketSession.receiveServerMessage(): ServerMessage {
        val frame = incoming.receive() as Frame.Text
        return parseServerMessage(frame.readText())
    }

    private suspend fun DefaultClientWebSocketSession.skipConnected(): ServerMessage.Connected {
        val msg = receiveServerMessage()
        assertTrue(msg is ServerMessage.Connected)
        return msg
    }

    @Test
    fun getVillageReturnsDefaultForNewUser() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=$token") {
            skipConnected()
            send(Frame.Text(sendMessage(ClientMessage.GetVillage)))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.GameStateUpdated)
            val state = msg.state
            assertEquals(1, state.village.buildings.size)
            assertTrue(state.village.buildings.any { it.type == BuildingType.TownHall })
            assertEquals(1000, state.resources.gold)
            assertEquals(1000, state.resources.wood)
        }
    }

    @Test
    fun invalidTokenClosesConnection() = testApp { client ->
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=invalid") {
            val reason = closeReason.await()
            assertNotNull(reason)
        }
    }

    @Test
    fun buildPlacesBuilding() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=$token") {
            skipConnected()
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.GameStateUpdated)
            val state = msg.state
            assertTrue(state.village.buildings.any { it.type == BuildingType.LumberCamp })
            assertEquals(950, state.resources.gold)
        }
    }

    @Test
    fun buildFailsInsufficientResources() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=$token") {
            skipConnected()
            // Start: 1000g, 1000w, 500m. ArcherTower costs 200g + 100w. Build until broke.
            // TH1 limit for ArcherTower is 1, so build 1 then use cannons (limit 2, 200g each)
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.ArcherTower, 0, 0))))
            receiveServerMessage() // 800g, 900w
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.Cannon, 4, 0))))
            receiveServerMessage() // 600g, 900w
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.Cannon, 8, 0))))
            receiveServerMessage() // 400g, 900w
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.LumberCamp, 12, 0))))
            receiveServerMessage() // 350g, 900w
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.LumberCamp, 0, 4))))
            receiveServerMessage() // 300g, 900w
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.GoldMine, 4, 4))))
            receiveServerMessage() // 300g, 850w
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.GoldMine, 8, 4))))
            receiveServerMessage() // 300g, 800w
            // Now try Cannon again — we already have 2 (maxed), so it'll hit the limit, not resources
            // Instead try ArcherTower which costs 200g+100w — we have 300g, can afford
            // But ArcherTower limit is 1, already built. Try Barracks: 100g+100w
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.Barracks, 12, 4))))
            receiveServerMessage() // 200g, 700w
            // Try to build another Barracks (limit 1 at TH1) — this hits limit, not resources
            // We need something with no limit issues. Walls cost 20w, no limit.
            // Spend remaining wood: 700w / 20w = 35 walls
            for (i in 0 until 35) {
                send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.Wall, i % 35, 8 + (i / 35)))))
                receiveServerMessage()
            }
            // Now: ~200g, ~0w. Try to build a GoldStorage (costs 50w) — insufficient wood
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.GoldStorage, 0, 12))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.Error, "Expected error but got: $msg")
            assertTrue((msg as ServerMessage.Error).reason.contains("Insufficient"), "Expected 'Insufficient' but got: ${msg.reason}")
        }
    }

    @Test
    fun buildFailsOutOfBounds() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=$token") {
            skipConnected()
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.LumberCamp, 39, 39))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("bounds"))
        }
    }

    @Test
    fun buildFailsOverlap() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=$token") {
            skipConnected()
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))))
            receiveServerMessage()
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.GoldMine, 5, 5))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("overlap", ignoreCase = true))
        }
    }

    @Test
    fun moveBuilding() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=$token") {
            skipConnected()
            send(Frame.Text(sendMessage(ClientMessage.GetVillage)))
            val getMsg = receiveServerMessage() as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.TownHall }

            send(Frame.Text(sendMessage(ClientMessage.Move(townHall.id, 0, 0))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.GameStateUpdated)
            val moved = msg.state.village.buildings.first { it.id == townHall.id }
            assertEquals(0, moved.x)
            assertEquals(0, moved.y)
        }
    }

    @Test
    fun collectResources() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=$token") {
            skipConnected()
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))))
            val buildMsg = receiveServerMessage() as ServerMessage.GameStateUpdated
            val lumberCamp = buildMsg.state.village.buildings.first { it.type == BuildingType.LumberCamp }

            send(Frame.Text(sendMessage(ClientMessage.Collect(lumberCamp.id))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.GameStateUpdated)
        }
    }
}
