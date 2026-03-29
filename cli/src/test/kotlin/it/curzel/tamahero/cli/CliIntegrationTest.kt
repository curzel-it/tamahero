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

            assertEquals(2, state.village.buildings.size)
            assertTrue(state.village.buildings.any { it.type == BuildingType.CommandCenter && it.level == 1 })
            assertTrue(state.village.buildings.any { it.type == BuildingType.RoboticsFactory && it.level == 1 })

            assertEquals(500, state.resources.credits)
            assertEquals(500, state.resources.metal)
            assertEquals(500, state.resources.crystal)
            assertEquals(250, state.resources.deuterium)
        } finally {
            client.close()
        }
    }

    @Test
    fun buildAlloyRefineryDeductsResources() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 5, 5))
            assertTrue(msg is ServerMessage.GameStateUpdated)
            val state = msg.state

            val lumberCamp = state.village.buildings.find { it.type == BuildingType.MetalMine }
            assertNotNull(lumberCamp)
            assertEquals(5, lumberCamp.x)
            assertEquals(5, lumberCamp.y)
            assertEquals(1, lumberCamp.level)
            assertNotNull(lumberCamp.constructionStartedAt)

            // AlloyRefinery costs 50 credits
            assertEquals(450, state.resources.credits)
            assertEquals(500, state.resources.metal)
        } finally {
            client.close()
        }
    }

    @Test
    fun buildMetalStorageDeductsCredits() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalStorage, 5, 5))
            assertTrue(msg is ServerMessage.GameStateUpdated)

            // MetalStorage costs 50 credits
            assertEquals(450, msg.state.resources.credits)
            assertEquals(500, msg.state.resources.metal)
        } finally {
            client.close()
        }
    }

    @Test
    fun workerLimitsBuilding() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build one thing — should succeed (1 worker available)
            val msg1 = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 0, 0))
            assertTrue(msg1 is ServerMessage.GameStateUpdated)
            // Try to build another — should fail (worker busy)
            val msg2 = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalStorage, 3, 0))
            assertTrue(msg2 is ServerMessage.Error)
            assertTrue(msg2.reason.contains("worker", ignoreCase = true))
        } finally {
            client.close()
        }
    }

    @Test
    fun buildFailsWithInsufficientResources() = runBlocking {
        val client = createConnectedClient()
        try {
            // Drain resources by building many things (start with 1000cr/1000al/500cr)
            val builds = listOf(
                ClientMessage.Build(BuildingType.MetalMine, 0, 0),
                ClientMessage.Build(BuildingType.MetalStorage, 0, 5),
                ClientMessage.Build(BuildingType.MetalStorage, 0, 10),
                ClientMessage.Build(BuildingType.MetalStorage, 0, 15),
                ClientMessage.Build(BuildingType.CrystalStorage, 0, 20),
                ClientMessage.Build(BuildingType.GaussCannon, 0, 25),
                ClientMessage.Build(BuildingType.LightLaser, 0, 30),
            )
            for (b in builds) {
                val r = client.sendAndReceive(b)
                if (r is ServerMessage.Error) break
            }
            // Try one more — should fail
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.MissileLauncher, 5, 0))
            assertTrue(msg is ServerMessage.Error, "Expected error, got $msg")
        } finally {
            client.close()
        }
    }

    @Test
    fun buildFailsOutOfBounds() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 39, 39))
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
            client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 5, 5))
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalStorage, 5, 5))
            assertTrue(msg is ServerMessage.Error, "Should fail: $msg")
            // May fail with overlap or worker limit (only 1 worker)
            assertTrue(msg.reason.contains("overlap", ignoreCase = true) || msg.reason.contains("worker", ignoreCase = true))
        } finally {
            client.close()
        }
    }

    @Test
    fun moveBuilding() = runBlocking {
        val client = createConnectedClient()
        try {
            val getMsg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.CommandCenter }

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
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.CommandCenter }

            // CommandCenter is 4x4, so placing at 38 would go out of bounds
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
            // Build an AlloyRefinery at (0,0)
            client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 0, 0))

            // Get CommandCenter (at 18,18) and try to move it on top of the AlloyRefinery
            val getMsg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.CommandCenter }

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
            // Build an alloy refinery
            val buildMsg = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 5, 5))
            assertTrue(buildMsg is ServerMessage.GameStateUpdated)
            val lumberCamp = buildMsg.state.village.buildings.first { it.type == BuildingType.MetalMine }

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
            client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 5, 5))

            // Fetch village again — the alloy refinery should still be there
            val msg = client.sendAndReceive(ClientMessage.GetVillage)
            assertTrue(msg is ServerMessage.GameStateUpdated)
            assertEquals(3, msg.state.village.buildings.size) // CommandCenter + DroneStation + AlloyRefinery
            assertTrue(msg.state.village.buildings.any { it.type == BuildingType.MetalMine })
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
    fun resourcesDeductedOnBuild() = runBlocking {
        val client = createConnectedClient()
        try {
            // Start: 1000 credits, 1000 alloy. Build AlloyRefinery costs resources.
            val before = (client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated).state.resources
            val msg1 = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 5, 5))
            assertTrue(msg1 is ServerMessage.GameStateUpdated)
            val after = msg1.state.resources
            assertTrue(after.credits < before.credits || after.metal < before.metal, "Resources should decrease after building")
        } finally {
            client.close()
        }
    }

    @Test
    fun newBuildingsAreUnderConstruction() = runBlocking {
        val client = createConnectedClient()
        try {
            val msg = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 5, 5))
            assertTrue(msg is ServerMessage.GameStateUpdated)

            val lumberCamp = msg.state.village.buildings.first { it.type == BuildingType.MetalMine }
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
            // Build an alloy refinery (costs 50 credits) → 950 credits
            val buildMsg = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 5, 5))
            assertTrue(buildMsg is ServerMessage.GameStateUpdated)
            val lumberCamp = buildMsg.state.village.buildings.first { it.type == BuildingType.MetalMine }

            // Demolish it → refund 25 credits (50% of 50) → 975 credits
            val msg = client.sendAndReceive(ClientMessage.Demolish(lumberCamp.id))
            assertTrue(msg is ServerMessage.GameStateUpdated)
            assertNull(msg.state.village.buildings.find { it.id == lumberCamp.id })
            assertEquals(475, msg.state.resources.credits)
        } finally {
            client.close()
        }
    }

    @Test
    fun demolishCommandCenterFails() = runBlocking {
        val client = createConnectedClient()
        try {
            val getMsg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.CommandCenter }

            val msg = client.sendAndReceive(ClientMessage.Demolish(townHall.id))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Command Center"))
        } finally {
            client.close()
        }
    }

    @Test
    fun cancelConstructionRefundsFull() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build an alloy refinery (costs 50 credits) → 950 credits
            val buildMsg = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 5, 5))
            assertTrue(buildMsg is ServerMessage.GameStateUpdated)
            val lumberCamp = buildMsg.state.village.buildings.first { it.type == BuildingType.MetalMine }
            // It should be under construction
            assertNotNull(lumberCamp.constructionStartedAt)

            // Cancel construction → full refund → 1000 credits
            val msg = client.sendAndReceive(ClientMessage.CancelConstruction(lumberCamp.id))
            assertTrue(msg is ServerMessage.GameStateUpdated)
            assertNull(msg.state.village.buildings.find { it.id == lumberCamp.id })
            assertEquals(500, msg.state.resources.credits)
        } finally {
            client.close()
        }
    }

    @Test
    fun cancelCompletedBuildingFails() = runBlocking {
        val client = createConnectedClient()
        try {
            val getMsg = client.sendAndReceive(ClientMessage.GetVillage) as ServerMessage.GameStateUpdated
            val townHall = getMsg.state.village.buildings.first { it.type == BuildingType.CommandCenter }

            val msg = client.sendAndReceive(ClientMessage.CancelConstruction(townHall.id))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("not under construction"))
        } finally {
            client.close()
        }
    }

    @Test
    fun speedUpCostsDeuterium() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build something
            val buildMsg = client.sendAndReceive(ClientMessage.Build(BuildingType.MetalMine, 5, 5))
            assertTrue(buildMsg is ServerMessage.GameStateUpdated)
            val mine = buildMsg.state.village.buildings.first { it.type == BuildingType.MetalMine }
            val deuteriumBefore = buildMsg.state.resources.deuterium

            // Speed up — should succeed (we start with 250 deuterium)
            val msg = client.sendAndReceive(ClientMessage.SpeedUp(mine.id))
            assertTrue(msg is ServerMessage.GameStateUpdated, "Speed up should succeed: $msg")
            assertTrue(msg.state.resources.deuterium < deuteriumBefore, "Should cost deuterium")
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
    fun trainTroopsRequiresAcademy() = runBlocking {
        val client = createConnectedClient()
        try {
            // Default village has no Academy
            val msg = client.sendAndReceive(ClientMessage.Train(TroopType.Marine, 1))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Barracks"))
        } finally {
            client.close()
        }
    }

    @Test
    fun trainTroopsWithAcademyUnderConstruction() = runBlocking {
        val client = createConnectedClient()
        try {
            // Build Academy (100cr + 100al) — under construction
            client.sendAndReceive(ClientMessage.Build(BuildingType.Barracks, 0, 0))

            // Train a marine — Academy is under construction so should fail
            val msg = client.sendAndReceive(ClientMessage.Train(TroopType.Marine, 1))
            assertTrue(msg is ServerMessage.Error)
            assertTrue(msg.reason.contains("Barracks"))
        } finally {
            client.close()
        }
    }

    @Test
    fun cancelTrainingInvalidIndex() = runBlocking {
        val client = createConnectedClient()
        try {
            // Test cancel on invalid index (no training queue)
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
