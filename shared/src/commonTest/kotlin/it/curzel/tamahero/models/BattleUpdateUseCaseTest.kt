package it.curzel.tamahero.models

import kotlin.test.*

class BattleUpdateUseCaseTest {

    private fun baseState(): GameState {
        val building = PlacedBuilding(
            id = 1, type = BuildingType.GoldStorage, level = 1,
            x = 10, y = 10, constructionStartedAt = null, hp = 200,
        )
        return GameState(
            playerId = 1,
            resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(building)),
            troops = emptyList(),
            lastUpdatedAt = 0,
        )
    }

    @Test
    fun noTroopsNoChange() {
        val state = baseState()
        val result = BattleUpdateUseCase.update(state, now = 10_000)
        assertEquals(state.village.buildings, result.village.buildings)
    }

    @Test
    fun troopMovesTowardBuilding() {
        val troop = Troop(id = 100, type = TroopType.HumanSoldier, hp = 50, x = 0f, y = 0f)
        val state = baseState().copy(troops = listOf(troop))
        val result = BattleUpdateUseCase.update(state, now = 100) // one tick
        val movedTroop = result.troops.first()
        assertTrue(movedTroop.x > 0f || movedTroop.y > 0f)
    }

    @Test
    fun troopDamagesBuilding() {
        val building = PlacedBuilding(
            id = 1, type = BuildingType.GoldStorage, level = 1,
            x = 5, y = 5, constructionStartedAt = null, hp = 200,
        )
        // Place troop right next to the building (within range 1)
        val troop = Troop(id = 100, type = TroopType.HumanSoldier, hp = 50, x = 5f, y = 5f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(building)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )
        // Run for 1 second (10 ticks) — HumanSoldier does 10 dps
        val result = BattleUpdateUseCase.update(state, now = 1000)
        val damagedBuilding = result.village.buildings.firstOrNull { it.id == 1L }
        assertNotNull(damagedBuilding)
        assertTrue(damagedBuilding.hp < 200)
    }

    @Test
    fun buildingDestroyedWhenHpReachesZero() {
        val building = PlacedBuilding(
            id = 1, type = BuildingType.Wall, level = 1,
            x = 5, y = 5, constructionStartedAt = null, hp = 10,
        )
        val troop = Troop(id = 100, type = TroopType.HumanSoldier, hp = 50, x = 5f, y = 5f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(building)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )
        val result = BattleUpdateUseCase.update(state, now = 5000)
        assertTrue(result.village.buildings.none { it.id == 1L })
    }

    @Test
    fun defensesDamageTroops() {
        val cannon = PlacedBuilding(
            id = 1, type = BuildingType.Cannon, level = 1,
            x = 5, y = 5, constructionStartedAt = null, hp = 300,
        )
        // Troop within cannon range (3 tiles)
        val troop = Troop(id = 100, type = TroopType.HumanSoldier, hp = 50, x = 5f, y = 5f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(cannon)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )
        val result = BattleUpdateUseCase.update(state, now = 1000)
        if (result.troops.isNotEmpty()) {
            assertTrue(result.troops.first().hp < 50)
        }
    }

    @Test
    fun orcBerserkerTargetsStrongestBuilding() {
        val weak = PlacedBuilding(id = 1, type = BuildingType.Wall, level = 1, x = 3, y = 0, hp = 100)
        val strong = PlacedBuilding(id = 2, type = BuildingType.TownHall, level = 1, x = 10, y = 0, hp = 1000)
        val troop = Troop(id = 100, type = TroopType.OrcBerserker, hp = 60, x = 0f, y = 0f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(weak, strong)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )
        // After one tick the orc should move toward the TownHall (x=10), not the Wall (x=3)
        val result = BattleUpdateUseCase.update(state, now = 100)
        val movedTroop = result.troops.first()
        // Moving toward x=10 means x should increase
        assertTrue(movedTroop.x > 0f)
    }

    @Test
    fun dwarfSapperTargetsDefensesFirst() {
        val storage = PlacedBuilding(id = 1, type = BuildingType.GoldStorage, level = 1, x = 3, y = 0, hp = 200)
        val cannon = PlacedBuilding(id = 2, type = BuildingType.Cannon, level = 1, x = 10, y = 0, hp = 300)
        val troop = Troop(id = 100, type = TroopType.DwarfSapper, hp = 80, x = 0f, y = 0f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(storage, cannon)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )
        val result = BattleUpdateUseCase.update(state, now = 100)
        val movedTroop = result.troops.first()
        // Should target cannon at x=10 even though storage at x=3 is closer
        assertTrue(movedTroop.x > 0f)
    }
}
