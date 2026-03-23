package it.curzel.tamahero.routes

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.curzel.tamahero.models.*
import kotlin.test.*

class BattleSimulationTest {

    private var usernameCounter = 0

    private suspend fun registerAdmin(client: io.ktor.client.HttpClient): Pair<String, Long> {
        val username = "admin_${++usernameCounter}"
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"testpass123","email":"$username@curzel.it"}""")
        }
        val body = response.bodyAsText()
        val authResponse = ProtocolJson.decodeFromString<AuthResponse>(body)
        assertNotNull(authResponse.token, "Admin registration failed: $body")
        return Pair(authResponse.token!!, authResponse.userId!!)
    }

    private suspend fun registerUser(client: io.ktor.client.HttpClient): Pair<String, Long> {
        val username = "user_${++usernameCounter}"
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"testpass123"}""")
        }
        val body = response.bodyAsText()
        val authResponse = ProtocolJson.decodeFromString<AuthResponse>(body)
        assertNotNull(authResponse.token, "Registration failed: $body")
        return Pair(authResponse.token!!, authResponse.userId!!)
    }

    private suspend fun getVillage(client: io.ktor.client.HttpClient, adminToken: String, userId: Long): GameState {
        val response = client.post("/api/admin/get-village") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$userId}""")
        }
        assertEquals(HttpStatusCode.OK, response.status, "get-village failed: ${response.bodyAsText()}")
        return ProtocolJson.decodeFromString<GameState>(response.bodyAsText())
    }

    private suspend fun placeBuilding(
        client: io.ktor.client.HttpClient,
        adminToken: String,
        userId: Long,
        type: String,
        x: Int,
        y: Int,
    ) {
        val response = client.post("/api/admin/place-building") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$userId,"type":"$type","x":$x,"y":$y}""")
        }
        assertEquals(HttpStatusCode.OK, response.status, "place-building failed: ${response.bodyAsText()}")
    }

    private suspend fun triggerEvent(
        client: io.ktor.client.HttpClient,
        adminToken: String,
        userId: Long,
        eventType: String,
        troops: List<TriggerEventTroop>,
    ) {
        val troopsJson = troops.joinToString(",") { """{"type":"${it.type}","count":${it.count},"level":${it.level}}""" }
        val response = client.post("/api/admin/trigger-event") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$userId,"eventType":"$eventType","troops":[$troopsJson]}""")
        }
        assertEquals(HttpStatusCode.OK, response.status, "trigger-event failed: ${response.bodyAsText()}")
    }

    private suspend fun advanceTime(
        client: io.ktor.client.HttpClient,
        adminToken: String,
        userId: Long,
        deltaMs: Long,
    ) {
        val response = client.post("/api/admin/advance-time") {
            header("Authorization", "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody("""{"userId":$userId,"deltaMs":$deltaMs}""")
        }
        assertEquals(HttpStatusCode.OK, response.status, "advance-time failed: ${response.bodyAsText()}")
    }

    @Test
    fun singleMarineVsSingleRailGun() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, userId) = registerUser(client)

        placeBuilding(client, adminToken, userId, "RailGun", x = 16, y = 18)

        val before = getVillage(client, adminToken, userId)
        val railGun = before.village.buildings.find { it.type == BuildingType.RailGun }
        assertNotNull(railGun, "RailGun should exist")
        assertEquals(300, railGun.hp, "RailGun should have full HP")

        triggerEvent(client, adminToken, userId, "ScoutParty", listOf(
            TriggerEventTroop(type = "Marine", count = 1, level = 1),
        ))

        val duringBattle = getVillage(client, adminToken, userId)
        assertNotNull(duringBattle.activeEvent, "Event should be active")
        assertEquals(EventType.ScoutParty, duringBattle.activeEvent!!.type)

        advanceTime(client, adminToken, userId, 60_000)

        val after = getVillage(client, adminToken, userId)
        assertEquals(0, after.troops.size, "Marine should be dead")
        assertTrue(after.activeEvent?.completed == true, "Event should be completed")

        val railGunAfter = after.village.buildings.find { it.type == BuildingType.RailGun }
        assertNotNull(railGunAfter, "RailGun should survive")
        assertTrue(railGunAfter.hp > 0, "RailGun should have HP remaining")

        val commandCenter = after.village.buildings.find { it.type == BuildingType.CommandCenter }
        assertNotNull(commandCenter, "CommandCenter should survive")
        assertTrue(commandCenter.hp > 0, "CommandCenter should have HP remaining")
    }

    @Test
    fun marineNavigatesAroundBuildings() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, userId) = registerUser(client)

        placeBuilding(client, adminToken, userId, "RailGun", x = 16, y = 18)
        placeBuilding(client, adminToken, userId, "RailGun", x = 22, y = 18)

        triggerEvent(client, adminToken, userId, "ScoutParty", listOf(
            TriggerEventTroop(type = "Marine", count = 1, level = 1),
        ))

        advanceTime(client, adminToken, userId, 120_000)

        val after = getVillage(client, adminToken, userId)
        assertEquals(0, after.troops.size, "Marine should be dead")
        assertTrue(after.activeEvent?.completed == true, "Event should be completed")
    }

    @Test
    fun multipleMarinesOverwhelmRailGun() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, userId) = registerUser(client)

        placeBuilding(client, adminToken, userId, "RailGun", x = 16, y = 18)

        triggerEvent(client, adminToken, userId, "ScoutParty", listOf(
            TriggerEventTroop(type = "Marine", count = 10, level = 1),
        ))

        advanceTime(client, adminToken, userId, 120_000)

        val after = getVillage(client, adminToken, userId)
        val troopInfo = after.troops.map { "id=${it.id} pos=(${it.x},${it.y}) hp=${it.hp} target=${it.targetId} pathLen=${it.path.size}" }
        val buildingInfo = after.village.buildings.map { "${it.type}(id=${it.id},hp=${it.hp})" }
        assertTrue(after.activeEvent?.completed == true,
            "Event should be completed. troops=$troopInfo buildings=$buildingInfo event=${after.activeEvent}")
    }
}
