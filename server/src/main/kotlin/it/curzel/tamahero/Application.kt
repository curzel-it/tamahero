package it.curzel.tamahero

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import it.curzel.tamahero.auth.AuthService
import it.curzel.tamahero.auth.configureAuth
import it.curzel.tamahero.db.Database
import it.curzel.tamahero.routes.*
import it.curzel.tamahero.websocket.TimerMonitor
import it.curzel.tamahero.websocket.WebSocketHandler
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    Database.init()
    AuthService.cleanupExpiredTokens()
    configureModule()
    TimerMonitor.start()
}

fun Application.testModule() {
    Database.initInMemory()
    configureModule()
}

private fun Application.configureModule() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = 65536
        masking = false
    }
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
        adminRoutes()
        webSocket("/ws") {
            val token = call.request.queryParameters["token"]
            WebSocketHandler.handleConnection(this, token)
        }
        staticResources("/", "static") {
            default("index.html")
        }
    }
}
