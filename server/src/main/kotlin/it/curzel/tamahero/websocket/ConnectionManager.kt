package it.curzel.tamahero.websocket

import io.ktor.websocket.*
import it.curzel.tamahero.models.ProtocolJson
import it.curzel.tamahero.models.ServerMessage
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class PlayerConnection(
    val id: Long,
    val userId: Long,
    val session: WebSocketSession,
    var lastKeepAlive: Long = System.currentTimeMillis(),
)

object ConnectionManager {

    private val logger = LoggerFactory.getLogger(ConnectionManager::class.java)
    private val connections = ConcurrentHashMap<Long, MutableSet<PlayerConnection>>()
    private val monitoringJobs = ConcurrentHashMap<Long, Job>()
    private val nextConnectionId = AtomicLong(0)

    private const val KEEP_ALIVE_CHECK_INTERVAL_MS = 5_000L
    private const val KEEP_ALIVE_TIMEOUT_MS = 30_000L

    fun registerConnection(userId: Long, session: WebSocketSession): Long {
        val connectionId = nextConnectionId.incrementAndGet()
        val connection = PlayerConnection(connectionId, userId, session)
        connections.compute(userId) { _, existing ->
            (existing ?: ConcurrentHashMap.newKeySet()).also { it.add(connection) }
        }
        startKeepAliveMonitoringIfNeeded(userId)
        logger.info("Player {} connected (connection {})", userId, connectionId)
        return connectionId
    }

    fun removeConnection(userId: Long, connectionId: Long) {
        connections.computeIfPresent(userId) { _, conns ->
            conns.removeAll { it.id == connectionId }
            if (conns.isEmpty()) {
                monitoringJobs.remove(userId)?.cancel()
                logger.info("Player {} fully disconnected", userId)
                null
            } else {
                conns
            }
        }
    }

    fun removeAllConnections(userId: Long) {
        connections.remove(userId)
        monitoringJobs.remove(userId)?.cancel()
        logger.info("Player {} all connections removed", userId)
    }

    fun updateKeepAlive(userId: Long, connectionId: Long) {
        connections[userId]?.find { it.id == connectionId }?.let {
            it.lastKeepAlive = System.currentTimeMillis()
        }
    }

    fun getConnectedPlayerIds(): Set<Long> = connections.keys.toSet()

    suspend fun sendToPlayer(userId: Long, message: ServerMessage) {
        val conns = connections[userId] ?: return
        val text = ProtocolJson.encodeToString(ServerMessage.serializer(), message)
        val toRemove = mutableListOf<Long>()
        for (conn in conns.toList()) {
            try {
                conn.session.send(Frame.Text(text))
            } catch (e: Exception) {
                logger.error("Failed to send message to player {} (connection {})", userId, conn.id, e)
                toRemove.add(conn.id)
            }
        }
        for (id in toRemove) {
            removeConnection(userId, id)
        }
    }

    private fun startKeepAliveMonitoringIfNeeded(userId: Long) {
        if (monitoringJobs.containsKey(userId)) return
        val job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            while (isActive && connections.containsKey(userId)) {
                delay(KEEP_ALIVE_CHECK_INTERVAL_MS)
                val conns = connections[userId] ?: break
                val now = System.currentTimeMillis()
                val timedOut = conns.filter { now - it.lastKeepAlive > KEEP_ALIVE_TIMEOUT_MS }
                for (conn in timedOut) {
                    logger.info("Player {} connection {} keep-alive timeout", userId, conn.id)
                    try {
                        conn.session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Keep-alive timeout"))
                    } catch (_: Exception) {}
                    removeConnection(userId, conn.id)
                }
            }
        }
        monitoringJobs[userId] = job
    }
}
