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
    fun singleSoldierVsSingleCannon() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, userId) = registerUser(client)

        // Place a cannon near the center (next to TownHall at 18,18 which is 4x4)
        placeBuilding(client, adminToken, userId, "Cannon", x = 16, y = 18)

        // Verify setup
        val before = getVillage(client, adminToken, userId)
        val cannon = before.village.buildings.find { it.type == BuildingType.Cannon }
        assertNotNull(cannon, "Cannon should exist")
        assertEquals(300, cannon.hp, "Cannon should have full HP")

        // Trigger a battle event with a single HumanSoldier
        triggerEvent(client, adminToken, userId, "ScoutParty", listOf(
            TriggerEventTroop(type = "HumanSoldier", count = 1, level = 1),
        ))

        // Verify troops were spawned
        val duringBattle = getVillage(client, adminToken, userId)
        assertNotNull(duringBattle.activeEvent, "Event should be active")
        assertEquals(EventType.ScoutParty, duringBattle.activeEvent!!.type)

        // Advance time by 60 seconds — enough for soldier to reach cannon and die
        // Soldier speed=1.0, distance ~16-20 tiles, cannon dps=10, soldier hp=50
        advanceTime(client, adminToken, userId, 60_000)

        // Check results
        val after = getVillage(client, adminToken, userId)
        assertEquals(0, after.troops.size, "Soldier should be dead")
        assertTrue(after.activeEvent?.completed == true, "Event should be completed")

        val cannonAfter = after.village.buildings.find { it.type == BuildingType.Cannon }
        assertNotNull(cannonAfter, "Cannon should survive")
        assertTrue(cannonAfter.hp > 0, "Cannon should have HP remaining")

        val townHall = after.village.buildings.find { it.type == BuildingType.TownHall }
        assertNotNull(townHall, "TownHall should survive")
        assertTrue(townHall.hp > 0, "TownHall should have HP remaining")
    }

    @Test
    fun soldierNavigatesAroundBuildings() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, userId) = registerUser(client)

        // Default village: TownHall at (18,18) 4x4
        // Place two cannons flanking the TownHall — soldiers must path around buildings
        placeBuilding(client, adminToken, userId, "Cannon", x = 16, y = 18)
        placeBuilding(client, adminToken, userId, "Cannon", x = 22, y = 18)

        triggerEvent(client, adminToken, userId, "ScoutParty", listOf(
            TriggerEventTroop(type = "HumanSoldier", count = 1, level = 1),
        ))

        // Advance enough time for battle to conclude
        advanceTime(client, adminToken, userId, 120_000)

        val after = getVillage(client, adminToken, userId)
        assertEquals(0, after.troops.size, "Soldier should be dead")
        assertTrue(after.activeEvent?.completed == true, "Event should be completed")
    }

    @Test
    fun multipleSoldiersOverwhelmCannon() = testApp { client ->
        val (adminToken, _) = registerAdmin(client)
        val (_, userId) = registerUser(client)

        // Just cannon + town hall
        placeBuilding(client, adminToken, userId, "Cannon", x = 16, y = 18)

        // Send 10 soldiers — should overwhelm defenses
        triggerEvent(client, adminToken, userId, "ScoutParty", listOf(
            TriggerEventTroop(type = "HumanSoldier", count = 10, level = 1),
        ))

        advanceTime(client, adminToken, userId, 120_000)

        val after = getVillage(client, adminToken, userId)
        val troopInfo = after.troops.map { "id=${it.id} pos=(${it.x},${it.y}) hp=${it.hp} target=${it.targetId} pathLen=${it.path.size}" }
        val buildingInfo = after.village.buildings.map { "${it.type}(id=${it.id},hp=${it.hp})" }
        assertTrue(after.activeEvent?.completed == true,
            "Event should be completed. troops=$troopInfo buildings=$buildingInfo event=${after.activeEvent}")
    }
}
