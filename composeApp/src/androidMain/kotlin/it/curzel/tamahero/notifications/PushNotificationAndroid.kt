package it.curzel.tamahero.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.curzel.tamahero.network.GameSocketClient

class PushNotificationAndroid : PushNotificationHandler {

    override fun requestPermissionAndRegister() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            GameSocketClient.registerDevice(token, "android")
        }
    }

    override fun unregister() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            GameSocketClient.unregisterDevice(token)
        }
    }
}

class TamaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        GameSocketClient.registerDevice(token, "android")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "TamaHero"
        val body = message.notification?.body ?: message.data["body"] ?: return

        val channelId = "tamahero_notifications"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Game Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
