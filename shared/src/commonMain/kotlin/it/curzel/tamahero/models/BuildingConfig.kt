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
        BuildingType.TownHall to listOf(
            BuildingLevelConfig(level = 1, hp = 1000, cost = Resources(), buildTimeSeconds = 0, width = 4, height = 4, requiredTownHallLevel = 0),
            BuildingLevelConfig(level = 2, hp = 1500, cost = Resources(gold = 500, wood = 500), buildTimeSeconds = 60, width = 4, height = 4, requiredTownHallLevel = 1),
            BuildingLevelConfig(level = 3, hp = 2000, cost = Resources(gold = 1500, wood = 1500, metal = 500), buildTimeSeconds = 300, width = 4, height = 4, requiredTownHallLevel = 2),
        ),
        BuildingType.LumberCamp to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(gold = 50), buildTimeSeconds = 10, productionPerHour = Resources(wood = 100)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(gold = 150), buildTimeSeconds = 30, productionPerHour = Resources(wood = 200)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(gold = 400), buildTimeSeconds = 120, productionPerHour = Resources(wood = 350)),
        ),
        BuildingType.GoldMine to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(wood = 50), buildTimeSeconds = 10, productionPerHour = Resources(gold = 100)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(wood = 150), buildTimeSeconds = 30, productionPerHour = Resources(gold = 200)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(wood = 400), buildTimeSeconds = 120, productionPerHour = Resources(gold = 350)),
        ),
        BuildingType.Forge to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(gold = 200, wood = 200), buildTimeSeconds = 60, productionPerHour = Resources(metal = 50), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(gold = 500, wood = 500), buildTimeSeconds = 180, productionPerHour = Resources(metal = 100), requiredTownHallLevel = 2),
        ),
        BuildingType.WoodStorage to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(gold = 50), buildTimeSeconds = 10, storageCapacity = Resources(wood = 1000)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(gold = 150), buildTimeSeconds = 30, storageCapacity = Resources(wood = 2500)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(gold = 400), buildTimeSeconds = 120, storageCapacity = Resources(wood = 5000)),
        ),
        BuildingType.GoldStorage to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(wood = 50), buildTimeSeconds = 10, storageCapacity = Resources(gold = 1000)),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(wood = 150), buildTimeSeconds = 30, storageCapacity = Resources(gold = 2500)),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(wood = 400), buildTimeSeconds = 120, storageCapacity = Resources(gold = 5000)),
        ),
        BuildingType.MetalStorage to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(gold = 100, wood = 100), buildTimeSeconds = 30, storageCapacity = Resources(metal = 500), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(gold = 300, wood = 300), buildTimeSeconds = 120, storageCapacity = Resources(metal = 1500), requiredTownHallLevel = 2),
        ),
        BuildingType.Barracks to listOf(
            BuildingLevelConfig(level = 1, hp = 300, cost = Resources(gold = 100, wood = 100), buildTimeSeconds = 30, width = 4, height = 2, requiredTownHallLevel = 1),
            BuildingLevelConfig(level = 2, hp = 450, cost = Resources(gold = 300, wood = 300), buildTimeSeconds = 120, width = 4, height = 2, requiredTownHallLevel = 2),
        ),
        BuildingType.ArmyCamp to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(gold = 100, wood = 100), buildTimeSeconds = 30, width = 4, height = 2, requiredTownHallLevel = 1, troopCapacity = 20),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(gold = 300, wood = 300), buildTimeSeconds = 120, width = 4, height = 2, requiredTownHallLevel = 2, troopCapacity = 30),
        ),
        BuildingType.Cannon to listOf(
            BuildingLevelConfig(level = 1, hp = 300, cost = Resources(gold = 200), buildTimeSeconds = 30, damage = 10, range = 3f, attackSpeedMs = 1000),
            BuildingLevelConfig(level = 2, hp = 450, cost = Resources(gold = 500), buildTimeSeconds = 120, damage = 15, range = 3.5f, attackSpeedMs = 1000),
        ),
        BuildingType.ArcherTower to listOf(
            BuildingLevelConfig(level = 1, hp = 250, cost = Resources(gold = 200, wood = 100), buildTimeSeconds = 30, damage = 7, range = 4f, attackSpeedMs = 800),
            BuildingLevelConfig(level = 2, hp = 375, cost = Resources(gold = 500, wood = 250), buildTimeSeconds = 120, damage = 11, range = 4.5f, attackSpeedMs = 800),
        ),
        BuildingType.Wall to listOf(
            BuildingLevelConfig(level = 1, hp = 500, cost = Resources(wood = 20), buildTimeSeconds = 5, width = 1, height = 1),
            BuildingLevelConfig(level = 2, hp = 1000, cost = Resources(wood = 50, metal = 20), buildTimeSeconds = 15, width = 1, height = 1),
        ),
        BuildingType.Mortar to listOf(
            BuildingLevelConfig(level = 1, hp = 250, cost = Resources(gold = 300, wood = 100), buildTimeSeconds = 60, damage = 15, range = 5f, minRange = 2f, attackSpeedMs = 2000, splashRadius = 1.5f),
            BuildingLevelConfig(level = 2, hp = 375, cost = Resources(gold = 600, wood = 200), buildTimeSeconds = 180, damage = 22, range = 5.5f, minRange = 2f, attackSpeedMs = 2000, splashRadius = 1.5f),
        ),
        BuildingType.WizardTower to listOf(
            BuildingLevelConfig(level = 1, hp = 350, cost = Resources(gold = 400, wood = 200), buildTimeSeconds = 60, damage = 11, range = 3.5f, attackSpeedMs = 1500, splashRadius = 1f),
            BuildingLevelConfig(level = 2, hp = 500, cost = Resources(gold = 800, wood = 400), buildTimeSeconds = 180, damage = 16, range = 3.5f, attackSpeedMs = 1500, splashRadius = 1f),
        ),
        BuildingType.SpikeTrap to listOf(
            BuildingLevelConfig(level = 1, hp = 1, cost = Resources(wood = 20), buildTimeSeconds = 5, width = 1, height = 1, burstDamage = 30, triggerRadius = 1f),
            BuildingLevelConfig(level = 2, hp = 1, cost = Resources(wood = 40, metal = 10), buildTimeSeconds = 10, width = 1, height = 1, burstDamage = 50, triggerRadius = 1.5f),
        ),
        BuildingType.SpringTrap to listOf(
            BuildingLevelConfig(level = 1, hp = 1, cost = Resources(wood = 30, metal = 10), buildTimeSeconds = 5, width = 1, height = 1, triggerRadius = 1f),
            BuildingLevelConfig(level = 2, hp = 1, cost = Resources(wood = 60, metal = 20), buildTimeSeconds = 10, width = 1, height = 1, triggerRadius = 1.5f),
        ),
        BuildingType.GiantBomb to listOf(
            BuildingLevelConfig(level = 1, hp = 1, cost = Resources(gold = 100, metal = 20), buildTimeSeconds = 10, width = 1, height = 1, burstDamage = 100, triggerRadius = 2f),
            BuildingLevelConfig(level = 2, hp = 1, cost = Resources(gold = 200, metal = 40), buildTimeSeconds = 15, width = 1, height = 1, burstDamage = 150, triggerRadius = 2.5f),
        ),
        BuildingType.ShieldDome to listOf(
            BuildingLevelConfig(level = 1, hp = 100, cost = Resources(gold = 500, wood = 500, metal = 200), buildTimeSeconds = 120, shieldHp = 500, requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 150, cost = Resources(gold = 1000, wood = 1000, metal = 500), buildTimeSeconds = 300, shieldHp = 1000, requiredTownHallLevel = 3),
        ),
        BuildingType.ManaWell to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(gold = 300, wood = 300), buildTimeSeconds = 60, productionPerHour = Resources(mana = 50), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(gold = 600, wood = 600), buildTimeSeconds = 180, productionPerHour = Resources(mana = 100), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 3, hp = 400, cost = Resources(gold = 1200, wood = 1200), buildTimeSeconds = 300, productionPerHour = Resources(mana = 175), requiredTownHallLevel = 3),
        ),
        BuildingType.ManaStorage to listOf(
            BuildingLevelConfig(level = 1, hp = 200, cost = Resources(gold = 200, wood = 200), buildTimeSeconds = 30, storageCapacity = Resources(mana = 500), requiredTownHallLevel = 2),
            BuildingLevelConfig(level = 2, hp = 300, cost = Resources(gold = 500, wood = 500), buildTimeSeconds = 120, storageCapacity = Resources(mana = 1500), requiredTownHallLevel = 2),
        ),
        BuildingType.BuilderHut to listOf(
            BuildingLevelConfig(level = 1, hp = 150, cost = Resources(gold = 200, wood = 200), buildTimeSeconds = 30, width = 2, height = 2),
            BuildingLevelConfig(level = 2, hp = 250, cost = Resources(gold = 500, wood = 500, metal = 100), buildTimeSeconds = 120, width = 2, height = 2, requiredTownHallLevel = 2),
        ),
    )

    // Max count per building type, keyed by TownHall level
    // Missing entries or missing TH levels mean unlimited (e.g. Wall)
    private val maxCounts: Map<BuildingType, Map<Int, Int>> = mapOf(
        BuildingType.LumberCamp to mapOf(1 to 2, 2 to 4, 3 to 6),
        BuildingType.GoldMine to mapOf(1 to 2, 2 to 4, 3 to 6),
        BuildingType.Forge to mapOf(2 to 1, 3 to 2),
        BuildingType.WoodStorage to mapOf(1 to 1, 2 to 2, 3 to 3),
        BuildingType.GoldStorage to mapOf(1 to 1, 2 to 2, 3 to 3),
        BuildingType.MetalStorage to mapOf(2 to 1, 3 to 2),
        BuildingType.Barracks to mapOf(1 to 1, 2 to 2, 3 to 2),
        BuildingType.ArmyCamp to mapOf(1 to 1, 2 to 2, 3 to 3),
        BuildingType.Cannon to mapOf(1 to 2, 2 to 3, 3 to 5),
        BuildingType.ArcherTower to mapOf(1 to 1, 2 to 3, 3 to 4),
        BuildingType.Mortar to mapOf(1 to 0, 2 to 1, 3 to 2),
        BuildingType.WizardTower to mapOf(1 to 0, 2 to 1, 3 to 2),
        BuildingType.SpikeTrap to mapOf(1 to 4, 2 to 6, 3 to 8),
        BuildingType.SpringTrap to mapOf(1 to 2, 2 to 4, 3 to 6),
        BuildingType.GiantBomb to mapOf(1 to 0, 2 to 2, 3 to 4),
        BuildingType.ShieldDome to mapOf(2 to 1, 3 to 1),
        BuildingType.ManaWell to mapOf(2 to 1, 3 to 2),
        BuildingType.ManaStorage to mapOf(2 to 1, 3 to 2),
        BuildingType.BuilderHut to mapOf(1 to 1, 2 to 2, 3 to 3),
    )

    fun configFor(type: BuildingType, level: Int): BuildingLevelConfig? =
        configs[type]?.find { it.level == level }

    fun maxLevel(type: BuildingType): Int =
        configs[type]?.maxOf { it.level } ?: 0

    fun maxCount(type: BuildingType, townHallLevel: Int): Int? {
        val limits = maxCounts[type] ?: return null
        // Find the limit for the highest TH level <= townHallLevel
        return limits.entries
            .filter { it.key <= townHallLevel }
            .maxByOrNull { it.key }
            ?.value
    }
}
