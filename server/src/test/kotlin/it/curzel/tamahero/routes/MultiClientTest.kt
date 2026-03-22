package it.curzel.tamahero.routes

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import it.curzel.tamahero.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.test.*

class MultiClientTest {

    private var usernameCounter = 0

    private suspend fun registerAndGetToken(client: io.ktor.client.HttpClient): String {
        val username = "multitest_${++usernameCounter}"
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

    private suspend fun DefaultClientWebSocketSession.skipConnected() {
        val msg = receiveServerMessage()
        assertTrue(msg is ServerMessage.Connected)
    }

    @Test
    fun bothClientsReceiveUpdateWhenOneActs() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient1 = client.config { install(WebSockets) }
        val wsClient2 = client.config { install(WebSockets) }

        val client2Ready = Channel<Unit>(1)
        val client2Message = Channel<ServerMessage>(1)
        val scope = CoroutineScope(Dispatchers.Default)

        val client2Job = scope.launch {
            wsClient2.webSocket("/ws?token=$token") {
                skipConnected()
                client2Ready.send(Unit)
                val msg = receiveServerMessage()
                client2Message.send(msg)
            }
        }

        client2Ready.receive()

        wsClient1.webSocket("/ws?token=$token") {
            skipConnected()
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))))
            val msg1 = receiveServerMessage()
            assertTrue(msg1 is ServerMessage.GameStateUpdated)
            assertTrue(msg1.state.village.buildings.any { it.type == BuildingType.LumberCamp })
        }

        val msg2 = withTimeout(5000) { client2Message.receive() }
        assertTrue(msg2 is ServerMessage.GameStateUpdated, "Client 2 should receive GameStateUpdated but got $msg2")
        assertTrue(msg2.state.village.buildings.any { it.type == BuildingType.LumberCamp })

        client2Job.cancel()
        scope.cancel()
    }

    @Test
    fun bothClientsReceiveConsistentState() = testApp { client ->
        val token = registerAndGetToken(client)
        val wsClient1 = client.config { install(WebSockets) }
        val wsClient2 = client.config { install(WebSockets) }

        val client2Ready = Channel<Unit>(1)
        val client2Messages = Channel<ServerMessage>(10)
        val scope = CoroutineScope(Dispatchers.Default)

        val client2Job = scope.launch {
            wsClient2.webSocket("/ws?token=$token") {
                skipConnected()
                client2Ready.send(Unit)
                repeat(2) {
                    val msg = receiveServerMessage()
                    client2Messages.send(msg)
                }
            }
        }

        client2Ready.receive()

        wsClient1.webSocket("/ws?token=$token") {
            skipConnected()
            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))))
            val msg1 = receiveServerMessage() as ServerMessage.GameStateUpdated

            send(Frame.Text(sendMessage(ClientMessage.Build(BuildingType.GoldMine, 10, 10))))
            val msg2 = receiveServerMessage() as ServerMessage.GameStateUpdated

            assertEquals(2, msg2.state.village.buildings.count {
                it.type == BuildingType.LumberCamp || it.type == BuildingType.GoldMine
            })
        }

        val secondMsg = withTimeout(5000) {
            val first = client2Messages.receive()
            val second = client2Messages.receive()
            second
        }
        assertTrue(secondMsg is ServerMessage.GameStateUpdated)
        assertTrue(secondMsg.state.village.buildings.any { it.type == BuildingType.LumberCamp })
        assertTrue(secondMsg.state.village.buildings.any { it.type == BuildingType.GoldMine })

        client2Job.cancel()
        scope.cancel()
    }
}
