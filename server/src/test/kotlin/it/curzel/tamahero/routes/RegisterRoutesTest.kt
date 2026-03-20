package it.curzel.tamahero.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.curzel.tamahero.models.RegisterRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class RegisterRoutesTest {

    @Test
    fun testRegisterSuccess() = testApp { client ->
        val response = client.post("/api/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "password123"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["userId"])
    }

    @Test
    fun testRegisterDuplicate() = testApp { client ->
        client.post("/api/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "password123"))
        }
        val response = client.post("/api/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "otherpass123"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Username already taken", body["error"]?.jsonPrimitive?.content)
    }

    @Test
    fun testRegisterShortPassword() = testApp { client ->
        val response = client.post("/api/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "short"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testRegisterBlankUsername() = testApp { client ->
        val response = client.post("/api/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("", "password123"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
