package it.curzel.tamahero.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.FirebaseMessagingException
import it.curzel.tamahero.db.DeviceTokenRepository
import it.curzel.tamahero.websocket.ConnectionManager
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

class FirebasePushNotificationService : PushNotificationService {

    private val logger = LoggerFactory.getLogger(FirebasePushNotificationService::class.java)
    private var initialized = false

    fun init() {
        val credentialsPath = System.getenv("FIREBASE_CREDENTIALS_PATH")
            ?: "firebase-service-account.json"
        val file = File(credentialsPath)

        if (!file.exists()) {
            logger.warn("Firebase credentials not found at '{}'. Push notifications disabled.", credentialsPath)
            return
        }

        try {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream(file)))
                .build()
            FirebaseApp.initializeApp(options)
            initialized = true
            logger.info("Firebase initialized. Push notifications enabled.")
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase", e)
        }
    }

    override fun isUserConnected(userId: Long): Boolean =
        ConnectionManager.getConnectedPlayerIds().contains(userId)

    override fun sendToUser(userId: Long, title: String, body: String, data: Map<String, String>) {
        if (!initialized) return

        val tokens = DeviceTokenRepository.getTokensForUser(userId)
        if (tokens.isEmpty()) return

        for (token in tokens) {
            try {
                val message = Message.builder()
                    .setToken(token)
                    .setNotification(
                        Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build()
                    )
                    .putAllData(data)
                    .build()

                FirebaseMessaging.getInstance().send(message)
            } catch (e: FirebaseMessagingException) {
                if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
                    e.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    logger.info("Removing invalid FCM token for user {}", userId)
                    DeviceTokenRepository.removeToken(token)
                } else {
                    logger.error("Failed to send push to user {} token {}", userId, token.take(10), e)
                }
            } catch (e: Exception) {
                logger.error("Failed to send push to user {}", userId, e)
            }
        }
    }
}
