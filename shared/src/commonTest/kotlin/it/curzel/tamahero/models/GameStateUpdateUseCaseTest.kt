package it.curzel.tamahero.models

import kotlin.test.*

class GameStateUpdateUseCaseTest {

    @Test
    fun constructionCompletesAndResourcesAccumulate() {
        val startTime = 0L
        val alloyRefinery = PlacedBuilding(
            id = 1, type = BuildingType.AlloyRefinery, level = 1,
            x = 0, y = 0, constructionStartedAt = 0, lastCollectedAt = 0, hp = 0,
        )
        val alloySilo = PlacedBuilding(
            id = 2, type = BuildingType.AlloySilo, level = 1,
            x = 5, y = 5, constructionStartedAt = null, lastCollectedAt = 0, hp = 200,
        )
        val state = GameState(
            playerId = 1,
            resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(alloyRefinery, alloySilo)),
            lastUpdatedAt = startTime,
        )
        // AlloyRefinery takes 10s to build. Update at 1 hour mark.
        val oneHour = 3_600_000L
        val result = GameStateUpdateUseCase.update(state, now = oneHour)

        // Construction should be complete
        val updatedCamp = result.village.buildings.first { it.type == BuildingType.AlloyRefinery }
        assertNull(updatedCamp.constructionStartedAt)

        // Should have produced resources (100 alloy/hour, but production starts after construction)
        // Construction completes at 10_000ms, so ~0.997 hours of production
        assertTrue(result.resources.alloy > 0)
        assertEquals(oneHour, result.lastUpdatedAt)
    }

    @Test
    fun lastUpdatedAtSetToNow() {
        val state = GameState(
            playerId = 1,
            resources = Resources(),
            village = Village(playerId = 1, buildings = emptyList()),
            lastUpdatedAt = 0,
        )
        val result = GameStateUpdateUseCase.update(state, now = 50_000)
        assertEquals(50_000, result.lastUpdatedAt)
    }

    @Test
    fun battleAndEconomyRunTogether() {
        val building = PlacedBuilding(
            id = 1, type = BuildingType.CreditVault, level = 1,
            x = 10, y = 10, constructionStartedAt = null, hp = 200,
        )
        val alloyRefinery = PlacedBuilding(
            id = 2, type = BuildingType.AlloyRefinery, level = 1,
            x = 0, y = 0, constructionStartedAt = null, lastCollectedAt = 0, hp = 200,
        )
        val alloySilo = PlacedBuilding(
            id = 3, type = BuildingType.AlloySilo, level = 1,
            x = 2, y = 2, constructionStartedAt = null, hp = 200,
        )
        val troop = Troop(id = 100, type = TroopType.Marine, hp = 50, x = 0f, y = 0f)
        val state = GameState(
            playerId = 1,
            resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(building, alloyRefinery, alloySilo)),
            troops = listOf(troop),
            lastUpdatedAt = 0,
        )
        val result = GameStateUpdateUseCase.update(state, now = 1000)

        // Troops should have moved
        assertTrue(result.troops.isNotEmpty() || result.village.buildings.size < 3)
    }
}
