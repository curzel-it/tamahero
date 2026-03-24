package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class BuildingLevelConfig(
    val level: Int,
    val hp: Int,
    val cost: Resources,
    val buildTimeSeconds: Long,
    val productionPerHour: Resources = Resources(),
    val storageCapacity: Resources = Resources(),
    val width: Int = 2,
    val height: Int = 2,
    val requiredTownHallLevel: Int = 1,
    val damage: Int = 0,
    val range: Float = 0f,
    val attackSpeedMs: Long = 0,
    val troopCapacity: Int = 0,
    val burstDamage: Int = 0,
    val triggerRadius: Float = 0f,
    val minRange: Float = 0f,
    val shieldHp: Int = 0,
    val splashRadius: Float = 0f,
)

object BuildingConfig {
    private val configs: Map<BuildingType, List<BuildingLevelConfig>> = mapOf(
        BuildingType.CommandCenter to listOf(
            BuildingLevelConfig(level = 1, hp = 1000, cost = Resources(), buildTimeSeconds = 0, width = 4, height = 4, requiredTownHallLevel = 0),
            BuildingLevelConfig(level = 2, hp = 1500, cost = Resources(credits = 500, alloy = 500), buildTimeSeconds = 60, width = 4, height = 4, requiredTownHallLevel = 1),
            BuildingLevelConfig(level = 3, hp = 2000, cost = Resources(credits = 1500, alloy = 1500, crystal = 500), buildTimeSeconds = 300, width = 4, height = 4, requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 4, hp = 2800, cost = Resources(credits = 4000, alloy = 4000, crystal = 1500), buildTimeSeconds = 900, width = 4, height = 4, requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 5, hp = 3800, cost = Resources(credits = 10000, alloy = 10000, crystal = 4000, plasma = 500), buildTimeSeconds = 2700, width = 4, height = 4, requiredTownHallLevel = 4),
            BuildingLevelConfig(level = 6, hp = 5000, cost = Resources(credits = 25000, alloy = 25000, crystal = 10000, plasma = 1500), buildTimeSeconds = 7200, width = 4, height = 4, requiredTownHallLevel = 5),
            BuildingLevelConfig(level = 7, hp = 6500, cost = Resources(credits = 60000, alloy = 60000, crystal = 25000, plasma = 4000), buildTimeSeconds = 18000, width = 4, height = 4, requiredTownHallLevel = 6),
            BuildingLevelConfig(level = 8, hp = 8500, cost = Resources(credits = 150000, alloy = 150000, crystal = 60000, plasma = 10000), buildTimeSeconds = 43200, width = 4, height = 4, requiredTownHallLevel = 7),
        ),
        BuildingType.AlloyRefinery to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 50), buildTimeSeconds = 10, productionPerHour = Resources(alloy = 100)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 150), buildTimeSeconds = 30, productionPerHour = Resources(alloy = 200)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 400), buildTimeSeconds = 120, productionPerHour = Resources(alloy = 350)),
            BuildingLevelConfig(level = 4, hp = 550, cost = Resources(credits = 1000), buildTimeSeconds = 300, productionPerHour = Resources(alloy = 550), requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 5, hp = 750, cost = Resources(credits = 2500), buildTimeSeconds = 900, productionPerHour = Resources(alloy = 800), requiredTownHallLevel = 5),
            BuildingLevelConfig(level = 6, hp = 1000, cost = Resources(credits = 6000), buildTimeSeconds = 2700, productionPerHour = Resources(alloy = 1200), requiredTownHallLevel = 7),
        ),
        BuildingType.CreditMint to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(alloy = 50), buildTimeSeconds = 10, productionPerHour = Resources(credits = 100)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(alloy = 150), buildTimeSeconds = 30, productionPerHour = Resources(credits = 200)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(alloy = 400), buildTimeSeconds = 120, productionPerHour = Resources(credits = 350)),
            BuildingLevelConfig(level = 4, hp = 550, cost = Resources(alloy = 1000), buildTimeSeconds = 300, productionPerHour = Resources(credits = 550), requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 5, hp = 750, cost = Resources(alloy = 2500), buildTimeSeconds = 900, productionPerHour = Resources(credits = 800), requiredTownHallLevel = 5),
            BuildingLevelConfig(level = 6, hp = 1000, cost = Resources(alloy = 6000), buildTimeSeconds = 2700, productionPerHour = Resources(credits = 1200), requiredTownHallLevel = 7),
        ),
        BuildingType.Foundry to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 200, alloy = 200), buildTimeSeconds = 60, productionPerHour = Resources(crystal = 50), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 500, alloy = 500), buildTimeSeconds = 180, productionPerHour = Resources(crystal = 100), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 1200, alloy = 1200), buildTimeSeconds = 600, productionPerHour = Resources(crystal = 175), requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 4, hp = 550, cost = Resources(credits = 3000, alloy = 3000), buildTimeSeconds = 1800, productionPerHour = Resources(crystal = 275), requiredTownHallLevel = 5),
            BuildingLevelConfig(level = 5, hp = 750, cost = Resources(credits = 7000, alloy = 7000), buildTimeSeconds = 5400, productionPerHour = Resources(crystal = 400), requiredTownHallLevel = 7),
        ),
        BuildingType.AlloySilo to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 50), buildTimeSeconds = 10, storageCapacity = Resources(alloy = 1000)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 150), buildTimeSeconds = 30, storageCapacity = Resources(alloy = 2500)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 400), buildTimeSeconds = 120, storageCapacity = Resources(alloy = 5000)),
            BuildingLevelConfig(level = 4, hp = 550, cost = Resources(credits = 1000), buildTimeSeconds = 300, storageCapacity = Resources(alloy = 10000), requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 5, hp = 750, cost = Resources(credits = 2500), buildTimeSeconds = 900, storageCapacity = Resources(alloy = 25000), requiredTownHallLevel = 5),
            BuildingLevelConfig(level = 6, hp = 1000, cost = Resources(credits = 6000), buildTimeSeconds = 2700, storageCapacity = Resources(alloy = 60000), requiredTownHallLevel = 7),
        ),
        BuildingType.CreditVault to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(alloy = 50), buildTimeSeconds = 10, storageCapacity = Resources(credits = 1000)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(alloy = 150), buildTimeSeconds = 30, storageCapacity = Resources(credits = 2500)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(alloy = 400), buildTimeSeconds = 120, storageCapacity = Resources(credits = 5000)),
            BuildingLevelConfig(level = 4, hp = 550, cost = Resources(alloy = 1000), buildTimeSeconds = 300, storageCapacity = Resources(credits = 10000), requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 5, hp = 750, cost = Resources(alloy = 2500), buildTimeSeconds = 900, storageCapacity = Resources(credits = 25000), requiredTownHallLevel = 5),
            BuildingLevelConfig(level = 6, hp = 1000, cost = Resources(alloy = 6000), buildTimeSeconds = 2700, storageCapacity = Resources(credits = 60000), requiredTownHallLevel = 7),
        ),
        BuildingType.CrystalSilo to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 100, alloy = 100), buildTimeSeconds = 30, storageCapacity = Resources(crystal = 500), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 300, alloy = 300), buildTimeSeconds = 120, storageCapacity = Resources(crystal = 1500), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 800, alloy = 800), buildTimeSeconds = 300, storageCapacity = Resources(crystal = 3000), requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 4, hp = 550, cost = Resources(credits = 2000, alloy = 2000), buildTimeSeconds = 900, storageCapacity = Resources(crystal = 7000), requiredTownHallLevel = 5),
            BuildingLevelConfig(level = 5, hp = 750, cost = Resources(credits = 5000, alloy = 5000), buildTimeSeconds = 2700, storageCapacity = Resources(crystal = 15000), requiredTownHallLevel = 7),
        ),
        BuildingType.Academy to listOf(
            BuildingLevelConfig(level = 1, hp = 300, cost = Resources(credits = 100, alloy = 100), buildTimeSeconds = 30, width = 4, height = 2, requiredTownHallLevel = 1),
            BuildingLevelConfig(level = 2, hp = 450, cost = Resources(credits = 300, alloy = 300), buildTimeSeconds = 120, width = 4, height = 2, requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 3, hp = 600, cost = Resources(credits = 800, alloy = 800, crystal = 200), buildTimeSeconds = 600, width = 4, height = 2, requiredTownHallLevel = 4),
            BuildingLevelConfig(level = 4, hp = 800, cost = Resources(credits = 2000, alloy = 2000, crystal = 500), buildTimeSeconds = 1800, width = 4, height = 2, requiredTownHallLevel = 6),
        ),
        BuildingType.Hangar to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 100, alloy = 100), buildTimeSeconds = 30, width = 4, height = 2, requiredTownHallLevel = 1, troopCapacity = 20),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 300, alloy = 300), buildTimeSeconds = 120, width = 4, height = 2, requiredTownHallLevel = 2, troopCapacity = 30),
            BuildingLevelConfig(level = 3, hp = 450, cost = Resources(credits = 800, alloy = 800), buildTimeSeconds = 300, width = 4, height = 2, requiredTownHallLevel = 3, troopCapacity = 45),
            BuildingLevelConfig(level = 4, hp = 600, cost = Resources(credits = 2000, alloy = 2000, crystal = 300), buildTimeSeconds = 900, width = 4, height = 2, requiredTownHallLevel = 5, troopCapacity = 60),
            BuildingLevelConfig(level = 5, hp = 800, cost = Resources(credits = 5000, alloy = 5000, crystal = 800), buildTimeSeconds = 2700, width = 4, height = 2, requiredTownHallLevel = 7, troopCapacity = 80),
        ),
        BuildingType.RailGun to listOf(
            BuildingLevelConfig(level = 1, hp = 300, cost = Resources(credits = 200), buildTimeSeconds = 30, damage = 10, range = 3f, attackSpeedMs = 1000),
            BuildingLevelConfig(level = 2, hp = 450, cost = Resources(credits = 500), buildTimeSeconds = 120, damage = 15, range = 3.5f, attackSpeedMs = 1000),
            BuildingLevelConfig(level = 3, hp = 630, cost = Resources(credits = 1200, crystal = 200), buildTimeSeconds = 300, damage = 20, range = 4f, attackSpeedMs = 1000, requiredTownHallLevel = 4),
            BuildingLevelConfig(level = 4, hp = 880, cost = Resources(credits = 3000, crystal = 500), buildTimeSeconds = 900, damage = 27, range = 4.5f, attackSpeedMs = 1000, requiredTownHallLevel = 6),
        ),
        BuildingType.LaserTurret to listOf(
            BuildingLevelConfig(level = 1, hp = 250, cost = Resources(credits = 200, alloy = 100), buildTimeSeconds = 30, damage = 7, range = 4f, attackSpeedMs = 800),
            BuildingLevelConfig(level = 2, hp = 375, cost = Resources(credits = 500, alloy = 250), buildTimeSeconds = 120, damage = 11, range = 4.5f, attackSpeedMs = 800),
            BuildingLevelConfig(level = 3, hp = 525, cost = Resources(credits = 1200, alloy = 600, crystal = 200), buildTimeSeconds = 300, damage = 15, range = 5f, attackSpeedMs = 800, requiredTownHallLevel = 4),
            BuildingLevelConfig(level = 4, hp = 735, cost = Resources(credits = 3000, alloy = 1500, crystal = 500), buildTimeSeconds = 900, damage = 20, range = 5.5f, attackSpeedMs = 800, requiredTownHallLevel = 6),
        ),
        BuildingType.Barrier to listOf(
            BuildingLevelConfig(level = 1, hp = 500, cost = Resources(alloy = 20), buildTimeSeconds = 5, width = 1, height = 1),
            BuildingLevelConfig(level = 2, hp = 1000, cost = Resources(alloy = 50, crystal = 20), buildTimeSeconds = 15, width = 1, height = 1),
            BuildingLevelConfig(level = 3, hp = 2000, cost = Resources(alloy = 100, crystal = 50), buildTimeSeconds = 30, width = 1, height = 1, requiredTownHallLevel = 4),
        ),
        BuildingType.MissileBattery to listOf(
            BuildingLevelConfig(level = 1, hp = 250, cost = Resources(credits = 300, alloy = 100), buildTimeSeconds = 60, damage = 15, range = 5f, minRange = 2f, attackSpeedMs = 2000, splashRadius = 1.5f),
            BuildingLevelConfig(level = 2, hp = 375, cost = Resources(credits = 600, alloy = 200), buildTimeSeconds = 180, damage = 22, range = 5.5f, minRange = 2f, attackSpeedMs = 2000, splashRadius = 1.5f),
            BuildingLevelConfig(level = 3, hp = 525, cost = Resources(credits = 1500, alloy = 500, crystal = 300), buildTimeSeconds = 600, damage = 30, range = 6f, minRange = 2f, attackSpeedMs = 2000, splashRadius = 1.5f, requiredTownHallLevel = 4),
            BuildingLevelConfig(level = 4, hp = 735, cost = Resources(credits = 4000, alloy = 1200, crystal = 800), buildTimeSeconds = 1800, damage = 40, range = 6.5f, minRange = 2f, attackSpeedMs = 2000, splashRadius = 2f, requiredTownHallLevel = 6),
        ),
        BuildingType.TeslaTower to listOf(
            BuildingLevelConfig(level = 1, hp = 350, cost = Resources(credits = 400, alloy = 200), buildTimeSeconds = 60, damage = 11, range = 3.5f, attackSpeedMs = 1500, splashRadius = 1f),
            BuildingLevelConfig(level = 2, hp = 500, cost = Resources(credits = 800, alloy = 400), buildTimeSeconds = 180, damage = 16, range = 3.5f, attackSpeedMs = 1500, splashRadius = 1f),
            BuildingLevelConfig(level = 3, hp = 700, cost = Resources(credits = 2000, alloy = 1000, crystal = 400), buildTimeSeconds = 600, damage = 22, range = 4f, attackSpeedMs = 1500, splashRadius = 1.5f, requiredTownHallLevel = 4),
            BuildingLevelConfig(level = 4, hp = 980, cost = Resources(credits = 5000, alloy = 2500, crystal = 1000), buildTimeSeconds = 1800, damage = 30, range = 4f, attackSpeedMs = 1500, splashRadius = 1.5f, requiredTownHallLevel = 6),
        ),
        BuildingType.MineTrap to listOf(
            BuildingLevelConfig(level = 1, hp = 1, cost = Resources(alloy = 20), buildTimeSeconds = 5, width = 1, height = 1, burstDamage = 30, triggerRadius = 1f),
            BuildingLevelConfig(level = 2, hp = 1, cost = Resources(alloy = 40, crystal = 10), buildTimeSeconds = 10, width = 1, height = 1, burstDamage = 50, triggerRadius = 1.5f),
            BuildingLevelConfig(level = 3, hp = 1, cost = Resources(alloy = 80, crystal = 30), buildTimeSeconds = 15, width = 1, height = 1, burstDamage = 80, triggerRadius = 2f, requiredTownHallLevel = 4),
        ),
        BuildingType.GravityTrap to listOf(
            BuildingLevelConfig(level = 1, hp = 1, cost = Resources(alloy = 30, crystal = 10), buildTimeSeconds = 5, width = 1, height = 1, triggerRadius = 1f),
            BuildingLevelConfig(level = 2, hp = 1, cost = Resources(alloy = 60, crystal = 20), buildTimeSeconds = 10, width = 1, height = 1, triggerRadius = 1.5f),
            BuildingLevelConfig(level = 3, hp = 1, cost = Resources(alloy = 120, crystal = 50), buildTimeSeconds = 15, width = 1, height = 1, triggerRadius = 2f, requiredTownHallLevel = 5),
        ),
        BuildingType.NovaBomb to listOf(
            BuildingLevelConfig(level = 1, hp = 1, cost = Resources(credits = 100, crystal = 20), buildTimeSeconds = 10, width = 1, height = 1, burstDamage = 100, triggerRadius = 2f),
            BuildingLevelConfig(level = 2, hp = 1, cost = Resources(credits = 200, crystal = 40), buildTimeSeconds = 15, width = 1, height = 1, burstDamage = 150, triggerRadius = 2.5f),
            BuildingLevelConfig(level = 3, hp = 1, cost = Resources(credits = 400, crystal = 80), buildTimeSeconds = 20, width = 1, height = 1, burstDamage = 220, triggerRadius = 3f, requiredTownHallLevel = 5),
        ),
        BuildingType.ShieldDome to listOf(
            BuildingLevelConfig(level = 1, hp = 100, cost = Resources(credits = 500, alloy = 500, crystal = 200), buildTimeSeconds = 120, shieldHp = 500, requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 150, cost = Resources(credits = 1000, alloy = 1000, crystal = 500), buildTimeSeconds = 300, shieldHp = 1000, requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 3, hp = 200, cost = Resources(credits = 3000, alloy = 3000, crystal = 1500), buildTimeSeconds = 900, shieldHp = 2000, requiredTownHallLevel = 5),
        ),
        BuildingType.PlasmaReactor to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 300, alloy = 300), buildTimeSeconds = 60, productionPerHour = Resources(plasma = 50), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 600, alloy = 600), buildTimeSeconds = 180, productionPerHour = Resources(plasma = 100), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 1200, alloy = 1200), buildTimeSeconds = 300, productionPerHour = Resources(plasma = 175), requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 4, hp = 550, cost = Resources(credits = 3000, alloy = 3000), buildTimeSeconds = 900, productionPerHour = Resources(plasma = 275), requiredTownHallLevel = 5),
            BuildingLevelConfig(level = 5, hp = 750, cost = Resources(credits = 7000, alloy = 7000), buildTimeSeconds = 2700, productionPerHour = Resources(plasma = 400), requiredTownHallLevel = 7),
        ),
        BuildingType.PlasmaBank to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 200, alloy = 200), buildTimeSeconds = 30, storageCapacity = Resources(plasma = 500), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 500, alloy = 500), buildTimeSeconds = 120, storageCapacity = Resources(plasma = 1500), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 1200, alloy = 1200), buildTimeSeconds = 300, storageCapacity = Resources(plasma = 3000), requiredTownHallLevel = 3),
            BuildingLevelConfig(level = 4, hp = 550, cost = Resources(credits = 3000, alloy = 3000), buildTimeSeconds = 900, storageCapacity = Resources(plasma = 7000), requiredTownHallLevel = 5),
            BuildingLevelConfig(level = 5, hp = 750, cost = Resources(credits = 7000, alloy = 7000), buildTimeSeconds = 2700, storageCapacity = Resources(plasma = 15000), requiredTownHallLevel = 7),
        ),
        BuildingType.DroneStation to listOf(
            BuildingLevelConfig(level = 1, hp = 150, cost = Resources(credits = 200, alloy = 200), buildTimeSeconds = 30, width = 2, height = 2),
            BuildingLevelConfig(level = 2, hp = 250, cost = Resources(credits = 500, alloy = 500, crystal = 100), buildTimeSeconds = 120, width = 2, height = 2, requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 1200, alloy = 1200, crystal = 300), buildTimeSeconds = 300, width = 2, height = 2, requiredTownHallLevel = 4),
        ),
    )

    private val maxCounts: Map<BuildingType, Map<Int, Int>> = mapOf(
        BuildingType.AlloyRefinery to mapOf(1 to 2, 2 to 4, 3 to 6, 4 to 7, 5 to 8, 6 to 9, 7 to 10, 8 to 12),
        BuildingType.CreditMint to mapOf(1 to 2, 2 to 4, 3 to 6, 4 to 7, 5 to 8, 6 to 9, 7 to 10, 8 to 12),
        BuildingType.Foundry to mapOf(2 to 1, 3 to 2, 4 to 3, 5 to 4, 6 to 5, 7 to 6, 8 to 7),
        BuildingType.AlloySilo to mapOf(1 to 1, 2 to 2, 3 to 3, 4 to 4, 5 to 4, 6 to 5, 7 to 5, 8 to 6),
        BuildingType.CreditVault to mapOf(1 to 1, 2 to 2, 3 to 3, 4 to 4, 5 to 4, 6 to 5, 7 to 5, 8 to 6),
        BuildingType.CrystalSilo to mapOf(2 to 1, 3 to 2, 4 to 3, 5 to 3, 6 to 4, 7 to 4, 8 to 5),
        BuildingType.Academy to mapOf(1 to 1, 2 to 2, 3 to 2, 4 to 2, 5 to 3, 6 to 3, 7 to 3, 8 to 3),
        BuildingType.Hangar to mapOf(1 to 1, 2 to 2, 3 to 3, 4 to 3, 5 to 4, 6 to 4, 7 to 5, 8 to 5),
        BuildingType.RailGun to mapOf(1 to 2, 2 to 3, 3 to 5, 4 to 6, 5 to 7, 6 to 8, 7 to 9, 8 to 10),
        BuildingType.LaserTurret to mapOf(1 to 1, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9),
        BuildingType.MissileBattery to mapOf(1 to 0, 2 to 1, 3 to 2, 4 to 3, 5 to 4, 6 to 5, 7 to 6, 8 to 7),
        BuildingType.TeslaTower to mapOf(1 to 0, 2 to 1, 3 to 2, 4 to 3, 5 to 4, 6 to 5, 7 to 5, 8 to 6),
        BuildingType.MineTrap to mapOf(1 to 4, 2 to 6, 3 to 8, 4 to 10, 5 to 12, 6 to 14, 7 to 16, 8 to 18),
        BuildingType.GravityTrap to mapOf(1 to 2, 2 to 4, 3 to 6, 4 to 8, 5 to 10, 6 to 10, 7 to 12, 8 to 12),
        BuildingType.NovaBomb to mapOf(1 to 0, 2 to 2, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9),
        BuildingType.ShieldDome to mapOf(2 to 1, 3 to 1, 4 to 1, 5 to 2, 6 to 2, 7 to 2, 8 to 3),
        BuildingType.PlasmaReactor to mapOf(2 to 1, 3 to 2, 4 to 2, 5 to 3, 6 to 3, 7 to 4, 8 to 4),
        BuildingType.PlasmaBank to mapOf(2 to 1, 3 to 2, 4 to 2, 5 to 3, 6 to 3, 7 to 4, 8 to 4),
        BuildingType.DroneStation to mapOf(1 to 1, 2 to 2, 3 to 3, 4 to 3, 5 to 4, 6 to 4, 7 to 5, 8 to 5),
    )

    fun configFor(type: BuildingType, level: Int): BuildingLevelConfig? =
        configs[type]?.find { it.level == level }

    fun maxLevel(type: BuildingType): Int =
        configs[type]?.maxOf { it.level } ?: 0

    fun maxCount(type: BuildingType, townHallLevel: Int): Int? {
        val limits = maxCounts[type] ?: return null
        return limits.entries
            .filter { it.key <= townHallLevel }
            .maxByOrNull { it.key }
            ?.value
    }
}
