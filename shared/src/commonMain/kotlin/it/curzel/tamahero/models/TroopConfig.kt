package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class TroopLevelConfig(
    val level: Int,
    val hp: Int,
    val dps: Int,
    val speed: Float,
    val range: Float,
    val trainingCost: Resources,
    val trainingTimeSeconds: Long,
    val wallDamageMultiplier: Float = 1f,
    val splashRadius: Float = 0f,
)

object TroopConfig {
    private val configs: Map<TroopType, List<TroopLevelConfig>> = mapOf(
        // Barbarian equivalent — cheap melee, targets nearest
        TroopType.HumanSoldier to listOf(
            TroopLevelConfig(level = 1, hp = 45, dps = 8, speed = 1f, range = 0.5f, trainingCost = Resources(gold = 25), trainingTimeSeconds = 20),
            TroopLevelConfig(level = 2, hp = 54, dps = 11, speed = 1f, range = 0.5f, trainingCost = Resources(gold = 40), trainingTimeSeconds = 20),
            TroopLevelConfig(level = 3, hp = 65, dps = 14, speed = 1f, range = 0.5f, trainingCost = Resources(gold = 60), trainingTimeSeconds = 20),
        ),
        // Archer equivalent — ranged, squishy, targets nearest
        TroopType.ElfArcher to listOf(
            TroopLevelConfig(level = 1, hp = 20, dps = 7, speed = 1.2f, range = 3.5f, trainingCost = Resources(gold = 50), trainingTimeSeconds = 25),
            TroopLevelConfig(level = 2, hp = 23, dps = 9, speed = 1.2f, range = 3.5f, trainingCost = Resources(gold = 80), trainingTimeSeconds = 25),
            TroopLevelConfig(level = 3, hp = 28, dps = 12, speed = 1.2f, range = 3.5f, trainingCost = Resources(gold = 120), trainingTimeSeconds = 25),
        ),
        // Wall Breaker equivalent — targets walls, bonus damage to walls
        TroopType.DwarfSapper to listOf(
            TroopLevelConfig(level = 1, hp = 20, dps = 12, speed = 1.5f, range = 0.5f, trainingCost = Resources(gold = 100, metal = 20), trainingTimeSeconds = 30, wallDamageMultiplier = 10f),
            TroopLevelConfig(level = 2, hp = 24, dps = 16, speed = 1.5f, range = 0.5f, trainingCost = Resources(gold = 150, metal = 30), trainingTimeSeconds = 30, wallDamageMultiplier = 10f),
            TroopLevelConfig(level = 3, hp = 29, dps = 21, speed = 1.5f, range = 0.5f, trainingCost = Resources(gold = 200, metal = 40), trainingTimeSeconds = 30, wallDamageMultiplier = 10f),
        ),
        // Giant equivalent — tanky, targets defenses
        TroopType.OrcBerserker to listOf(
            TroopLevelConfig(level = 1, hp = 300, dps = 11, speed = 0.6f, range = 0.5f, trainingCost = Resources(gold = 250, metal = 20), trainingTimeSeconds = 60),
            TroopLevelConfig(level = 2, hp = 360, dps = 14, speed = 0.6f, range = 0.5f, trainingCost = Resources(gold = 400, metal = 40), trainingTimeSeconds = 60),
            TroopLevelConfig(level = 3, hp = 430, dps = 18, speed = 0.6f, range = 0.5f, trainingCost = Resources(gold = 600, metal = 60), trainingTimeSeconds = 60),
        ),
        // Goblin equivalent — fast, targets resource buildings
        TroopType.Goblin to listOf(
            TroopLevelConfig(level = 1, hp = 25, dps = 11, speed = 2f, range = 0.5f, trainingCost = Resources(gold = 25), trainingTimeSeconds = 15),
            TroopLevelConfig(level = 2, hp = 30, dps = 14, speed = 2f, range = 0.5f, trainingCost = Resources(gold = 40), trainingTimeSeconds = 15),
            TroopLevelConfig(level = 3, hp = 36, dps = 19, speed = 2f, range = 0.5f, trainingCost = Resources(gold = 60), trainingTimeSeconds = 15),
        ),
        // Wizard equivalent — ranged splash, glass cannon
        TroopType.Wizard to listOf(
            TroopLevelConfig(level = 1, hp = 75, dps = 50, speed = 1f, range = 3f, trainingCost = Resources(gold = 150, metal = 50), trainingTimeSeconds = 60, splashRadius = 1f),
            TroopLevelConfig(level = 2, hp = 90, dps = 70, speed = 1f, range = 3f, trainingCost = Resources(gold = 250, metal = 80), trainingTimeSeconds = 60, splashRadius = 1f),
            TroopLevelConfig(level = 3, hp = 108, dps = 90, speed = 1f, range = 3f, trainingCost = Resources(gold = 400, metal = 120), trainingTimeSeconds = 60, splashRadius = 1f),
        ),
        // Dragon equivalent — tanky ranged splash, slow, expensive
        TroopType.Dragon to listOf(
            TroopLevelConfig(level = 1, hp = 500, dps = 35, speed = 0.8f, range = 2.5f, trainingCost = Resources(gold = 500, metal = 100), trainingTimeSeconds = 120, splashRadius = 1f),
            TroopLevelConfig(level = 2, hp = 600, dps = 45, speed = 0.8f, range = 2.5f, trainingCost = Resources(gold = 750, metal = 150), trainingTimeSeconds = 120, splashRadius = 1f),
            TroopLevelConfig(level = 3, hp = 720, dps = 55, speed = 0.8f, range = 2.5f, trainingCost = Resources(gold = 1000, metal = 200), trainingTimeSeconds = 120, splashRadius = 1f),
        ),
    )

    fun configFor(type: TroopType, level: Int): TroopLevelConfig? =
        configs[type]?.find { it.level == level }
}
