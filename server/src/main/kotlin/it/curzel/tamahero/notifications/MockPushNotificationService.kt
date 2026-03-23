package it.curzel.tamahero.notifications

data class SentNotification(
    val userId: Long,
    val title: String,
    val body: String,
    val data: Map<String, String>,
)

class MockPushNotificationService : PushNotificationService {

    private val _sent = mutableListOf<SentNotification>()
    private val _connectedUsers = mutableSetOf<Long>()

    val sent: List<SentNotification> get() = _sent.toList()

    val sentCount: Int get() = _sent.size

    override fun isUserConnected(userId: Long): Boolean = _connectedUsers.contains(userId)

    override fun sendToUser(userId: Long, title: String, body: String, data: Map<String, String>) {
        _sent.add(SentNotification(userId, title, body, data))
    }

    fun setConnected(userId: Long, connected: Boolean) {
        if (connected) _connectedUsers.add(userId) else _connectedUsers.remove(userId)
    }

    fun clear() {
        _sent.clear()
    }

    fun sentTo(userId: Long): List<SentNotification> = _sent.filter { it.userId == userId }

    fun sentWithType(type: String): List<SentNotification> = _sent.filter { it.data["type"] == type }
}
