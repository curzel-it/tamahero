package it.curzel.tamahero.models

import kotlin.test.*

class CombatSystemTest {

    private fun building(id: Long, type: BuildingType, x: Int, y: Int, hp: Int? = null, level: Int = 1): PlacedBuilding {
        val config = BuildingConfig.configFor(type, level)
        return PlacedBuilding(id = id, type = type, level = level, x = x, y = y, hp = hp ?: config?.hp ?: 100)
    }

    private fun troop(id: Long, type: TroopType, x: Float, y: Float, hp: Int? = null, level: Int = 1): Troop {
        val config = TroopConfig.configFor(type, level)
        return Troop(id = id, type = type, level = level, hp = hp ?: config?.hp ?: 50, x = x, y = y)
    }

    private fun battle(buildings: List<PlacedBuilding>, troops: List<Troop>, durationMs: Long): GameState {
        val state = GameState(
            playerId = 1,
            village = Village(playerId = 1, buildings = buildings),
            troops = troops,
            lastUpdatedAt = 0,
        )
        return BattleUpdateUseCase.update(state, now = durationMs)
    }

    // --- Targeting AI ---

    @Test
    fun goblinTargetsResourceBuildings() {
        val storage = building(1, BuildingType.GoldStorage, x = 15, y = 5)
        val cannon = building(2, BuildingType.Cannon, x = 5, y = 5)
        val goblin = troop(100, TroopType.Goblin, x = 10f, y = 5.5f)
        val result = battle(listOf(storage, cannon), listOf(goblin), 100)
        val moved = result.troops.first()
        // Goblin should target GoldStorage (resource), not cannon (defense)
        // even though cannon is closer
        assertEquals(storage.id, moved.targetId)
    }

    @Test
    fun orcBerserkerTargetsDefenses() {
        val storage = building(1, BuildingType.GoldStorage, x = 5, y = 5)
        val cannon = building(2, BuildingType.Cannon, x = 15, y = 5)
        val orc = troop(100, TroopType.OrcBerserker, x = 10f, y = 5.5f)
        val result = battle(listOf(storage, cannon), listOf(orc), 100)
        val moved = result.troops.first()
        assertEquals(cannon.id, moved.targetId)
    }

    @Test
    fun dwarfSapperTargetsWallsFirst() {
        val storage = building(1, BuildingType.GoldStorage, x = 5, y = 5)
        val wall = building(2, BuildingType.Wall, x = 15, y = 5)
        val sapper = troop(100, TroopType.DwarfSapper, x = 10f, y = 5.5f)
        val result = battle(listOf(storage, wall), listOf(sapper), 100)
        val moved = result.troops.first()
        assertEquals(wall.id, moved.targetId)
    }

    @Test
    fun dwarfSapperTargetsDefenseWhenNoWalls() {
        val storage = building(1, BuildingType.GoldStorage, x = 5, y = 5)
        val cannon = building(2, BuildingType.Cannon, x = 15, y = 5)
        val sapper = troop(100, TroopType.DwarfSapper, x = 10f, y = 5.5f)
        val result = battle(listOf(storage, cannon), listOf(sapper), 100)
        val moved = result.troops.first()
        assertEquals(cannon.id, moved.targetId)
    }

    @Test
    fun goblinFallsBackToNearestWhenNoResources() {
        val cannon = building(1, BuildingType.Cannon, x = 5, y = 5)
        val wall = building(2, BuildingType.Wall, x = 15, y = 5)
        val goblin = troop(100, TroopType.Goblin, x = 10f, y = 5.5f)
        val result = battle(listOf(cannon, wall), listOf(goblin), 100)
        val moved = result.troops.first()
        // Should target cannon (nearest) since no resource buildings
        assertEquals(cannon.id, moved.targetId)
    }

    // --- Wall Breaker mechanics ---

    @Test
    fun dwarfSapperDealsExtraDamageToWalls() {
        // Wall is 1x1 at (5,5). Sapper ON the wall tile (distance=0, within range 0.5)
        val wall = building(1, BuildingType.Wall, x = 5, y = 5, hp = 500)
        // Need a second non-wall building so the battle doesn't end immediately when wall dies
        val storage = building(2, BuildingType.GoldStorage, x = 20, y = 20, hp = 200)
        val sapper = troop(100, TroopType.DwarfSapper, x = 5.5f, y = 5.5f, hp = 1000)

        val result = battle(listOf(wall, storage), listOf(sapper), 10_000)
        val damagedWall = result.village.buildings.find { it.id == 1L }
        assertNull(damagedWall, "Wall should be destroyed by wall breaker")
    }

    @Test
    fun dwarfSapperNormalDamageToNonWalls() {
        val storage = building(1, BuildingType.GoldStorage, x = 5, y = 5, hp = 200)
        val sapper = troop(100, TroopType.DwarfSapper, x = 5.5f, y = 4.5f, hp = 1000)
        val result = battle(listOf(storage), listOf(sapper), 5_000)
        val damaged = result.village.buildings.find { it.id == 1L }
        // Normal DPS (12) for 5 seconds = 60 damage. Storage: 200-60 = 140
        assertNotNull(damaged)
        assertTrue(damaged.hp in 120..160, "Normal damage (no wall multiplier) expected, got ${damaged.hp}")
    }

    // --- New troop types ---

    @Test
    fun wizardHasRangedSplash() {
        val config = TroopConfig.configFor(TroopType.Wizard, 1)!!
        assertTrue(config.range >= 2f, "Wizard should have ranged attack")
        assertTrue(config.splashRadius > 0f, "Wizard should have splash damage")
        assertTrue(config.dps >= 40, "Wizard should be a glass cannon")
    }

    @Test
    fun dragonIsTankyWithSplash() {
        val config = TroopConfig.configFor(TroopType.Dragon, 1)!!
        assertTrue(config.hp >= 400, "Dragon should be very tanky")
        assertTrue(config.splashRadius > 0f, "Dragon should have splash damage")
        assertTrue(config.range >= 2f, "Dragon should have ranged attack")
    }

    @Test
    fun goblinIsFast() {
        val config = TroopConfig.configFor(TroopType.Goblin, 1)!!
        val soldierConfig = TroopConfig.configFor(TroopType.HumanSoldier, 1)!!
        assertTrue(config.speed > soldierConfig.speed, "Goblin should be faster than soldier")
        assertTrue(config.speed >= 1.5f, "Goblin should be fast")
    }

    // --- New defenses ---

    @Test
    fun wizardTowerHasSplash() {
        val config = BuildingConfig.configFor(BuildingType.WizardTower, 1)!!
        assertTrue(config.splashRadius > 0f, "WizardTower should have splash damage")
        assertTrue(config.damage > 0, "WizardTower should deal damage")
        assertTrue(config.range > 2f, "WizardTower should have decent range")
    }

    @Test
    fun wizardTowerDamagesMultipleTroops() {
        val tower = building(1, BuildingType.WizardTower, x = 10, y = 5)
        // Two soldiers close together within WizardTower range
        val t1 = troop(100, TroopType.HumanSoldier, x = 8f, y = 5.5f)
        val t2 = troop(101, TroopType.HumanSoldier, x = 8.5f, y = 5.5f)
        val result = battle(listOf(tower), listOf(t1, t2), 2_000)
        for (t in result.troops) {
            assertTrue(t.hp < 45, "Troop ${t.id} should be damaged by WizardTower splash")
        }
    }

    // --- Giant Bomb trap ---

    @Test
    fun giantBombDealsMassiveDamage() {
        val trap = building(1, BuildingType.GiantBomb, x = 5, y = 5, hp = 1)
        val target = building(2, BuildingType.GoldStorage, x = 10, y = 5)
        val t1 = troop(100, TroopType.HumanSoldier, x = 5f, y = 5f)
        val t2 = troop(101, TroopType.HumanSoldier, x = 5.5f, y = 5.5f)
        val result = battle(listOf(trap, target), listOf(t1, t2), 100)
        // GiantBomb has burstDamage=100, triggerRadius=2.0
        // Soldiers have 45 hp — both should be dead
        assertTrue(result.troops.isEmpty(), "GiantBomb should kill both soldiers (100 burst damage vs 45 hp)")
        val triggeredTrap = result.village.buildings.find { it.type == BuildingType.GiantBomb }
        assertTrue(triggeredTrap?.triggered == true)
    }

    @Test
    fun giantBombLargerRadiusThanSpikeTrap() {
        val giantBombConfig = BuildingConfig.configFor(BuildingType.GiantBomb, 1)!!
        val spikeTrapConfig = BuildingConfig.configFor(BuildingType.SpikeTrap, 1)!!
        assertTrue(giantBombConfig.triggerRadius > spikeTrapConfig.triggerRadius)
        assertTrue(giantBombConfig.burstDamage > spikeTrapConfig.burstDamage)
    }

    // --- Spring trap expanded to include Goblin ---

    @Test
    fun springTrapKillsGoblin() {
        val trap = building(1, BuildingType.SpringTrap, x = 5, y = 5, hp = 1)
        val target = building(2, BuildingType.GoldStorage, x = 10, y = 5)
        val goblin = troop(100, TroopType.Goblin, x = 5f, y = 5f)
        val result = battle(listOf(trap, target), listOf(goblin), 100)
        assertTrue(result.troops.isEmpty(), "SpringTrap should remove Goblin")
    }

    @Test
    fun springTrapDoesNotKillDragon() {
        val trap = building(1, BuildingType.SpringTrap, x = 5, y = 5, hp = 1)
        val target = building(2, BuildingType.GoldStorage, x = 10, y = 5)
        val dragon = troop(100, TroopType.Dragon, x = 5f, y = 5f)
        val result = battle(listOf(trap, target), listOf(dragon), 100)
        assertTrue(result.troops.isNotEmpty(), "SpringTrap should not kill heavy Dragon")
    }

    // --- Battle endings ---

    @Test
    fun battleEndsWhenAllBuildingsDestroyed() {
        val storage = building(1, BuildingType.GoldStorage, x = 5, y = 5, hp = 10)
        val soldier = troop(100, TroopType.HumanSoldier, x = 5.5f, y = 4.5f, hp = 1000)
        val result = battle(listOf(storage), listOf(soldier), 5_000)
        assertTrue(result.troops.isEmpty(), "Troops should be cleared when all buildings destroyed")
    }

    @Test
    fun battleEndsWhenAllTroopsDie() {
        val cannon = building(1, BuildingType.Cannon, x = 5, y = 5)
        // Weak soldier within cannon range
        val soldier = troop(100, TroopType.HumanSoldier, x = 5.5f, y = 4.5f, hp = 5)
        val result = battle(listOf(cannon), listOf(soldier), 5_000)
        assertTrue(result.troops.isEmpty(), "Troops should all be dead")
    }

    // --- Mixed army scenarios ---

    @Test
    fun mixedArmyWithTargeting() {
        val storage = building(1, BuildingType.GoldStorage, x = 10, y = 10, hp = 200)
        val cannon = building(2, BuildingType.Cannon, x = 20, y = 10)
        val wall = building(3, BuildingType.Wall, x = 15, y = 10)

        val soldier = troop(100, TroopType.HumanSoldier, x = 5f, y = 10.5f)
        val orc = troop(101, TroopType.OrcBerserker, x = 5f, y = 11.5f)
        val sapper = troop(102, TroopType.DwarfSapper, x = 5f, y = 12.5f)

        val result = battle(listOf(storage, cannon, wall), listOf(soldier, orc, sapper), 100)

        val soldierTarget = result.troops.find { it.id == 100L }?.targetId
        val orcTarget = result.troops.find { it.id == 101L }?.targetId
        val sapperTarget = result.troops.find { it.id == 102L }?.targetId

        // Soldier: nearest (storage at distance ~5)
        assertEquals(storage.id, soldierTarget, "Soldier should target nearest (storage)")
        // Orc: targets defenses — nearest defense is wall (at ~10) or cannon (at ~15)
        assertTrue(orcTarget == cannon.id || orcTarget == wall.id, "OrcBerserker should target a defense")
        // Sapper: targets walls first
        assertEquals(wall.id, sapperTarget, "DwarfSapper should target wall first")
    }

    // --- Stat balance checks ---

    @Test
    fun allTroopTypesHaveLevel1Config() {
        for (type in TroopType.entries) {
            val config = TroopConfig.configFor(type, 1)
            assertNotNull(config, "$type should have a level 1 config")
            assertTrue(config.hp > 0, "$type hp should be positive")
            assertTrue(config.dps > 0, "$type dps should be positive")
            assertTrue(config.speed > 0, "$type speed should be positive")
            assertTrue(config.range > 0, "$type range should be positive")
        }
    }

    @Test
    fun allDefenseTypesHaveDamage() {
        for (type in listOf(BuildingType.Cannon, BuildingType.ArcherTower, BuildingType.Mortar, BuildingType.WizardTower)) {
            val config = BuildingConfig.configFor(type, 1)
            assertNotNull(config, "$type should have a level 1 config")
            assertTrue(config.damage > 0, "$type should deal damage")
            assertTrue(config.range > 0, "$type should have range")
        }
    }

    @Test
    fun allTrapTypesHaveEffect() {
        for (type in listOf(BuildingType.SpikeTrap, BuildingType.GiantBomb)) {
            val config = BuildingConfig.configFor(type, 1)
            assertNotNull(config, "$type should have a config")
            assertTrue(config.burstDamage > 0, "$type should have burst damage")
            assertTrue(config.triggerRadius > 0, "$type should have trigger radius")
        }
        val springConfig = BuildingConfig.configFor(BuildingType.SpringTrap, 1)
        assertNotNull(springConfig, "SpringTrap should have a config")
    }
}
