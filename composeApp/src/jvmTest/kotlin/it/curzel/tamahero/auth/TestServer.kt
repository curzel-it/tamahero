package it.curzel.tamahero.auth

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import it.curzel.tamahero.testModule
import java.net.ServerSocket

object TestServer {

    private var server: EmbeddedServer<*, *>? = null
    var port: Int = 0
        private set

    val baseUrl: String get() = "http://localhost:$port"

    fun start() {
        port = ServerSocket(0).use { it.localPort }
        server = embeddedServer(Netty, port = port, host = "127.0.0.1") {
            testModule()
        }.start(wait = false)
        Thread.sleep(1000)
    }

    fun stop() {
        server?.stop()
        server = null
    }
}
