package it.curzel.tamahero.notifications

interface PushNotificationService {
    fun sendToUser(userId: Long, title: String, body: String, data: Map<String, String> = emptyMap())

    fun isUserConnected(userId: Long): Boolean = false

    fun sendIfOffline(userId: Long, title: String, body: String, data: Map<String, String> = emptyMap()) {
        if (!isUserConnected(userId)) {
            sendToUser(userId, title, body, data)
        }
    }

    fun notifyUnderAttack(defenderId: Long, attackerName: String) {
        sendIfOffline(
            defenderId,
            "You are under attack!",
            "$attackerName is attacking your base!",
            mapOf("type" to "under_attack"),
        )
    }

    fun notifyDefenseResult(
        defenderId: Long,
        attackerName: String,
        stars: Int,
        lootCredits: Long,
        lootAlloy: Long,
        lootCrystal: Long,
    ) {
        val starText = "\u2B50".repeat(stars)
        sendIfOffline(
            defenderId,
            "Your base was attacked!",
            "$attackerName attacked you $starText — lost ${lootCredits}cr ${lootAlloy}al ${lootCrystal}xy",
            mapOf("type" to "defense_result"),
        )
    }

    fun notifyBuildingComplete(userId: Long, buildingType: String, level: Int) {
        sendIfOffline(
            userId,
            "Construction Complete",
            "$buildingType level $level is ready!",
            mapOf("type" to "building_complete"),
        )
    }

    fun notifyTrainingComplete(userId: Long, troopType: String, level: Int) {
        sendIfOffline(
            userId,
            "Training Complete",
            "$troopType level $level is ready for battle!",
            mapOf("type" to "training_complete"),
        )
    }

    fun notifyEventStarted(userId: Long, eventType: String) {
        sendIfOffline(
            userId,
            "Event Started",
            "A $eventType event has begun! Defend your base!",
            mapOf("type" to "event_started"),
        )
    }

    fun notifyEventEnded(userId: Long, eventType: String, success: Boolean) {
        val outcome = if (success) "defended successfully" else "suffered damage"
        sendIfOffline(
            userId,
            "Event Over",
            "$eventType ended — you $outcome!",
            mapOf("type" to "event_ended"),
        )
    }
}

object PushNotificationServiceProvider {
    var instance: PushNotificationService = NoOpPushNotificationService()
}

private class NoOpPushNotificationService : PushNotificationService {
    override fun sendToUser(userId: Long, title: String, body: String, data: Map<String, String>) {}
}
