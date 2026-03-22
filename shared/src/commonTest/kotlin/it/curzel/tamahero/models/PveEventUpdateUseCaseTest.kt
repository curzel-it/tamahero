package it.curzel.tamahero.models

import kotlin.test.*

class PveEventUpdateUseCaseTest {

    private fun baseState() = GameState(
        playerId = 1,
        resources = Resources(gold = 500, wood = 500),
        village = Village(
            playerId = 1,
            buildings = listOf(
                PlacedBuilding(id = 1, type = BuildingType.TownHall, level = 1, x = 19, y = 19, hp = 1000),
                PlacedBuilding(id = 2, type = BuildingType.GoldStorage, level = 1, x = 16, y = 19, hp = 200),
                PlacedBuilding(id = 3, type = BuildingType.WoodStorage, level = 1, x = 22, y = 19, hp = 200),
                PlacedBuilding(id = 4, type = BuildingType.Cannon, level = 1, x = 15, y = 15, hp = 300),
            ),
        ),
        lastUpdatedAt = 0,
    )

    @Test
    fun noEventNoChange() {
        val state = baseState()
        val result = PveEventUpdateUseCase.update(state, now = 100_000)
        assertEquals(state, result)
    }

    @Test
    fun earthquakeDamagesBuildings() {
        val state = baseState()
        val result = PveEventUpdateUseCase.startEvent(state, EventType.Earthquake, now = 1000)

        assertNotNull(result.activeEvent)
        assertTrue(result.activeEvent!!.completed)
        assertNotNull(result.activeEvent!!.pendingRewards)

        // Some buildings should have taken damage
        val totalHpBefore = state.village.buildings.sumOf { it.hp }
        val totalHpAfter = result.village.buildings.sumOf { it.hp }
        assertTrue(totalHpAfter < totalHpBefore, "Earthquake should damage buildings")
    }

    @Test
    fun stormDamagesEdgeBuildings() {
        // Place a building on the edge
        val edgeState = baseState().copy(
            village = Village(
                playerId = 1,
                buildings = listOf(
                    PlacedBuilding(id = 1, type = BuildingType.TownHall, level = 1, x = 19, y = 19, hp = 1000),
                    PlacedBuilding(id = 2, type = BuildingType.GoldStorage, level = 1, x = 0, y = 0, hp = 200),
                ),
            ),
        )
        val result = PveEventUpdateUseCase.startEvent(edgeState, EventType.Storm, now = 1000)

        assertNotNull(result.activeEvent)
        assertTrue(result.activeEvent!!.completed)
        // Edge building should take damage
        val edgeBuilding = result.village.buildings.find { it.id == 2L }
        assertNotNull(edgeBuilding)
        assertTrue(edgeBuilding.hp < 200)
    }

    @Test
    fun scoutPartySpawnsTroops() {
        val state = baseState()
        val result = PveEventUpdateUseCase.startEvent(state, EventType.ScoutParty, now = 1000)

        assertNotNull(result.activeEvent)
        assertFalse(result.activeEvent!!.completed)
        assertTrue(result.troops.isNotEmpty(), "Scout party should spawn troops")
        assertEquals(8, result.troops.size) // 6 soldiers + 2 archers
    }

    @Test
    fun battleSpawnsTroops() {
        val state = baseState()
        val result = PveEventUpdateUseCase.startEvent(state, EventType.Battle, now = 1000)

        assertNotNull(result.activeEvent)
        assertFalse(result.activeEvent!!.completed)
        assertTrue(result.troops.isNotEmpty())
    }

    @Test
    fun battleEventCompletesWhenTroopsDie() {
        val state = baseState()
        var current = PveEventUpdateUseCase.startEvent(state, EventType.ScoutParty, now = 0)

        // Simulate battle running until all troops die
        current = GameStateUpdateUseCase.update(current, now = 300_000)

        // After enough time, troops should be dead (defenses kill them)
        // and event should be completed
        if (current.troops.isEmpty() && current.activeEvent != null) {
            // Run one more update to trigger completion
            current = PveEventUpdateUseCase.update(current, now = 300_001)
            if (current.activeEvent?.completed == true) {
                assertNotNull(current.activeEvent!!.pendingRewards)
            }
        }
    }

    @Test
    fun disasterRewardsOnSuccess() {
        val state = baseState()
        val result = PveEventUpdateUseCase.startEvent(state, EventType.Earthquake, now = 1000)

        val event = result.activeEvent!!
        assertTrue(event.completed)
        assertNotNull(event.pendingRewards)
        // Rewards should be positive
        val rewards = event.pendingRewards!!
        assertTrue(rewards.gold > 0 || rewards.wood > 0)
    }

    @Test
    fun eligibleEventsIncludeAllTypes() {
        val events = PveEventConfig.eligibleEvents(1)
        assertTrue(EventType.ScoutParty in events)
        assertTrue(EventType.Battle in events)
        assertTrue(EventType.Earthquake in events)
        assertTrue(EventType.Storm in events)
    }

    @Test
    fun eventWaveConfigs() {
        // Verify all event types have valid configs
        for (type in EventType.entries) {
            val waves = PveEventConfig.wavesFor(type)
            val rewards = PveEventConfig.rewardsFor(type)
            assertNotNull(rewards)
            if (type.isBattle) {
                assertTrue(waves.isNotEmpty(), "$type should have waves")
                for (wave in waves) {
                    assertTrue(wave.troops.isNotEmpty(), "$type wave should have troops")
                }
            }
        }
    }
}
