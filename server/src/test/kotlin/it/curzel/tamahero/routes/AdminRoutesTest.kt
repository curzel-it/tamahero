package it.curzel.tamahero.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.curzel.tamahero.models.*
import kotlin.test.*

class AdminRoutesTest {

    private var usernameCounter = 0

    private suspend fun registerAdmin(client: io.ktor.client.HttpClient): Pair<String, Long> {
        val username = "admin_${++usernameCounter}"
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"testpass123","email":"$username@curzel.it"}""")
        }
        val body = response.bodyAsText()
        val authResponse = ProtocolJson.decodeFromString<AuthResponse>(body)
        assertNotNull(authResponse.token, "Admin registration failed: $body")
        return Pair(authResponse.token!!, authResponse.userId!!)
    }

    private suspend fun registerUser(client: io.ktor.client.HttpClient): Pair<String, Long> {
        val username = "user_${++usernameCounter}"
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"testpass123"}""")
        }
        val body = response.bodyAsText()
        val authResponse = ProtocolJson.decodeFromString<AuthResponse>(body)
        assertNotNull(authResponse.token, "Registration failed: $body")
        return Pair(authResponse.token!!, authResponse.userId!!)
    }

    @Test
    fun triggerEventRequiresAdmin() = testApp { client ->
        val (userToken, userId) = registerUser(client)
        val response = client.post("/api/admin/trigger-event") {
            header("Authorization", "Bearer $userToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$userId,"eventType":"ScoutParty"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun triggerEventNoTokenReturns403() = testApp { client ->
        val response = client.post("/api/admin/trigger-event") {
            contentType(ContentType.Application.Json)
            setBody("""{"userId":1,"eventType":"ScoutParty"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun triggerScoutParty() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, targetUserId) = registerUser(client)
        val response = client.post("/api/admin/trigger-event") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$targetUserId,"eventType":"ScoutParty"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = ProtocolJson.decodeFromString<AdminResponse>(response.bodyAsText())
        assertTrue(body.success)
    }

    @Test
    fun triggerQuake() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, targetUserId) = registerUser(client)
        val response = client.post("/api/admin/trigger-event") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$targetUserId,"eventType":"Quake"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = ProtocolJson.decodeFromString<AdminResponse>(response.bodyAsText())
        assertTrue(body.success)
    }

    @Test
    fun triggerIonStorm() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, targetUserId) = registerUser(client)
        val response = client.post("/api/admin/trigger-event") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$targetUserId,"eventType":"IonStorm"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = ProtocolJson.decodeFromString<AdminResponse>(response.bodyAsText())
        assertTrue(body.success)
    }

    @Test
    fun triggerBattle() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, targetUserId) = registerUser(client)
        val response = client.post("/api/admin/trigger-event") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$targetUserId,"eventType":"Battle"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(ProtocolJson.decodeFromString<AdminResponse>(response.bodyAsText()).success)
    }

    @Test
    fun triggerEventFailsWhenEventAlreadyActive() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, targetUserId) = registerUser(client)
        // Trigger first event
        client.post("/api/admin/trigger-event") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$targetUserId,"eventType":"ScoutParty"}""")
        }
        // Trigger second event — should fail
        val response = client.post("/api/admin/trigger-event") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$targetUserId,"eventType":"Quake"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = ProtocolJson.decodeFromString<AdminResponse>(response.bodyAsText())
        assertFalse(body.success)
        assertTrue(body.error?.contains("already active") == true)
    }

    @Test
    fun triggerInvalidEventType() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, targetUserId) = registerUser(client)
        val response = client.post("/api/admin/trigger-event") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$targetUserId,"eventType":"FakeEvent"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun grantResources() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, targetUserId) = registerUser(client)
        val response = client.post("/api/admin/grant-resources") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$targetUserId,"credits":5000,"metal":3000,"crystal":1000}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(ProtocolJson.decodeFromString<AdminResponse>(response.bodyAsText()).success)
    }

    @Test
    fun resetVillage() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, targetUserId) = registerUser(client)
        val response = client.post("/api/admin/reset-village") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$targetUserId}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(ProtocolJson.decodeFromString<AdminResponse>(response.bodyAsText()).success)
    }
}
