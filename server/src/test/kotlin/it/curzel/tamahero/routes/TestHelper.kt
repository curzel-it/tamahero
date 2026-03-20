package it.curzel.tamahero.routes

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import it.curzel.tamahero.testModule

fun testApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
    application {
        testModule()
    }
    val jsonClient = createClient {
        install(ContentNegotiation) {
            json()
        }
    }
    block(jsonClient)
}
