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
            // Drain resources by building many gold mines (50 wood each)
            for (i in 0..19) {
                send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.GoldMine, (i % 20) * 2, (i / 20) * 2))))
                receiveServerMessage()
            }
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.GoldMine, 0, 10))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Insufficient"))
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
