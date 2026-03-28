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
            assertEquals(2, state.village.buildings.size)
            assertTrue(state.village.buildings.any { it.type == BuildingType.CommandCenter })
            assertTrue(state.village.buildings.any { it.type == BuildingType.RoboticsFactory })
            assertEquals(500, state.resources.credits)
            assertEquals(500, state.resources.metal)
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
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.MetalMine, 5, 5))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.GameStateUpdated)
            val state = msg.state
            assertTrue(state.village.buildings.any { it.type == BuildingType.MetalMine })
            assertEquals(450, state.resources.credits)
        }
    }

    @Test
    fun actionFailsInsufficientResources() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=$token") {
            skipConnected()
            val buildings = listOf(
                ClientMessage.Build(BuildingType.MetalMine, 0, 0),
                ClientMessage.Build(BuildingType.MetalMine, 0, 5),
                ClientMessage.Build(BuildingType.MetalStorage, 0, 10),
                ClientMessage.Build(BuildingType.GaussCannon, 0, 15),
                ClientMessage.Build(BuildingType.LightLaser, 5, 0),
                ClientMessage.Build(BuildingType.LightLaser, 5, 5),
                ClientMessage.Build(BuildingType.GaussCannon, 5, 10),
            )
            for (b in buildings) {
                send(Frame.Text(sendMessage(b)))
                val resp = receiveServerMessage()
                if (resp is ServerMessage.Error) break
            }
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.MissileLauncher, 5, 0))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.Error, "Expected error but got: $msg")
        }
    }

    @Test
    fun buildFailsOutOfBounds() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient = client.config { install(WebSockets) }
        wsClient.webSocket("/ws?token=$token") {
            skipConnected()
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.MetalMine, 39, 39))))
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
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.MetalMine, 5, 5))))
            receiveServerMessage()
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.MetalStorage, 5, 5))))
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
            val commandCenter = getMsg.state.village.buildings.first { it.type == BuildingType.CommandCenter }

            send(Frame.Text(sendMessage(ClientMessage.Move(commandCenter.id, 0, 0))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.GameStateUpdated)
            val moved = msg.state.village.buildings.first { it.id == commandCenter.id }
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
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.MetalMine, 5, 5))))
            val buildMsg = receiveServerMessage() as ServerMessage.GameStateUpdated
            val refinery = buildMsg.state.village.buildings.first { it.type == BuildingType.MetalMine }

            send(Frame.Text(sendMessage(ClientMessage.Collect(refinery.id))))
            val msg = receiveServerMessage()
            assertTrue(msg is ServerMessage.GameStateUpdated)
        }
    }
}
