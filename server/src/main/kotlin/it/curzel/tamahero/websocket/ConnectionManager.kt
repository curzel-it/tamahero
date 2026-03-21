package it.curzel.tamahero.websocket

import io.ktor.websocket.*
import it.curzel.tamahero.models.ProtocolJson
import it.curzel.tamahero.models.ServerMessage
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

data class PlayerConnection(
    val userId: Long,
    val session: WebSocketSession,
    var lastKeepAlive: Long = System.currentTimeMillis(),
)

object ConnectionManager {

    private val logger = LoggerFactory.getLogger(ConnectionManager::class.java)
    private val connections = ConcurrentHashMap<Long, PlayerConnection>()
    private val monitoringJobs = ConcurrentHashMap<Long, Job>()

    private const val KEEP_ALIVE_CHECK_INTERVAL_MS = 5_000L
    private const val KEEP_ALIVE_TIMEOUT_MS = 30_000L

    fun registerConnection(userId: Long, session: WebSocketSession) {
        val existing = connections[userId]
        if (existing != null) {
            monitoringJobs.remove(userId)?.cancel()
        }
        connections[userId] = PlayerConnection(userId, session)
        startKeepAliveMonitoring(userId)
        logger.info("Player {} connected", userId)
    }

    fun removeConnection(userId: Long) {
        connections.remove(userId)
        monitoringJobs.remove(userId)?.cancel()
        logger.info("Player {} disconnected", userId)
    }

    fun updateKeepAlive(userId: Long) {
        connections[userId]?.lastKeepAlive = System.currentTimeMillis()
    }

    fun getConnectedPlayerIds(): Set<Long> = connections.keys.toSet()

    suspend fun sendToPlayer(userId: Long, message: ServerMessage) {
        val conn = connections[userId] ?: return
        try {
            val text = ProtocolJson.encodeToString(ServerMessage.serializer(), message)
            conn.session.send(Frame.Text(text))
        } catch (e: Exception) {
            logger.error("Failed to send message to player {}", userId, e)
            removeConnection(userId)
        }
    }

    private fun startKeepAliveMonitoring(userId: Long) {
        val job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            while (isActive && connections.containsKey(userId)) {
                delay(KEEP_ALIVE_CHECK_INTERVAL_MS)
                val conn = connections[userId] ?: break
                val elapsed = System.currentTimeMillis() - conn.lastKeepAlive
                if (elapsed > KEEP_ALIVE_TIMEOUT_MS) {
                    logger.info("Player {} keep-alive timeout", userId)
                    try {
                        conn.session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Keep-alive timeout"))
                    } catch (_: Exception) {}
                    removeConnection(userId)
                    break
                }
            }
        }
        monitoringJobs[userId] = job
    }
}
