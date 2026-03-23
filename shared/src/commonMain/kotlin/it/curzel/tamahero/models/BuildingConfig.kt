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
        ),
        BuildingType.AlloyRefinery to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 50), buildTimeSeconds = 10, productionPerHour = Resources(alloy = 100)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 150), buildTimeSeconds = 30, productionPerHour = Resources(alloy = 200)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 400), buildTimeSeconds = 120, productionPerHour = Resources(alloy = 350)),
        ),
        BuildingType.CreditMint to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(alloy = 50), buildTimeSeconds = 10, productionPerHour = Resources(credits = 100)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(alloy = 150), buildTimeSeconds = 30, productionPerHour = Resources(credits = 200)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(alloy = 400), buildTimeSeconds = 120, productionPerHour = Resources(credits = 350)),
        ),
        BuildingType.Foundry to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 200, alloy = 200), buildTimeSeconds = 60, productionPerHour = Resources(crystal = 50), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 500, alloy = 500), buildTimeSeconds = 180, productionPerHour = Resources(crystal = 100), requiredTownHallLevel = 2),
        ),
        BuildingType.AlloySilo to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 50), buildTimeSeconds = 10, storageCapacity = Resources(alloy = 1000)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 150), buildTimeSeconds = 30, storageCapacity = Resources(alloy = 2500)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 400), buildTimeSeconds = 120, storageCapacity = Resources(alloy = 5000)),
        ),
        BuildingType.CreditVault to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(alloy = 50), buildTimeSeconds = 10, storageCapacity = Resources(credits = 1000)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(alloy = 150), buildTimeSeconds = 30, storageCapacity = Resources(credits = 2500)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(alloy = 400), buildTimeSeconds = 120, storageCapacity = Resources(credits = 5000)),
        ),
        BuildingType.CrystalSilo to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 100, alloy = 100), buildTimeSeconds = 30, storageCapacity = Resources(crystal = 500), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 300, alloy = 300), buildTimeSeconds = 120, storageCapacity = Resources(crystal = 1500), requiredTownHallLevel = 2),
        ),
        BuildingType.Academy to listOf(
            BuildingLevelConfig(level = 1, hp = 300, cost = Resources(credits = 100, alloy = 100), buildTimeSeconds = 30, width = 4, height = 2, requiredTownHallLevel = 1),
            BuildingLevelConfig(level = 2, hp = 450, cost = Resources(credits = 300, alloy = 300), buildTimeSeconds = 120, width = 4, height = 2, requiredTownHallLevel = 2),
        ),
        BuildingType.Hangar to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 100, alloy = 100), buildTimeSeconds = 30, width = 4, height = 2, requiredTownHallLevel = 1, troopCapacity = 20),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 300, alloy = 300), buildTimeSeconds = 120, width = 4, height = 2, requiredTownHallLevel = 2, troopCapacity = 30),
        ),
        BuildingType.RailGun to listOf(
            BuildingLevelConfig(level = 1, hp = 300, cost = Resources(credits = 200), buildTimeSeconds = 30, damage = 10, range = 3f, attackSpeedMs = 1000),
            BuildingLevelConfig(level = 2, hp = 450, cost = Resources(credits = 500), buildTimeSeconds = 120, damage = 15, range = 3.5f, attackSpeedMs = 1000),
        ),
        BuildingType.LaserTurret to listOf(
            BuildingLevelConfig(level = 1, hp = 250, cost = Resources(credits = 200, alloy = 100), buildTimeSeconds = 30, damage = 7, range = 4f, attackSpeedMs = 800),
            BuildingLevelConfig(level = 2, hp = 375, cost = Resources(credits = 500, alloy = 250), buildTimeSeconds = 120, damage = 11, range = 4.5f, attackSpeedMs = 800),
        ),
        BuildingType.Barrier to listOf(
            BuildingLevelConfig(level = 1, hp = 500, cost = Resources(alloy = 20), buildTimeSeconds = 5, width = 1, height = 1),
            BuildingLevelConfig(level = 2, hp = 1000, cost = Resources(alloy = 50, crystal = 20), buildTimeSeconds = 15, width = 1, height = 1),
        ),
        BuildingType.MissileBattery to listOf(
            BuildingLevelConfig(level = 1, hp = 250, cost = Resources(credits = 300, alloy = 100), buildTimeSeconds = 60, damage = 15, range = 5f, minRange = 2f, attackSpeedMs = 2000, splashRadius = 1.5f),
            BuildingLevelConfig(level = 2, hp = 375, cost = Resources(credits = 600, alloy = 200), buildTimeSeconds = 180, damage = 22, range = 5.5f, minRange = 2f, attackSpeedMs = 2000, splashRadius = 1.5f),
        ),
        BuildingType.TeslaTower to listOf(
            BuildingLevelConfig(level = 1, hp = 350, cost = Resources(credits = 400, alloy = 200), buildTimeSeconds = 60, damage = 11, range = 3.5f, attackSpeedMs = 1500, splashRadius = 1f),
            BuildingLevelConfig(level = 2, hp = 500, cost = Resources(credits = 800, alloy = 400), buildTimeSeconds = 180, damage = 16, range = 3.5f, attackSpeedMs = 1500, splashRadius = 1f),
        ),
        BuildingType.MineTrap to listOf(
            BuildingLevelConfig(level = 1, hp = 1, cost = Resources(alloy = 20), buildTimeSeconds = 5, width = 1, height = 1, burstDamage = 30, triggerRadius = 1f),
            BuildingLevelConfig(level = 2, hp = 1, cost = Resources(alloy = 40, crystal = 10), buildTimeSeconds = 10, width = 1, height = 1, burstDamage = 50, triggerRadius = 1.5f),
        ),
        BuildingType.GravityTrap to listOf(
            BuildingLevelConfig(level = 1, hp = 1, cost = Resources(alloy = 30, crystal = 10), buildTimeSeconds = 5, width = 1, height = 1, triggerRadius = 1f),
            BuildingLevelConfig(level = 2, hp = 1, cost = Resources(alloy = 60, crystal = 20), buildTimeSeconds = 10, width = 1, height = 1, triggerRadius = 1.5f),
        ),
        BuildingType.NovaBomb to listOf(
            BuildingLevelConfig(level = 1, hp = 1, cost = Resources(credits = 100, crystal = 20), buildTimeSeconds = 10, width = 1, height = 1, burstDamage = 100, triggerRadius = 2f),
            BuildingLevelConfig(level = 2, hp = 1, cost = Resources(credits = 200, crystal = 40), buildTimeSeconds = 15, width = 1, height = 1, burstDamage = 150, triggerRadius = 2.5f),
        ),
        BuildingType.ShieldDome to listOf(
            BuildingLevelConfig(level = 1, hp = 100, cost = Resources(credits = 500, alloy = 500, crystal = 200), buildTimeSeconds = 120, shieldHp = 500, requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 150, cost = Resources(credits = 1000, alloy = 1000, crystal = 500), buildTimeSeconds = 300, shieldHp = 1000, requiredTownHallLevel = 3),
        ),
        BuildingType.PlasmaReactor to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 300, alloy = 300), buildTimeSeconds = 60, productionPerHour = Resources(plasma = 50), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 600, alloy = 600), buildTimeSeconds = 180, productionPerHour = Resources(plasma = 100), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(credits = 1200, alloy = 1200), buildTimeSeconds = 300, productionPerHour = Resources(plasma = 175), requiredTownHallLevel = 3),
        ),
        BuildingType.PlasmaBank to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(credits = 200, alloy = 200), buildTimeSeconds = 30, storageCapacity = Resources(plasma = 500), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(credits = 500, alloy = 500), buildTimeSeconds = 120, storageCapacity = Resources(plasma = 1500), requiredTownHallLevel = 2),
        ),
        BuildingType.DroneStation to listOf(
            BuildingLevelConfig(level = 1, hp = 150, cost = Resources(credits = 200, alloy = 200), buildTimeSeconds = 30, width = 2, height = 2),
            BuildingLevelConfig(level = 2, hp = 250, cost = Resources(credits = 500, alloy = 500, crystal = 100), buildTimeSeconds = 120, width = 2, height = 2, requiredTownHallLevel = 2),
        ),
    )

    private val maxCounts: Map<BuildingType, Map<Int, Int>> = mapOf(
        BuildingType.AlloyRefinery to mapOf(1 to 2, 2 to 4, 3 to 6),
        BuildingType.CreditMint to mapOf(1 to 2, 2 to 4, 3 to 6),
        BuildingType.Foundry to mapOf(2 to 1, 3 to 2),
        BuildingType.AlloySilo to mapOf(1 to 1, 2 to 2, 3 to 3),
        BuildingType.CreditVault to mapOf(1 to 1, 2 to 2, 3 to 3),
        BuildingType.CrystalSilo to mapOf(2 to 1, 3 to 2),
        BuildingType.Academy to mapOf(1 to 1, 2 to 2, 3 to 2),
        BuildingType.Hangar to mapOf(1 to 1, 2 to 2, 3 to 3),
        BuildingType.RailGun to mapOf(1 to 2, 2 to 3, 3 to 5),
        BuildingType.LaserTurret to mapOf(1 to 1, 2 to 3, 3 to 4),
        BuildingType.MissileBattery to mapOf(1 to 0, 2 to 1, 3 to 2),
        BuildingType.TeslaTower to mapOf(1 to 0, 2 to 1, 3 to 2),
        BuildingType.MineTrap to mapOf(1 to 4, 2 to 6, 3 to 8),
        BuildingType.GravityTrap to mapOf(1 to 2, 2 to 4, 3 to 6),
        BuildingType.NovaBomb to mapOf(1 to 0, 2 to 2, 3 to 4),
        BuildingType.ShieldDome to mapOf(2 to 1, 3 to 1),
        BuildingType.PlasmaReactor to mapOf(2 to 1, 3 to 2),
        BuildingType.PlasmaBank to mapOf(2 to 1, 3 to 2),
        BuildingType.DroneStation to mapOf(1 to 1, 2 to 2, 3 to 3),
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
