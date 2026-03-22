package it.curzel.tamahero.auth

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import it.curzel.tamahero.models.ClientMessage
import it.curzel.tamahero.models.ProtocolJson
import it.curzel.tamahero.models.ServerMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.AfterClass
import org.junit.BeforeClass
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

@kotlinx.serialization.Serializable
private data class RenewResponse(val success: Boolean, val token: String? = null)

class AuthFlowTest {

    companion object {
        private val counter = AtomicInteger(0)

        @JvmStatic @BeforeClass
        fun setup() { TestServer.start() }

        @JvmStatic @AfterClass
        fun teardown() { TestServer.stop() }
    }

    private fun uniqueName() = "testuser_${counter.incrementAndGet()}"

    @Test
    fun registerTransitionsToLoggedIn() = runBlocking {
        val client = AuthClient(baseUrl = TestServer.baseUrl)
        val username = uniqueName()
        client.register(username, "password123")

        val state = client.state.value
        assertTrue(state is AuthState.LoggedIn, "Expected LoggedIn but got $state")
        assertEquals(username, state.username)
        assertFalse(state.token.isBlank())
    }

    @Test
    fun loginAfterRegisterSucceeds() = runBlocking {
        val username = uniqueName()

        val client1 = AuthClient(baseUrl = TestServer.baseUrl)
        client1.register(username, "password123")
        assertTrue(client1.state.value is AuthState.LoggedIn)

        val client2 = AuthClient(baseUrl = TestServer.baseUrl)
        client2.login(username, "password123")

        val state = client2.state.value
        assertTrue(state is AuthState.LoggedIn, "Expected LoggedIn but got $state")
        assertEquals(username, state.username)
    }

    @Test
    fun loginWithWrongPasswordFails() = runBlocking {
        val username = uniqueName()

        val client1 = AuthClient(baseUrl = TestServer.baseUrl)
        client1.register(username, "correctpass")

        val client2 = AuthClient(baseUrl = TestServer.baseUrl)
        client2.login(username, "wrongpass")

        val state = client2.state.value
        assertTrue(state is AuthState.Error, "Expected Error but got $state")
    }

    @Test
    fun duplicateRegistrationFails() = runBlocking {
        val username = uniqueName()

        val client1 = AuthClient(baseUrl = TestServer.baseUrl)
        client1.register(username, "password123")
        assertTrue(client1.state.value is AuthState.LoggedIn)

        val client2 = AuthClient(baseUrl = TestServer.baseUrl)
        client2.register(username, "password456")

        val state = client2.state.value
        assertTrue(state is AuthState.Error, "Expected Error but got $state")
    }

    @Test
    fun logoutTransitionsToLoggedOut() = runBlocking {
        val client = AuthClient(baseUrl = TestServer.baseUrl)
        client.register(uniqueName(), "password123")
        assertTrue(client.state.value is AuthState.LoggedIn)

        client.logout()
        assertTrue(client.state.value is AuthState.LoggedOut)
    }

    @Test
    fun validateTokenSucceeds() = runBlocking {
        val authClient = AuthClient(baseUrl = TestServer.baseUrl)
        authClient.register(uniqueName(), "password123")
        val token = (authClient.state.value as AuthState.LoggedIn).token

        val httpClient = HttpClient()
        val response = httpClient.get("${TestServer.baseUrl}/api/auth/validate") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        httpClient.close()
    }

    @Test
    fun validateInvalidTokenFails() = runBlocking {
        val httpClient = HttpClient()
        val response = httpClient.get("${TestServer.baseUrl}/api/auth/validate") {
            header("Authorization", "Bearer invalid_token_12345")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        httpClient.close()
    }

    @Test
    fun renewTokenReturnsNewToken() = runBlocking {
        val authClient = AuthClient(baseUrl = TestServer.baseUrl)
        authClient.register(uniqueName(), "password123")
        val oldToken = (authClient.state.value as AuthState.LoggedIn).token

        val httpClient = HttpClient()
        val response = httpClient.post("${TestServer.baseUrl}/api/auth/renew") {
            header("Authorization", "Bearer $oldToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json { ignoreUnknownKeys = true }
        val body = json.decodeFromString<RenewResponse>(response.bodyAsText())
        assertTrue(body.success)
        assertNotNull(body.token)
        assertNotEquals(oldToken, body.token)

        // Old token should no longer work
        val validateOld = httpClient.get("${TestServer.baseUrl}/api/auth/validate") {
            header("Authorization", "Bearer $oldToken")
        }
        assertEquals(HttpStatusCode.Unauthorized, validateOld.status)

        // New token should work
        val validateNew = httpClient.get("${TestServer.baseUrl}/api/auth/validate") {
            header("Authorization", "Bearer ${body.token}")
        }
        assertEquals(HttpStatusCode.OK, validateNew.status)
        httpClient.close()
    }

    @Test
    fun tokenReceivedAfterLoginCanConnectWebSocket() = runBlocking {
        val authClient = AuthClient(baseUrl = TestServer.baseUrl)
        authClient.register(uniqueName(), "password123")
        val token = (authClient.state.value as AuthState.LoggedIn).token

        val wsClient = HttpClient { install(WebSockets) }
        val wsUrl = TestServer.baseUrl.replace("http://", "ws://") + "/ws?token=$token"

        wsClient.webSocket(wsUrl) {
            val connectedFrame = incoming.receive() as Frame.Text
            val connected = ProtocolJson.decodeFromString(ServerMessage.serializer(), connectedFrame.readText())
            assertTrue(connected is ServerMessage.Connected)

            send(Frame.Text(ProtocolJson.encodeToString(ClientMessage.serializer(), ClientMessage.GetVillage)))
            val villageFrame = incoming.receive() as Frame.Text
            val village = ProtocolJson.decodeFromString(ServerMessage.serializer(), villageFrame.readText())
            assertTrue(village is ServerMessage.GameStateUpdated)
            assertTrue(village.state.village.buildings.isNotEmpty())
        }
        wsClient.close()
    }
}
