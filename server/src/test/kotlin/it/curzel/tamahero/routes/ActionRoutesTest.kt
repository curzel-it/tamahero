package it.curzel.tamahero.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.curzel.tamahero.db.ActionRepository
import it.curzel.tamahero.models.ActionType
import it.curzel.tamahero.models.CreateHeroRequest
import it.curzel.tamahero.models.RegisterRequest
import it.curzel.tamahero.models.StartActionRequest
import kotlinx.serialization.json.*
import kotlin.test.*

class ActionRoutesTest {

    private fun basicAuth(username: String, password: String): String {
        val encoded = java.util.Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        return "Basic $encoded"
    }

    private suspend fun registerAndCreateHero(client: io.ktor.client.HttpClient): Pair<String, Long> {
        client.post("/api/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "password123"))
        }
        val auth = basicAuth("testuser", "password123")
        val createResponse = client.post("/api/heroes") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(CreateHeroRequest("TestHero"))
        }
        val heroId = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.long
        return auth to heroId
    }

    @Test
    fun testStartAction() = testApp { client ->
        val (auth, heroId) = registerAndCreateHero(client)
        val response = client.post("/api/heroes/$heroId/action") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(StartActionRequest(ActionType.TRAIN_STRENGTH))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("TRAIN_STRENGTH", body["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun testStartActionDuplicate() = testApp { client ->
        val (auth, heroId) = registerAndCreateHero(client)
        client.post("/api/heroes/$heroId/action") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(StartActionRequest(ActionType.TRAIN_STRENGTH))
        }
        val response = client.post("/api/heroes/$heroId/action") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(StartActionRequest(ActionType.TRAIN_AGILITY))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testCheckActionInProgress() = testApp { client ->
        val (auth, heroId) = registerAndCreateHero(client)
        client.post("/api/heroes/$heroId/action") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(StartActionRequest(ActionType.TRAIN_STRENGTH))
        }
        val response = client.get("/api/heroes/$heroId/action") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["type"])
    }

    @Test
    fun testCheckActionIdle() = testApp { client ->
        val (auth, heroId) = registerAndCreateHero(client)
        val response = client.get("/api/heroes/$heroId/action") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun testCheckActionCompleted() = testApp { client ->
        val (auth, heroId) = registerAndCreateHero(client)
        val now = System.currentTimeMillis()
        ActionRepository.createAction(heroId, ActionType.TRAIN_STRENGTH, now - 600_000, now - 1)

        val response = client.get("/api/heroes/$heroId/action") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(11, body["stats"]?.jsonObject?.get("strength")?.jsonPrimitive?.int)
    }

    @Test
    fun testStartActionNoAuth() = testApp { client ->
        val response = client.post("/api/heroes/1/action") {
            contentType(ContentType.Application.Json)
            setBody(StartActionRequest(ActionType.TRAIN_STRENGTH))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
