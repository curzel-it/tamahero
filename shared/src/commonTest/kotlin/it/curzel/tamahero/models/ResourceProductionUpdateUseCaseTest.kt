package it.curzel.tamahero.models

import kotlin.test.*

class ResourceProductionUpdateUseCaseTest {

    private fun completeAlloyRefinery(lastCollectedAt: Long) = PlacedBuilding(
        id = 1, type = BuildingType.MetalMine, level = 1,
        x = 0, y = 0, constructionStartedAt = null,
        lastCollectedAt = lastCollectedAt, hp = 200,
    )

    private fun completeAlloySilo() = PlacedBuilding(
        id = 10, type = BuildingType.MetalStorage, level = 1,
        x = 5, y = 5, constructionStartedAt = null, hp = 200,
    )

    private fun completeCreditVault() = PlacedBuilding(
        id = 11, type = BuildingType.CrystalStorage, level = 1,
        x = 7, y = 7, constructionStartedAt = null, hp = 200,
    )

    @Test
    fun noProducersNoChange() {
        val resources = Resources(credits = 100)
        val buildings = listOf(completeAlloySilo())
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = 100_000)
        assertEquals(100, result.credits)
    }

    @Test
    fun lumberCampProducesWood() {
        val start = 0L
        val oneHourLater = 3_600_000L
        val resources = Resources()
        val buildings = listOf(completeAlloyRefinery(start), completeAlloySilo())
        // AlloyRefinery level 1 produces 100 alloy/hour
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = oneHourLater)
        assertEquals(100, result.metal)
    }

    @Test
    fun productionCappedByStorage() {
        val start = 0L
        val tenHoursLater = 36_000_000L
        val resources = Resources()
        val buildings = listOf(completeAlloyRefinery(start), completeAlloySilo())
        // 10 hours * 100/hour = 1000 alloy, AlloySilo level 1 cap = 1000
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = tenHoursLater)
        assertEquals(1000, result.metal)
    }

    @Test
    fun productionCappedExceedingStorage() {
        val start = 0L
        val twentyHoursLater = 72_000_000L
        val resources = Resources()
        val buildings = listOf(completeAlloyRefinery(start), completeAlloySilo())
        // 20 hours * 100/hour = 2000 alloy, but cap is 1000
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = twentyHoursLater)
        assertEquals(1000, result.metal)
    }

    @Test
    fun buildingUnderConstructionDoesNotProduce() {
        val now = 5_000L
        val building = PlacedBuilding(
            id = 1, type = BuildingType.MetalMine, level = 1,
            x = 0, y = 0, constructionStartedAt = 0, lastCollectedAt = 0, hp = 0,
        )
        val resources = Resources()
        val buildings = listOf(building, completeAlloySilo())
        // AlloyRefinery takes 10s to build, at 5s it's still under construction
        val (result, _) = ResourceProductionUpdateUseCase.update(resources, buildings, now = now)
        assertEquals(0, result.metal)
    }

    @Test
    fun lastCollectedAtUpdated() {
        val now = 3_600_000L
        val buildings = listOf(completeAlloyRefinery(0), completeAlloySilo())
        val (_, updatedBuildings) = ResourceProductionUpdateUseCase.update(Resources(), buildings, now)
        val producer = updatedBuildings.first { it.type == BuildingType.MetalMine }
        assertEquals(now, producer.lastCollectedAt)
    }

    @Test
    fun multipleProducersAccumulate() {
        val start = 0L
        val oneHourLater = 3_600_000L
        val crystalMine = PlacedBuilding(
            id = 2, type = BuildingType.CrystalMine, level = 1,
            x = 3, y = 3, constructionStartedAt = null,
            lastCollectedAt = start, hp = 200,
        )
        val buildings = listOf(
            completeAlloyRefinery(start), crystalMine,
            completeAlloySilo(), completeCreditVault(),
        )
        val (result, _) = ResourceProductionUpdateUseCase.update(Resources(), buildings, now = oneHourLater)
        assertEquals(100, result.metal)
        assertEquals(50, result.crystal) // CrystalMine L1 produces 50 crystal/hr
    }
}
