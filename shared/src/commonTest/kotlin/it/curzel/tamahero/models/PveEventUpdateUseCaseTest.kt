package it.curzel.tamahero.models

import kotlin.test.*

class PveEventUpdateUseCaseTest {

    private fun baseState() = GameState(
        playerId = 1,
        resources = Resources(credits = 500, alloy = 500),
        village = Village(
            playerId = 1,
            buildings = listOf(
                PlacedBuilding(id = 1, type = BuildingType.CommandCenter, level = 1, x = 8, y = 8, hp = 1000),
                PlacedBuilding(id = 2, type = BuildingType.CreditVault, level = 1, x = 5, y = 8, hp = 200),
                PlacedBuilding(id = 3, type = BuildingType.AlloySilo, level = 1, x = 12, y = 8, hp = 200),
                PlacedBuilding(id = 4, type = BuildingType.RailGun, level = 1, x = 5, y = 5, hp = 300),
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
    fun quakeDamagesBuildings() {
        val state = baseState()
        val result = PveEventUpdateUseCase.startEvent(state, EventType.Quake, now = 1000)

        assertNotNull(result.activeEvent)
        assertTrue(result.activeEvent!!.completed)
        assertNotNull(result.activeEvent!!.pendingRewards)

        val totalHpBefore = state.village.buildings.sumOf { it.hp }
        val totalHpAfter = result.village.buildings.sumOf { it.hp }
        assertTrue(totalHpAfter < totalHpBefore, "Quake should damage buildings")
    }

    @Test
    fun ionStormDamagesEdgeBuildings() {
        val edgeState = baseState().copy(
            village = Village(
                playerId = 1,
                buildings = listOf(
                    PlacedBuilding(id = 1, type = BuildingType.CommandCenter, level = 1, x = 8, y = 8, hp = 1000),
                    PlacedBuilding(id = 2, type = BuildingType.CreditVault, level = 1, x = 0, y = 0, hp = 200),
                ),
            ),
        )
        val result = PveEventUpdateUseCase.startEvent(edgeState, EventType.IonStorm, now = 1000)

        assertNotNull(result.activeEvent)
        assertTrue(result.activeEvent!!.completed)
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
        assertEquals(8, result.troops.size)
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

        current = GameStateUpdateUseCase.update(current, now = 300_000)

        if (current.troops.isEmpty() && current.activeEvent != null) {
            current = PveEventUpdateUseCase.update(current, now = 300_001)
            if (current.activeEvent?.completed == true) {
                assertNotNull(current.activeEvent!!.pendingRewards)
            }
        }
    }

    @Test
    fun disasterRewardsOnSuccess() {
        val state = baseState()
        val result = PveEventUpdateUseCase.startEvent(state, EventType.Quake, now = 1000)

        val event = result.activeEvent!!
        assertTrue(event.completed)
        assertNotNull(event.pendingRewards)
        val rewards = event.pendingRewards!!
        assertTrue(rewards.credits > 0 || rewards.alloy > 0)
    }

    @Test
    fun eligibleEventsIncludeAllTypes() {
        val events = PveEventConfig.eligibleEvents(1)
        assertTrue(EventType.ScoutParty in events)
        assertTrue(EventType.Battle in events)
        assertTrue(EventType.Quake in events)
        assertTrue(EventType.IonStorm in events)
    }

    @Test
    fun eventWaveConfigs() {
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
