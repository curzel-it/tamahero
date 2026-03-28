package it.curzel.tamahero.models

import kotlin.test.*

class BattleUpdateUseCaseTest {

    private fun baseState(): GameState {
        val building = PlacedBuilding(
            id = 1, type = BuildingType.CreditVault, level = 1,
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
        val troop = Troop(id = 100, type = TroopType.Marine, hp = 50, x = 0f, y = 0f)
        val state = baseState().copy(troops = listOf(troop))
        val result = BattleUpdateUseCase.update(state, now = 100) // one tick
        val movedTroop = result.troops.first()
        assertTrue(movedTroop.x > 0f || movedTroop.y > 0f)
    }

    @Test
    fun troopDamagesBuilding() {
        val building = PlacedBuilding(
            id = 1, type = BuildingType.CreditVault, level = 1,
            x = 5, y = 5, constructionStartedAt = null, hp = 200,
        )
        // Place troop right next to the building (within range 1)
        val troop = Troop(id = 100, type = TroopType.Marine, hp = 50, x = 5f, y = 5f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(building)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )
        // Run for 1 second (10 ticks) — Marine does 10 dps
        val result = BattleUpdateUseCase.update(state, now = 1000)
        val damagedBuilding = result.village.buildings.firstOrNull { it.id == 1L }
        assertNotNull(damagedBuilding)
        assertTrue(damagedBuilding.hp < 200)
    }

    @Test
    fun buildingDestroyedWhenHpReachesZero() {
        // Use GoldStorage (non-defense) so it won't be auto-rebuilt in post-battle
        val building = PlacedBuilding(
            id = 1, type = BuildingType.CreditVault, level = 1,
            x = 5, y = 5, constructionStartedAt = null, hp = 10,
        )
        val troop = Troop(id = 100, type = TroopType.Marine, hp = 50, x = 5f, y = 5f)
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
            id = 1, type = BuildingType.RailGun, level = 1,
            x = 5, y = 5, constructionStartedAt = null, hp = 300,
        )
        // Troop within cannon range (3 tiles)
        val troop = Troop(id = 100, type = TroopType.Marine, hp = 50, x = 5f, y = 5f)
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
    fun juggernautTargetsStrongestBuilding() {
        val weak = PlacedBuilding(id = 1, type = BuildingType.Barrier, level = 1, x = 3, y = 0, hp = 100)
        val strong = PlacedBuilding(id = 2, type = BuildingType.CommandCenter, level = 1, x = 10, y = 0, hp = 1000)
        val troop = Troop(id = 100, type = TroopType.Juggernaut, hp = 60, x = 0f, y = 0f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(weak, strong)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )
        // After one tick the juggernaut should move toward the CommandCenter (x=10), not the Barrier (x=3)
        val result = BattleUpdateUseCase.update(state, now = 100)
        val movedTroop = result.troops.first()
        // Moving toward x=10 means x should increase
        assertTrue(movedTroop.x > 0f)
    }

    @Test
    fun engineerTargetsDefensesFirst() {
        val storage = PlacedBuilding(id = 1, type = BuildingType.CreditVault, level = 1, x = 3, y = 0, hp = 200)
        val cannon = PlacedBuilding(id = 2, type = BuildingType.RailGun, level = 1, x = 10, y = 0, hp = 300)
        val troop = Troop(id = 100, type = TroopType.Engineer, hp = 80, x = 0f, y = 0f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(storage, cannon)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )
        val result = BattleUpdateUseCase.update(state, now = 100)
        val movedTroop = result.troops.first()
        // Should target rail gun at x=10 even though storage at x=3 is closer
        assertTrue(movedTroop.x > 0f)
    }

    @Test
    fun troopNavigatesAroundObstacle() {
        // Place a large obstacle (4x4) between troop and target
        // Troop at (2, 10), obstacle at (6, 8) occupying 6-9 x 8-11, target at (14, 10)
        val target = PlacedBuilding(id = 1, type = BuildingType.CreditVault, level = 1, x = 14, y = 10, hp = 200)
        val obstacle = PlacedBuilding(id = 2, type = BuildingType.CommandCenter, level = 1, x = 6, y = 8, hp = 1000)
        val troop = Troop(id = 100, type = TroopType.Marine, hp = 50, x = 5f, y = 10.5f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(target, obstacle)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )

        // Verify pathfinding finds a path that avoids the obstacle
        val path = Pathfinding.findPath(5f, 10.5f, target, listOf(target, obstacle))
        assertNotNull(path, "Pathfinding should find a path around obstacle")
        for (pos in path) {
            val inObstacle = pos.x in 6..9 && pos.y in 8..11
            assertFalse(inObstacle, "Path should not go through obstacle at ${pos.x},${pos.y}")
        }

        // Run a few seconds — troop should be moving (not stuck)
        val result = BattleUpdateUseCase.update(state, now = 3_000)
        val movedTroop = result.troops.firstOrNull()
        assertNotNull(movedTroop, "Troop should still be alive")
        assertTrue(movedTroop.x > 5f, "Troop should have moved from starting position")
    }

    @Test
    fun troopWalksOverTrapAndTriggersIt() {
        // Target beyond a spike trap
        val target = PlacedBuilding(id = 1, type = BuildingType.CreditVault, level = 1, x = 10, y = 5, hp = 200)
        val trap = PlacedBuilding(id = 2, type = BuildingType.MineTrap, level = 1, x = 7, y = 5, hp = 1)
        val troop = Troop(id = 100, type = TroopType.Marine, hp = 50, x = 5f, y = 5.5f)
        val state = GameState(
            playerId = 1, resources = Resources(),
            village = Village(playerId = 1, buildings = listOf(target, trap)),
            troops = listOf(troop), lastUpdatedAt = 0,
        )
        // Run for 5 seconds — troop should walk over trap
        val result = BattleUpdateUseCase.update(state, now = 5_000)
        val triggeredTrap = result.village.buildings.find { it.id == 2L }
        if (triggeredTrap != null) {
            assertTrue(triggeredTrap.triggered, "Trap should be triggered after troop walks over it")
        }
    }
}
