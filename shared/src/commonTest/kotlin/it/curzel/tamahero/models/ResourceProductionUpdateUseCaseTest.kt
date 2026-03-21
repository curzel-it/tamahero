package it.curzel.tamahero.models

import kotlin.test.*

class ResourceProductionUpdateUseCaseTest {

    private fun completeLumberCamp(lastCollectedAt: Long) = PlacedBuilding(
        id = 1, type = BuildingType.LumberCamp, level = 1,
        x = 0, y = 0, constructionStartedAt = null,
        lastCollectedAt = lastCollectedAt, hp = 200,
    )

    private fun completeWoodStorage() = PlacedBuilding(
        id = 10, type = BuildingType.WoodStorage, level = 1,
        x = 5, y = 5, constructionStartedAt = null, hp = 200,
    )

    private fun completeGoldStorage() = PlacedBuilding(
        id = 11, type = BuildingType.GoldStorage, level = 1,
        x = 7, y = 7, constructionStartedAt = null, hp = 200,
    )

    @Test
    fun noProducersNoChange() {
        val resources = Resources(gold = 100)
        val buildings = listOf(completeWoodStorage())
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = 100_000)
        assertEquals(100, result.gold)
    }

    @Test
    fun lumberCampProducesWood() {
        val start = 0L
        val oneHourLater = 3_600_000L
        val resources = Resources()
        val buildings = listOf(completeLumberCamp(start), completeWoodStorage())
        // LumberCamp level 1 produces 100 wood/hour
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = oneHourLater)
        assertEquals(100, result.wood)
    }

    @Test
    fun productionCappedByStorage() {
        val start = 0L
        val tenHoursLater = 36_000_000L
        val resources = Resources()
        val buildings = listOf(completeLumberCamp(start), completeWoodStorage())
        // 10 hours * 100/hour = 1000 wood, WoodStorage level 1 cap = 1000
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = tenHoursLater)
        assertEquals(1000, result.wood)
    }

    @Test
    fun productionCappedExceedingStorage() {
        val start = 0L
        val twentyHoursLater = 72_000_000L
        val resources = Resources()
        val buildings = listOf(completeLumberCamp(start), completeWoodStorage())
        // 20 hours * 100/hour = 2000 wood, but cap is 1000
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = twentyHoursLater)
        assertEquals(1000, result.wood)
    }

    @Test
    fun buildingUnderConstructionDoesNotProduce() {
        val now = 5_000L
        val building = PlacedBuilding(
            id = 1, type = BuildingType.LumberCamp, level = 1,
            x = 0, y = 0, constructionStartedAt = 0, lastCollectedAt = 0, hp = 0,
        )
        val resources = Resources()
        val buildings = listOf(building, completeWoodStorage())
        // LumberCamp takes 10s to build, at 5s it's still under construction
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = now)
        assertEquals(0, result.wood)
    }

    @Test
    fun lastCollectedAtUpdated() {
        val now = 3_600_000L
        val buildings = listOf(completeLumberCamp(0), completeWoodStorage())
        val (_, updatedBuildings) = ResourceProductionUpdateUseCase.update(Resources(), buildings, now)
        val producer = updatedBuildings.first { it.type == BuildingType.LumberCamp }
        assertEquals(now, producer.lastCollectedAt)
    }

    @Test
    fun multipleProducersAccumulate() {
        val start = 0L
        val oneHourLater = 3_600_000L
        val goldMine = PlacedBuilding(
            id = 2, type = BuildingType.GoldMine, level = 1,
            x = 3, y = 3, constructionStartedAt = null,
            lastCollectedAt = start, hp = 200,
        )
        val buildings = listOf(
            completeLumberCamp(start), goldMine,
            completeWoodStorage(), completeGoldStorage(),
        )
        val (result, _) = ResourceProductionUpdateUseCase.update(Resources(), buildings, now = oneHourLater)
        assertEquals(100, result.wood)
        assertEquals(100, result.gold)
    }
}
