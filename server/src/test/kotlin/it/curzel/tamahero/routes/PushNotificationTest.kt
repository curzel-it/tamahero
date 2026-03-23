package it.curzel.tamahero.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.curzel.tamahero.models.*
import it.curzel.tamahero.notifications.MockPushNotificationService
import it.curzel.tamahero.notifications.PushNotificationServiceProvider
import kotlin.test.*

class PushNotificationTest {

    private var usernameCounter = 0

    private suspend fun registerUser(client: io.ktor.client.HttpClient, name: String? = null): Pair<String, Long> {
        val username = name ?: "user_push_${++usernameCounter}"
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"testpass123"}""")
        }
        val body = response.bodyAsText()
        val authResponse = ProtocolJson.decodeFromString<AuthResponse>(body)
        assertNotNull(authResponse.token)
        return Pair(authResponse.token!!, authResponse.userId!!)
    }

    @Test
    fun underAttackNotification() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, defenderId) = registerUser(client, "target_player")

        mock.notifyUnderAttack(defenderId, "raider42")

        val notifs = mock.sentWithType("under_attack")
        assertEquals(1, notifs.size)
        assertEquals(defenderId, notifs[0].userId)
        assertTrue(notifs[0].title.contains("under attack"))
        assertTrue(notifs[0].body.contains("raider42"))
    }

    @Test
    fun defenseResultNotification() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, defenderId) = registerUser(client, "defender")

        mock.notifyDefenseResult(
            defenderId = defenderId,
            attackerName = "attacker123",
            stars = 2,
            lootGold = 500,
            lootWood = 300,
            lootMetal = 100,
        )

        val notifs = mock.sentWithType("defense_result")
        assertEquals(1, notifs.size)
        assertEquals(defenderId, notifs[0].userId)
        assertTrue(notifs[0].title.contains("attacked"))
        assertTrue(notifs[0].body.contains("attacker123"))
        assertTrue(notifs[0].body.contains("500g"))
    }

    @Test
    fun eventStartedNotification() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, userId) = registerUser(client)

        mock.notifyEventStarted(userId, "Battle")

        val notifs = mock.sentWithType("event_started")
        assertEquals(1, notifs.size)
        assertTrue(notifs[0].body.contains("Battle"))
        assertTrue(notifs[0].body.contains("Defend"))
    }

    @Test
    fun eventEndedNotification() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, userId) = registerUser(client)

        mock.notifyEventEnded(userId, "Storm", true)

        val notifs = mock.sentWithType("event_ended")
        assertEquals(1, notifs.size)
        assertTrue(notifs[0].body.contains("Storm"))
        assertTrue(notifs[0].body.contains("defended successfully"))
    }

    @Test
    fun eventEndedFailureNotification() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, userId) = registerUser(client)

        mock.notifyEventEnded(userId, "Earthquake", false)

        val notifs = mock.sentWithType("event_ended")
        assertEquals(1, notifs.size)
        assertTrue(notifs[0].body.contains("suffered damage"))
    }

    @Test
    fun buildingCompleteNotification() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, userId) = registerUser(client)

        mock.notifyBuildingComplete(userId, "Cannon", 1)

        val notifs = mock.sentWithType("building_complete")
        assertEquals(1, notifs.size)
        assertTrue(notifs[0].title.contains("Construction Complete"))
        assertTrue(notifs[0].body.contains("Cannon"))
    }

    @Test
    fun trainingCompleteNotification() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, userId) = registerUser(client)

        mock.notifyTrainingComplete(userId, "ElfArcher", 2)

        val notifs = mock.sentWithType("training_complete")
        assertEquals(1, notifs.size)
        assertTrue(notifs[0].body.contains("ElfArcher"))
        assertTrue(notifs[0].body.contains("level 2"))
    }

    @Test
    fun noNotificationWhenUserIsConnected() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, userId) = registerUser(client)

        mock.setConnected(userId, true)

        mock.notifyUnderAttack(userId, "attacker")
        mock.notifyDefenseResult(userId, "attacker", 2, 500, 300, 100)
        mock.notifyEventStarted(userId, "Battle")
        mock.notifyEventEnded(userId, "Storm", true)
        mock.notifyBuildingComplete(userId, "Cannon", 1)
        mock.notifyTrainingComplete(userId, "Dragon", 3)

        assertEquals(0, mock.sentCount, "No push notifications should be sent to connected users")
    }

    @Test
    fun notificationSentWhenUserDisconnects() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, userId) = registerUser(client)

        mock.setConnected(userId, true)
        mock.notifyUnderAttack(userId, "attacker")
        assertEquals(0, mock.sentCount)

        mock.setConnected(userId, false)
        mock.notifyUnderAttack(userId, "attacker")
        assertEquals(1, mock.sentCount)
    }

    @Test
    fun mixedOnlineOfflineUsers() = testApp { client ->
        val mock = MockPushNotificationService()
        PushNotificationServiceProvider.instance = mock

        val (_, onlineUser) = registerUser(client, "online_player")
        val (_, offlineUser) = registerUser(client, "offline_player")

        mock.setConnected(onlineUser, true)

        mock.notifyUnderAttack(onlineUser, "raider")
        mock.notifyUnderAttack(offlineUser, "raider")

        assertEquals(0, mock.sentTo(onlineUser).size)
        assertEquals(1, mock.sentTo(offlineUser).size)
    }
}
