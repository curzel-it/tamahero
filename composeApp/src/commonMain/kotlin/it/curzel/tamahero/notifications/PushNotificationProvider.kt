package it.curzel.tamahero.notifications

interface PushNotificationHandler {
    fun requestPermissionAndRegister()
    fun unregister()
}

object PushNotificationProvider {
    private var _instance: PushNotificationHandler? = null

    val instance: PushNotificationHandler
        get() = _instance ?: throw IllegalStateException("PushNotificationProvider not initialized")

    fun setProvider(handler: PushNotificationHandler) {
        _instance = handler
    }

    fun isInitialized(): Boolean = _instance != null
}
