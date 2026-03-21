package it.curzel.tamahero

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.curzel.tamahero.auth.AuthService
import it.curzel.tamahero.auth.configureAuth
import it.curzel.tamahero.db.Database
import it.curzel.tamahero.routes.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    Database.init()
    AuthService.cleanupExpiredTokens()
    configureModule()
}

fun Application.testModule() {
    Database.initInMemory()
    configureModule()
}

private fun Application.configureModule() {
    configureSerialization()
    configureStatusPages()
    configureAuth()
    routing {
        get("/health") {
            call.respondText("OK", ContentType.Text.Plain)
        }
        get("/version") {
            val buildTime = object {}.javaClass.getResource("/version.txt")?.readText()?.trim() ?: "unknown"
            val json = buildJsonObject {
                put("buildTime", buildTime)
                put("status", "running")
            }
            call.respondText(json.toString(), ContentType.Application.Json)
        }
        authRoutes()
        staticResources("/", "static") {
            default("index.html")
        }
    }
}
