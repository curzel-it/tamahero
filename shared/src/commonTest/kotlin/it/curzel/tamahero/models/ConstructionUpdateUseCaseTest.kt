package it.curzel.tamahero.models

import kotlin.test.*

class ConstructionUpdateUseCaseTest {

    @Test
    fun completeBuildingUnchanged() {
        val building = PlacedBuilding(
            id = 1, type = BuildingType.AlloyRefinery, level = 1,
            x = 0, y = 0, constructionStartedAt = null, hp = 200,
        )
        val result = ConstructionUpdateUseCase.update(building, now = 100_000)
        assertEquals(building, result)
    }

    @Test
    fun underConstructionNotEnoughTime() {
        val building = PlacedBuilding(
            id = 1, type = BuildingType.AlloyRefinery, level = 1,
            x = 0, y = 0, constructionStartedAt = 0, hp = 0,
        )
        // AlloyRefinery level 1 takes 10 seconds = 10_000ms
        val result = ConstructionUpdateUseCase.update(building, now = 5_000)
        assertNotNull(result.constructionStartedAt)
    }

    @Test
    fun constructionCompletes() {
        val building = PlacedBuilding(
            id = 1, type = BuildingType.AlloyRefinery, level = 1,
            x = 0, y = 0, constructionStartedAt = 0, hp = 0,
        )
        // AlloyRefinery level 1 takes 10 seconds = 10_000ms
        val result = ConstructionUpdateUseCase.update(building, now = 10_000)
        assertNull(result.constructionStartedAt)
        assertEquals(200, result.hp)
    }

    @Test
    fun constructionCompletesAfterExactTime() {
        val startTime = 50_000L
        val building = PlacedBuilding(
            id = 1, type = BuildingType.CreditMint, level = 2,
            x = 5, y = 5, constructionStartedAt = startTime, hp = 0,
        )
        // CreditMint level 2 takes 30 seconds = 30_000ms
        val result = ConstructionUpdateUseCase.update(building, now = startTime + 30_000)
        assertNull(result.constructionStartedAt)
        assertEquals(300, result.hp)
    }
}
