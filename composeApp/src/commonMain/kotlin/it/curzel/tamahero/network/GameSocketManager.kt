package it.curzel.tamahero.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

object GameSocketManager {

    private var client: HttpClient? = null
    private var session: WebSocketSession? = null
    private var sendChannel: Channel<String>? = null
    private var connectionJob: Job? = null

    fun initialize(client: HttpClient) {
        this.client = client
    }

    fun connect(
        url: String,
        onMessage: (String) -> Unit,
        onError: (String) -> Unit,
        onClose: () -> Unit,
    ) {
        sendChannel = Channel(Channel.BUFFERED)

        connectionJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                client!!.webSocket(url) {
                    session = this

                    launch {
                        for (message in sendChannel!!) {
                            send(Frame.Text(message))
                        }
                    }

                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> onMessage(frame.readText())
                            is Frame.Close -> {
                                onClose()
                                break
                            }
                            else -> {}
                        }
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "WebSocket error")
            } finally {
                session = null
                onClose()
            }
        }
    }

    fun send(message: String) {
        sendChannel?.trySend(message)
    }

    fun close() {
        sendChannel?.close()
        connectionJob?.cancel()
        connectionJob = null
        session = null
        sendChannel = null
    }
}
