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
    fun droneTargetsResourceBuildings() {
        val storage = building(1, BuildingType.CreditVault, x = 15, y = 5)
        val railGun = building(2, BuildingType.RailGun, x = 5, y = 5)
        val drone = troop(100, TroopType.Drone, x = 10f, y = 5.5f)
        val result = battle(listOf(storage, railGun), listOf(drone), 100)
        val moved = result.troops.first()
        assertEquals(storage.id, moved.targetId)
    }

    @Test
    fun juggernautTargetsDefenses() {
        val storage = building(1, BuildingType.CreditVault, x = 5, y = 5)
        val railGun = building(2, BuildingType.RailGun, x = 15, y = 5)
        val juggernaut = troop(100, TroopType.Juggernaut, x = 10f, y = 5.5f)
        val result = battle(listOf(storage, railGun), listOf(juggernaut), 100)
        val moved = result.troops.first()
        assertEquals(railGun.id, moved.targetId)
    }

    @Test
    fun engineerTargetsBarriersFirst() {
        val storage = building(1, BuildingType.CreditVault, x = 5, y = 5)
        val barrier = building(2, BuildingType.Barrier, x = 15, y = 5)
        val engineer = troop(100, TroopType.Engineer, x = 10f, y = 5.5f)
        val result = battle(listOf(storage, barrier), listOf(engineer), 100)
        val moved = result.troops.first()
        assertEquals(barrier.id, moved.targetId)
    }

    @Test
    fun engineerTargetsDefenseWhenNoBarriers() {
        val storage = building(1, BuildingType.CreditVault, x = 5, y = 5)
        val railGun = building(2, BuildingType.RailGun, x = 15, y = 5)
        val engineer = troop(100, TroopType.Engineer, x = 10f, y = 5.5f)
        val result = battle(listOf(storage, railGun), listOf(engineer), 100)
        val moved = result.troops.first()
        assertEquals(railGun.id, moved.targetId)
    }

    @Test
    fun droneFallsBackToNearestWhenNoResources() {
        val railGun = building(1, BuildingType.RailGun, x = 5, y = 5)
        val barrier = building(2, BuildingType.Barrier, x = 15, y = 5)
        val drone = troop(100, TroopType.Drone, x = 10f, y = 5.5f)
        val result = battle(listOf(railGun, barrier), listOf(drone), 100)
        val moved = result.troops.first()
        assertEquals(railGun.id, moved.targetId)
    }

    // --- Wall Breaker mechanics ---

    @Test
    fun engineerDealsExtraDamageToBarriers() {
        val barrier = building(1, BuildingType.Barrier, x = 5, y = 5, hp = 500)
        val storage = building(2, BuildingType.CreditVault, x = 20, y = 20, hp = 200)
        val engineer = troop(100, TroopType.Engineer, x = 5.5f, y = 5.5f, hp = 1000)

        val result = battle(listOf(barrier, storage), listOf(engineer), 10_000)
        val damagedBarrier = result.village.buildings.find { it.id == 1L }
        assertNull(damagedBarrier, "Barrier should be destroyed by engineer")
    }

    @Test
    fun engineerNormalDamageToNonBarriers() {
        val storage = building(1, BuildingType.CreditVault, x = 5, y = 5, hp = 200)
        val engineer = troop(100, TroopType.Engineer, x = 5.5f, y = 4.5f, hp = 1000)
        val result = battle(listOf(storage), listOf(engineer), 5_000)
        val damaged = result.village.buildings.find { it.id == 1L }
        assertNotNull(damaged)
        assertTrue(damaged.hp in 120..160, "Normal damage (no barrier multiplier) expected, got ${damaged.hp}")
    }

    // --- New troop types ---

    @Test
    fun spectreHasRangedSplash() {
        val config = TroopConfig.configFor(TroopType.Spectre, 1)!!
        assertTrue(config.range >= 2f, "Spectre should have ranged attack")
        assertTrue(config.splashRadius > 0f, "Spectre should have splash damage")
        assertTrue(config.dps >= 40, "Spectre should be a glass cannon")
    }

    @Test
    fun gunshipIsTankyWithSplash() {
        val config = TroopConfig.configFor(TroopType.Gunship, 1)!!
        assertTrue(config.hp >= 400, "Gunship should be very tanky")
        assertTrue(config.splashRadius > 0f, "Gunship should have splash damage")
        assertTrue(config.range >= 2f, "Gunship should have ranged attack")
    }

    @Test
    fun droneIsFast() {
        val config = TroopConfig.configFor(TroopType.Drone, 1)!!
        val marineConfig = TroopConfig.configFor(TroopType.Marine, 1)!!
        assertTrue(config.speed > marineConfig.speed, "Drone should be faster than marine")
        assertTrue(config.speed >= 1.5f, "Drone should be fast")
    }

    // --- New defenses ---

    @Test
    fun teslaTowerHasSplash() {
        val config = BuildingConfig.configFor(BuildingType.TeslaTower, 1)!!
        assertTrue(config.splashRadius > 0f, "TeslaTower should have splash damage")
        assertTrue(config.damage > 0, "TeslaTower should deal damage")
        assertTrue(config.range > 2f, "TeslaTower should have decent range")
    }

    @Test
    fun teslaTowerDamagesMultipleTroops() {
        val tower = building(1, BuildingType.TeslaTower, x = 10, y = 5)
        val t1 = troop(100, TroopType.Marine, x = 8f, y = 5.5f)
        val t2 = troop(101, TroopType.Marine, x = 8.5f, y = 5.5f)
        val result = battle(listOf(tower), listOf(t1, t2), 2_000)
        for (t in result.troops) {
            assertTrue(t.hp < 45, "Troop ${t.id} should be damaged by TeslaTower splash")
        }
    }

    // --- NovaBomb trap ---

    @Test
    fun novaBombDealsMassiveDamage() {
        val trap = building(1, BuildingType.NovaBomb, x = 5, y = 5, hp = 1)
        val target = building(2, BuildingType.CreditVault, x = 10, y = 5)
        val t1 = troop(100, TroopType.Marine, x = 5f, y = 5f)
        val t2 = troop(101, TroopType.Marine, x = 5.5f, y = 5.5f)
        val result = battle(listOf(trap, target), listOf(t1, t2), 100)
        assertTrue(result.troops.isEmpty(), "NovaBomb should kill both marines (100 burst damage vs 45 hp)")
        val triggeredTrap = result.village.buildings.find { it.type == BuildingType.NovaBomb }
        assertTrue(triggeredTrap?.triggered == true)
    }

    @Test
    fun novaBombLargerRadiusThanMineTrap() {
        val novaBombConfig = BuildingConfig.configFor(BuildingType.NovaBomb, 1)!!
        val mineTrapConfig = BuildingConfig.configFor(BuildingType.MineTrap, 1)!!
        assertTrue(novaBombConfig.triggerRadius > mineTrapConfig.triggerRadius)
        assertTrue(novaBombConfig.burstDamage > mineTrapConfig.burstDamage)
    }

    // --- GravityTrap ---

    @Test
    fun gravityTrapKillsDrone() {
        val trap = building(1, BuildingType.GravityTrap, x = 5, y = 5, hp = 1)
        val target = building(2, BuildingType.CreditVault, x = 10, y = 5)
        val drone = troop(100, TroopType.Drone, x = 5f, y = 5f)
        val result = battle(listOf(trap, target), listOf(drone), 100)
        assertTrue(result.troops.isEmpty(), "GravityTrap should remove Drone")
    }

    @Test
    fun gravityTrapDoesNotKillGunship() {
        val trap = building(1, BuildingType.GravityTrap, x = 5, y = 5, hp = 1)
        val target = building(2, BuildingType.CreditVault, x = 10, y = 5)
        val gunship = troop(100, TroopType.Gunship, x = 5f, y = 5f)
        val result = battle(listOf(trap, target), listOf(gunship), 100)
        assertTrue(result.troops.isNotEmpty(), "GravityTrap should not kill heavy Gunship")
    }

    // --- Battle endings ---

    @Test
    fun battleEndsWhenAllBuildingsDestroyed() {
        val storage = building(1, BuildingType.CreditVault, x = 5, y = 5, hp = 10)
        val marine = troop(100, TroopType.Marine, x = 5.5f, y = 4.5f, hp = 1000)
        val result = battle(listOf(storage), listOf(marine), 5_000)
        assertTrue(result.troops.isEmpty(), "Troops should be cleared when all buildings destroyed")
    }

    @Test
    fun battleEndsWhenAllTroopsDie() {
        val railGun = building(1, BuildingType.RailGun, x = 5, y = 5)
        val marine = troop(100, TroopType.Marine, x = 5.5f, y = 4.5f, hp = 5)
        val result = battle(listOf(railGun), listOf(marine), 5_000)
        assertTrue(result.troops.isEmpty(), "Troops should all be dead")
    }

    // --- Mixed army scenarios ---

    @Test
    fun mixedArmyWithTargeting() {
        val storage = building(1, BuildingType.CreditVault, x = 10, y = 10, hp = 200)
        val railGun = building(2, BuildingType.RailGun, x = 20, y = 10)
        val barrier = building(3, BuildingType.Barrier, x = 15, y = 10)

        val marine = troop(100, TroopType.Marine, x = 5f, y = 10.5f)
        val juggernaut = troop(101, TroopType.Juggernaut, x = 5f, y = 11.5f)
        val engineer = troop(102, TroopType.Engineer, x = 5f, y = 12.5f)

        val result = battle(listOf(storage, railGun, barrier), listOf(marine, juggernaut, engineer), 100)

        val marineTarget = result.troops.find { it.id == 100L }?.targetId
        val juggernautTarget = result.troops.find { it.id == 101L }?.targetId
        val engineerTarget = result.troops.find { it.id == 102L }?.targetId

        assertEquals(storage.id, marineTarget, "Marine should target nearest (storage)")
        assertTrue(juggernautTarget == railGun.id || juggernautTarget == barrier.id, "Juggernaut should target a defense")
        assertEquals(barrier.id, engineerTarget, "Engineer should target barrier first")
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
        for (type in listOf(BuildingType.RailGun, BuildingType.LaserTurret, BuildingType.MissileBattery, BuildingType.TeslaTower)) {
            val config = BuildingConfig.configFor(type, 1)
            assertNotNull(config, "$type should have a level 1 config")
            assertTrue(config.damage > 0, "$type should deal damage")
            assertTrue(config.range > 0, "$type should have range")
        }
    }

    @Test
    fun allTrapTypesHaveEffect() {
        for (type in listOf(BuildingType.MineTrap, BuildingType.NovaBomb)) {
            val config = BuildingConfig.configFor(type, 1)
            assertNotNull(config, "$type should have a config")
            assertTrue(config.burstDamage > 0, "$type should have burst damage")
            assertTrue(config.triggerRadius > 0, "$type should have trigger radius")
        }
        val gravityConfig = BuildingConfig.configFor(BuildingType.GravityTrap, 1)
        assertNotNull(gravityConfig, "GravityTrap should have a config")
    }
}
