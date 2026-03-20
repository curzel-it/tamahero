package it.curzel.tamahero.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.curzel.tamahero.models.CreateHeroRequest
import it.curzel.tamahero.models.RegisterRequest
import kotlinx.serialization.json.*
import kotlin.test.*

class HeroRoutesTest {

    private fun basicAuth(username: String, password: String): String {
        val encoded = java.util.Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        return "Basic $encoded"
    }

    private suspend fun registerUser(client: io.ktor.client.HttpClient, username: String = "testuser", password: String = "password123") {
        client.post("/api/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username, password))
        }
    }

    @Test
    fun testCreateHero() = testApp { client ->
        registerUser(client)
        val auth = basicAuth("testuser", "password123")
        val response = client.post("/api/heroes") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(CreateHeroRequest("Aragorn"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Aragorn", body["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun testCreateHeroNoAuth() = testApp { client ->
        val response = client.post("/api/heroes") {
            contentType(ContentType.Application.Json)
            setBody(CreateHeroRequest("Aragorn"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testListHeroes() = testApp { client ->
        registerUser(client)
        val auth = basicAuth("testuser", "password123")
        client.post("/api/heroes") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(CreateHeroRequest("Hero1"))
        }
        client.post("/api/heroes") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(CreateHeroRequest("Hero2"))
        }
        val response = client.get("/api/heroes") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, body.size)
    }

    @Test
    fun testGetHeroById() = testApp { client ->
        registerUser(client)
        val auth = basicAuth("testuser", "password123")
        val createResponse = client.post("/api/heroes") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody(CreateHeroRequest("Gandalf"))
        }
        val heroId = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.long
        val response = client.get("/api/heroes/$heroId") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Gandalf", body["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun testGetHeroNotFound() = testApp { client ->
        registerUser(client)
        val auth = basicAuth("testuser", "password123")
        val response = client.get("/api/heroes/999") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testGetHeroWrongAuth() = testApp { client ->
        registerUser(client)
        val auth = basicAuth("testuser", "wrongpassword")
        val response = client.get("/api/heroes") {
            header(HttpHeaders.Authorization, auth)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
