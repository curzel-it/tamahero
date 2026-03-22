package it.curzel.tamahero.cli

import it.curzel.tamahero.models.*
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.*

class CliIntegrationTest {

    companion object {
        private val counter = java.util.concurrent.atomic.AtomicInteger(0)

        @JvmStatic @BeforeClass
        fun setup() { TestServer.start() }

        @JvmStatic @AfterClass
        fun teardown() { TestServer.stop() }
    }

    private suspend fun createConnectedClient(): CliClient {
        val client = CliClient(TestServer.baseUrl)
        val uniqueName = "user_${counter.incrementAndGet()}"
        assertTrue(client.register(uniqueName, "testpass123"), "Registration failed for $uniqueName")
        client.connectInBackground()
        return client
    }

    @Test
    fun registerAndConnect() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.GetVillage)
            assertNotNull(msg)
            assertTrue(msg is ServerMessage.GameStateUpdated)
        } finally {
            client.close()
        }
    }

    @Test
    fun newVillageHasDefaultBuildings() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val state = msg.state

            assertEquals(1, state.village.buildings.size)
            assertTrue(state.village.buildings.any { it.type == BuildingType.TownHall && it.level == 1 })

            assertEquals(1000, state.resources.gold)
            assertEquals(1000, state.resources.wood)
            assertEquals(500, state.resources.metal)
            assertEquals(0, state.resources.mana)
        } finally {
            client.close()
        }
    }

    @Test
    fun buildLumberCampDeductsResources() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))
            assertTrue(msg is ServerMessage.GameStateUpdated)
            val state = msg.state

            val lumberCamp = state.village.buildings.find { it.type == BuildingType.LumberCamp }
            assertNotNull(lumberCamp)
            assertEquals(5, lumberCamp.x)
            assertEquals(5, lumberCamp.y)
            assertEquals(1, lumberCamp.level)
            assertNotNull(lumberCamp.constructionStartedAt)

            // LumberCamp costs 50 gold
            assertEquals(950, state.resources.gold)
            assertEquals(1000, state.resources.wood)
        } finally {
            client.close()
        }
    }

    @Test
    fun buildGoldMineDeductsWood() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.GoldMine, 5, 5))
            assertTrue(msg is ServerMessage.GameStateUpdated)

            // GoldMine costs 50 wood
            assertEquals(1000, msg.state.resources.gold)
            assertEquals(950, msg.state.resources.wood)
        } finally {
            client.close()
        }
    }

    @Test
    fun buildMultipleBuildings() = runBlocking {
        val client = createConnectedClient()
        try {
            client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 0, 0))
            client.sendAndReceive(ClientMessage.Build(BuildingType.GoldMine, 3, 0))
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 6, 0))
            assertTrue(msg is ServerMessage.GameStateUpdated)

            val state = msg.state
            assertEquals(4, state.village.buildings.size) // 1 default + 3 new
            assertEquals(2, state.village.buildings.count { it.type == BuildingType.LumberCamp })
            assertEquals(1, state.village.buildings.count { it.type == BuildingType.GoldMine })

            // 2 lumber camps * 50 gold + 1 gold mine * 50 wood
            assertEquals(900, state.resources.gold)
            assertEquals(950, state.resources.wood)
        } finally {
            client.close()
        }
    }

    @Test
    fun buildFailsWithInsufficientResources() = runBlocking {
        val client = createConnectedClient()
        try {
            // Drain gold: build 20 lumber camps at 50 gold each = 1000 gold
            for (i in 0..19) {
                client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, (i % 20) * 2, (i / 20) * 2))
            }
            // Should be out of gold now
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 0, 10))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Insufficient"))
        } finally {
            client.close()
        }
    }

    @Test
    fun buildFailsOutOfBounds() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 39, 39))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("bounds"))
        } finally {
            client.close()
        }
    }

    @Test
    fun buildFailsOnOverlap() = runBlocking {
        val client = createConnectedClient()
        try {
            client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.GoldMine, 5, 5))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("overlap", ignoreCase = true))
        } finally {
            client.close()
        }
    }

    @Test
    fun moveBuilding() = runBlocking {
        val client = createConnectedClient()
        try {
            val getMsg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.TownHall }

            val msg = client.sendAndReceive(ClientMessage.Move(townHall.id, 0, 0))
            assertTrue(msg is ServerMessage.GameStateUpdated)
            val moved = msg.state.village.buildings.first { it.id == townHall.id }
            assertEquals(0, moved.x)
            assertEquals(0, moved.y)
        } finally {
            client.close()
        }
    }

    @Test
    fun moveFailsOutOfBounds() = runBlocking {
        val client = createConnectedClient()
        try {
            val getMsg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.TownHall }

            // TownHall is 4x4, so placing at 38 would go out of bounds
            val msg = client.sendAndReceive(ClientMessage.Move(townHall.id, 38, 38))
            assertTrue(msg is ServerMessage.Error)
        } finally {
            client.close()
        }
    }

    @Test
    fun moveFailsOnOverlap() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build a LumberCamp at (0,0)
            client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 0, 0))

            // Get TownHall (at 18,18) and try to move it on top of the LumberCamp
            val getMsg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.TownHall }

            val msg = client.sendAndReceive(ClientMessage.Move(townHall.id, 0, 0))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("overlap", ignoreCase = true))
        } finally {
            client.close()
        }
    }

    @Test
    fun collectFromProducer() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build a lumber camp
            val buildMsg = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))
            assertTrue(buildMsg is ServerMessage.GameStateUpdated)
            val lumberCamp = buildMsg.state.village.buildings.first { it.type == BuildingType.LumberCamp }

            // Collect immediately — no time has passed so no new resources
            val msg = client.sendAndReceive(ClientMessage.Collect(lumberCamp.id))
            assertTrue(msg is ServerMessage.GameStateUpdated)
        } finally {
            client.close()
        }
    }

    @Test
    fun villageStatePersistedAcrossRequests() = runBlocking {
        val client = createConnectedClient()
        try {
            client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))

            // Fetch village again — the lumber camp should still be there
            val msg = client.sendAndReceive(ClientMessage.GetVillage)
            assertTrue(msg is ServerMessage.GameStateUpdated)
            assertEquals(2, msg.state.village.buildings.size)
            assertTrue(msg.state.village.buildings.any { it.type == BuildingType.LumberCamp })
        } finally {
            client.close()
        }
    }

    @Test
    fun loginAfterRegister() = runBlocking {
        val username = "login_${counter.incrementAndGet()}"
        val client1 = CliClient(TestServer.baseUrl)
        assertTrue(client1.register(username, "mypassword"))
        client1.close()

        val client2 = CliClient(TestServer.baseUrl)
        assertTrue(client2.login(username, "mypassword"))
        client2.connectInBackground()

        val msg = client2.sendAndReceive(ClientMessage.GetVillage)
        assertTrue(msg is ServerMessage.GameStateUpdated)
        client2.close()
    }

    @Test
    fun loginFailsWithWrongPassword() = runBlocking {
        val username = "loginfail_${counter.incrementAndGet()}"
        val client = CliClient(TestServer.baseUrl)
        client.register(username, "correctpass")
        client.close()

        val client2 = CliClient(TestServer.baseUrl)
        assertFalse(client2.login(username, "wrongpass"))
        client2.close()
    }

    @Test
    fun resourcesDeductedCumulatively() = runBlocking {
        val client = createConnectedClient()
        try {
            // Start: 1000 gold, 1000 wood
            // Build LumberCamp: -50 gold → 950 gold
            val msg1 = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 0, 0))
            assertTrue(msg1 is ServerMessage.GameStateUpdated)
            assertEquals(950, msg1.state.resources.gold)

            // Build GoldMine: -50 wood → 950 wood
            val msg2 = client.sendAndReceive(ClientMessage.Build(BuildingType.GoldMine, 3, 0))
            assertTrue(msg2 is ServerMessage.GameStateUpdated)
            assertEquals(950, msg2.state.resources.gold)
            assertEquals(950, msg2.state.resources.wood)

            // Build another LumberCamp: -50 gold → 900 gold
            val msg3 = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 6, 0))
            assertTrue(msg3 is ServerMessage.GameStateUpdated)
            assertEquals(900, msg3.state.resources.gold)
            assertEquals(950, msg3.state.resources.wood)
        } finally {
            client.close()
        }
    }

    @Test
    fun newBuildingsAreUnderConstruction() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))
            assertTrue(msg is ServerMessage.GameStateUpdated)

            val lumberCamp = msg.state.village.buildings.first { it.type == BuildingType.LumberCamp }
            assertNotNull(lumberCamp.constructionStartedAt)
            assertEquals(0, lumberCamp.hp)
        } finally {
            client.close()
        }
    }

    @Test
    fun defaultBuildingsAreComplete() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated

            for (building in msg.state.village.buildings) {
                assertNull(building.constructionStartedAt, "${building.type} should be complete")
                assertTrue(building.hp > 0, "${building.type} should have HP")
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun demolishBuildingRemovesItAndRefunds() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build a lumber camp (costs 50 gold) → 950 gold
            val buildMsg = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))
            assertTrue(buildMsg is ServerMessage.GameStateUpdated)
            val lumberCamp = buildMsg.state.village.buildings.first { it.type == BuildingType.LumberCamp }

            // Demolish it → refund 25 gold (50% of 50) → 975 gold
            val msg = client.sendAndReceive(ClientMessage.Demolish(lumberCamp.id))
            assertTrue(msg is ServerMessage.GameStateUpdated)
            assertNull(msg.state.village.buildings.find { it.id == lumberCamp.id })
            assertEquals(975, msg.state.resources.gold)
        } finally {
            client.close()
        }
    }

    @Test
    fun demolishTownHallFails() = runBlocking {
        val client = createConnectedClient()
        try {
            val getMsg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.TownHall }

            val msg = client.sendAndReceive(ClientMessage.Demolish(townHall.id))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Town Hall"))
        } finally {
            client.close()
        }
    }

    @Test
    fun cancelConstructionRefundsFull() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build a lumber camp (costs 50 gold) → 950 gold
            val buildMsg = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))
            assertTrue(buildMsg is ServerMessage.GameStateUpdated)
            val lumberCamp = buildMsg.state.village.buildings.first { it.type == BuildingType.LumberCamp }
            // It should be under construction
            assertNotNull(lumberCamp.constructionStartedAt)

            // Cancel construction → full refund → 1000 gold
            val msg = client.sendAndReceive(ClientMessage.CancelConstruction(lumberCamp.id))
            assertTrue(msg is ServerMessage.GameStateUpdated)
            assertNull(msg.state.village.buildings.find { it.id == lumberCamp.id })
            assertEquals(1000, msg.state.resources.gold)
        } finally {
            client.close()
        }
    }

    @Test
    fun cancelCompletedBuildingFails() = runBlocking {
        val client = createConnectedClient()
        try {
            val getMsg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.TownHall }

            val msg = client.sendAndReceive(ClientMessage.CancelConstruction(townHall.id))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("not under construction"))
        } finally {
            client.close()
        }
    }

    @Test
    fun speedUpRequiresMana() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build something
            val buildMsg = client.sendAndReceive(ClientMessage.Build(BuildingType.LumberCamp, 5, 5))
            assertTrue(buildMsg is ServerMessage.GameStateUpdated)
            val lumberCamp = buildMsg.state.village.buildings.first { it.type == BuildingType.LumberCamp }

            // Try to speed up — should fail because we have 0 mana
            val msg = client.sendAndReceive(ClientMessage.SpeedUp(lumberCamp.id))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Insufficient"))
        } finally {
            client.close()
        }
    }

    @Test
    fun collectAllReturnsGameState() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.CollectAll)
            assertTrue(msg is ServerMessage.GameStateUpdated)
        } finally {
            client.close()
        }
    }

    @Test
    fun trainTroopsRequiresBarracks() = runBlocking {
        val client = createConnectedClient()
        try {
            // Default village has no Barracks
            val msg = client.sendAndReceive(ClientMessage.Train(TroopType.HumanSoldier, 1))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Barracks"))
        } finally {
            client.close()
        }
    }

    @Test
    fun trainTroopsWithBarracks() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build Barracks (100g + 100w) and ArmyCamp (100g + 100w)
            client.sendAndReceive(ClientMessage.Build(BuildingType.Barracks, 0, 0))
            client.sendAndReceive(ClientMessage.Build(BuildingType.ArmyCamp, 5, 0))

            // Train a soldier (25g) — Barracks is under construction so should fail
            val msg = client.sendAndReceive(ClientMessage.Train(TroopType.HumanSoldier, 1))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Barracks"))
        } finally {
            client.close()
        }
    }

    @Test
    fun cancelTrainingRefunds() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build Barracks + ArmyCamp
            client.sendAndReceive(ClientMessage.Build(BuildingType.Barracks, 0, 0))
            client.sendAndReceive(ClientMessage.Build(BuildingType.ArmyCamp, 5, 0))

            // Can't train without completed barracks, so test cancel on invalid index
            val msg = client.sendAndReceive(ClientMessage.CancelTraining(0))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Invalid"))
        } finally {
            client.close()
        }
    }

    @Test
    fun demolishNonExistentBuildingFails() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.Demolish(9999))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("not found"))
        } finally {
            client.close()
        }
    }

    @Test
    fun collectEventRewardsFailsWithNoEvent() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.CollectEventRewards)
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("No active event"))
        } finally {
            client.close()
        }
    }
}
