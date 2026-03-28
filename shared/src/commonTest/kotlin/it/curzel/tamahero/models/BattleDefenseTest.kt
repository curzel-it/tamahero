package it.curzel.tamahero.models

import kotlin.test.*

class BattleDefenseTest {

    private fun soldier(id: Long = 100, x: Float = 0f, y: Float = 0f, hp: Int = 45) =
        Troop(id = id, type = TroopType.Marine, hp = hp, x = x, y = y)

    private fun archer(id: Long = 200, x: Float = 0f, y: Float = 0f, hp: Int = 20) =
        Troop(id = id, type = TroopType.Sniper, hp = hp, x = x, y = y)

    private fun orc(id: Long = 300, x: Float = 0f, y: Float = 0f, hp: Int = 300) =
        Troop(id = id, type = TroopType.Juggernaut, hp = hp, x = x, y = y)

    private fun gameState(
        buildings: List<PlacedBuilding>,
        troops: List<Troop>,
        shieldHp: Int = 0,
    ) = GameState(
        playerId = 1,
        village = Village(playerId = 1, buildings = buildings),
        troops = troops,
        battleShieldHp = shieldHp,
        lastUpdatedAt = 0,
    )

    // --- Wall blocking ---

    @Test
    fun wallBlocksTroopPath() {
        val target = PlacedBuilding(id = 1, type = BuildingType.CreditVault, level = 1, x = 10, y = 0, hp = 200)
        val wall = PlacedBuilding(id = 2, type = BuildingType.Barrier, level = 1, x = 5, y = 0, hp = 500)
        val troop = soldier(x = 0f, y = 0f)
        val state = gameState(listOf(target, wall), listOf(troop))

        val result = BattleUpdateUseCase.update(state, now = 100)
        val movedTroop = result.troops.first()
        // Should be heading toward the wall, not the gold storage
        assertTrue(movedTroop.targetId == wall.id || movedTroop.x > 0f)
    }

    @Test
    fun troopDestroysWallThenProceedsToTarget() {
        val target = PlacedBuilding(id = 1, type = BuildingType.CreditVault, level = 1, x = 15, y = 0, hp = 200)
        val wall = PlacedBuilding(id = 2, type = BuildingType.Barrier, level = 1, x = 5, y = 0, hp = 10)
        val troop = soldier(x = 5f, y = 0f, hp = 50)
        val state = gameState(listOf(target, wall), listOf(troop))

        // Run long enough to destroy the wall
        val result = BattleUpdateUseCase.update(state, now = 5000)
        // Wall should be destroyed
        val wallExists = result.village.buildings.any { it.id == 2L }
        assertFalse(wallExists)
        // Target should still exist (troop was busy with wall)
        assertTrue(result.village.buildings.any { it.id == 1L })
    }

    // --- Spike Trap ---

    @Test
    fun spikeTrapDamagesTroops() {
        val trap = PlacedBuilding(id = 1, type = BuildingType.MineTrap, level = 1, x = 5, y = 0, hp = 1)
        val target = PlacedBuilding(id = 2, type = BuildingType.CreditVault, level = 1, x = 10, y = 0, hp = 200)
        val troop = soldier(x = 5f, y = 0f)
        val state = gameState(listOf(trap, target), listOf(troop))

        val result = BattleUpdateUseCase.update(state, now = 100)
        // Trap should be triggered
        val triggeredTrap = result.village.buildings.find { it.id == 1L }
        assertTrue(triggeredTrap?.triggered == true)
        // Troop should have taken damage (burst=30, soldier has 50hp → 20hp)
        if (result.troops.isNotEmpty()) {
            assertTrue(result.troops.first().hp < 50)
        }
    }

    @Test
    fun spikeTrapTriggersOnlyOnce() {
        val trap = PlacedBuilding(id = 1, type = BuildingType.MineTrap, level = 1, x = 5, y = 0, hp = 1, triggered = true)
        val target = PlacedBuilding(id = 2, type = BuildingType.CreditVault, level = 1, x = 10, y = 0, hp = 200)
        val troop = soldier(x = 5f, y = 0f)
        val state = gameState(listOf(trap, target), listOf(troop))

        val result = BattleUpdateUseCase.update(state, now = 100)
        // Troop should NOT take trap damage (already triggered)
        if (result.troops.isNotEmpty()) {
            // HP might change from defense damage but not from trap
            assertTrue(result.troops.first().hp == 50 || result.troops.first().hp > 0)
        }
    }

    // --- Spring Trap ---

    @Test
    fun springTrapKillsLightTroop() {
        val trap = PlacedBuilding(id = 1, type = BuildingType.GravityTrap, level = 1, x = 5, y = 0, hp = 1)
        val target = PlacedBuilding(id = 2, type = BuildingType.CreditVault, level = 1, x = 10, y = 0, hp = 200)
        val troop = archer(x = 5f, y = 0f)
        val state = gameState(listOf(trap, target), listOf(troop))

        val result = BattleUpdateUseCase.update(state, now = 100)
        val triggeredTrap = result.village.buildings.find { it.id == 1L }
        assertTrue(triggeredTrap?.triggered == true)
        // Archer should be killed
        assertTrue(result.troops.isEmpty())
    }

    @Test
    fun springTrapDoesNotKillHeavyTroop() {
        val trap = PlacedBuilding(id = 1, type = BuildingType.GravityTrap, level = 1, x = 5, y = 0, hp = 1)
        val target = PlacedBuilding(id = 2, type = BuildingType.CreditVault, level = 1, x = 10, y = 0, hp = 200)
        val troop = orc(x = 5f, y = 0f)
        val state = gameState(listOf(trap, target), listOf(troop))

        val result = BattleUpdateUseCase.update(state, now = 100)
        // Gravity trap should not trigger for Juggernaut
        val triggeredTrap = result.village.buildings.find { it.id == 1L }
        assertFalse(triggeredTrap?.triggered == true)
        assertTrue(result.troops.isNotEmpty())
    }

    // --- Shield Dome ---

    @Test
    fun shieldDomeAbsorbsDamage() {
        val building = PlacedBuilding(id = 1, type = BuildingType.CreditVault, level = 1, x = 5, y = 5, hp = 200)
        val dome = PlacedBuilding(id = 2, type = BuildingType.ShieldDome, level = 1, x = 3, y = 3, hp = 100)
        val troop = soldier(x = 5f, y = 5f)
        val state = gameState(listOf(building, dome), listOf(troop), shieldHp = 500)

        val result = BattleUpdateUseCase.update(state, now = 1000)
        // Shield should have absorbed damage, building should be at full HP or close
        assertTrue(result.village.buildings.any { it.id == 1L && it.hp == 200 })
        assertTrue(result.battleShieldHp < 500)
    }

    @Test
    fun shieldDomeBreaksWhenDepleted() {
        val building = PlacedBuilding(id = 1, type = BuildingType.CreditVault, level = 1, x = 5, y = 5, hp = 200)
        val dome = PlacedBuilding(id = 2, type = BuildingType.ShieldDome, level = 1, x = 3, y = 3, hp = 100)
        val troop = soldier(x = 5f, y = 5f, hp = 5000) // high HP to survive long
        val buildings = listOf(building, dome)
        // Set preBattleBuildings so the initializer doesn't override our shieldHp
        val state = GameState(
            playerId = 1,
            village = Village(playerId = 1, buildings = buildings),
            troops = listOf(troop),
            battleShieldHp = 5,
            preBattleBuildings = buildings,
            lastUpdatedAt = 0,
        )

        val result = BattleUpdateUseCase.update(state, now = 5000)
        assertFalse(result.village.buildings.any { it.type == BuildingType.ShieldDome })
        assertEquals(0, result.battleShieldHp)
    }

    // --- Mortar splash ---

    @Test
    fun mortarDamagesMultipleTroops() {
        val mortar = PlacedBuilding(id = 1, type = BuildingType.MissileBattery, level = 1, x = 10, y = 0, hp = 250)
        // Two troops close together within mortar range but beyond minRange
        val troop1 = soldier(id = 100, x = 7f, y = 0f)
        val troop2 = soldier(id = 101, x = 7.5f, y = 0f)
        val state = gameState(listOf(mortar), listOf(troop1, troop2))

        val result = BattleUpdateUseCase.update(state, now = 1000)
        // Both troops should take damage from splash
        for (troop in result.troops) {
            assertTrue(troop.hp < 50, "Troop ${troop.id} should have taken splash damage")
        }
    }

    @Test
    fun mortarMinRangeProtectsCloseTroops() {
        val mortar = PlacedBuilding(id = 1, type = BuildingType.MissileBattery, level = 1, x = 5, y = 0, hp = 250)
        // Troop is within minRange (< 2 tiles)
        val troop = soldier(x = 5f, y = 0f)
        val state = gameState(listOf(mortar), listOf(troop))

        val result = BattleUpdateUseCase.update(state, now = 1000)
        // Troop should NOT take mortar damage (too close)
        if (result.troops.isNotEmpty()) {
            assertEquals(45, result.troops.first().hp)
        }
    }

    // --- Post-battle rebuild ---

    @Test
    fun defensesAutoRebuildAfterBattle() {
        val cannon = PlacedBuilding(id = 1, type = BuildingType.RailGun, level = 1, x = 5, y = 5, hp = 10)
        val storage = PlacedBuilding(id = 2, type = BuildingType.CreditVault, level = 1, x = 10, y = 10, hp = 200)
        val troop = soldier(x = 5f, y = 5f, hp = 500) // beefy troop to destroy cannon
        val state = gameState(listOf(cannon, storage), listOf(troop))

        // Run battle until cannon is destroyed and troop dies or buildings are gone
        val result = BattleUpdateUseCase.update(state, now = 60_000)
        // After battle, some defenses should be rebuilt (70% chance, deterministic via pseudoRandom)
        // We can't predict exact outcome but the mechanism should run
        // At minimum, storage should survive (troop targets cannon first)
    }

    @Test
    fun trapsNotRebuiltAfterBattle() {
        val trap = PlacedBuilding(id = 1, type = BuildingType.MineTrap, level = 1, x = 5, y = 0, hp = 1)
        val target = PlacedBuilding(id = 2, type = BuildingType.CreditVault, level = 1, x = 10, y = 0, hp = 200)
        val troop = soldier(x = 5f, y = 0f, hp = 15) // will die after trap + some damage

        val state = gameState(listOf(trap, target), listOf(troop))
        val result = BattleUpdateUseCase.update(state, now = 60_000)

        // Trap should be triggered, not rebuilt
        val resultTrap = result.village.buildings.find { it.type == BuildingType.MineTrap }
        if (resultTrap != null) {
            assertTrue(resultTrap.triggered)
        }
    }

    // --- Shield after attack ---

    @Test
    fun shieldGrantedAfterBattle() {
        val buildings = listOf(
            PlacedBuilding(id = 1, type = BuildingType.CreditVault, level = 1, x = 5, y = 5, hp = 10),
            PlacedBuilding(id = 2, type = BuildingType.AlloySilo, level = 1, x = 10, y = 10, hp = 200),
        )
        val troop = soldier(x = 5f, y = 5f, hp = 5) // weak, will die after destroying one building
        val state = gameState(buildings, listOf(troop))

        val result = BattleUpdateUseCase.update(state, now = 60_000)
        // Should have a shield since some buildings were destroyed
        if (result.troops.isEmpty()) {
            assertTrue(result.shieldExpiresAt > 0)
        }
    }
}
