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
)

object TroopConfig {
    private val configs: Map<TroopType, List<TroopLevelConfig>> = mapOf(
        TroopType.HumanSoldier to listOf(
            TroopLevelConfig(level = 1, hp = 50, dps = 10, speed = 1f, range = 1f, trainingCost = Resources(gold = 25), trainingTimeSeconds = 20),
            TroopLevelConfig(level = 2, hp = 65, dps = 14, speed = 1f, range = 1f, trainingCost = Resources(gold = 40), trainingTimeSeconds = 25),
        ),
        TroopType.ElfArcher to listOf(
            TroopLevelConfig(level = 1, hp = 25, dps = 8, speed = 1.2f, range = 4f, trainingCost = Resources(gold = 50), trainingTimeSeconds = 25),
            TroopLevelConfig(level = 2, hp = 30, dps = 12, speed = 1.2f, range = 4.5f, trainingCost = Resources(gold = 80), trainingTimeSeconds = 30),
        ),
        TroopType.DwarfSapper to listOf(
            TroopLevelConfig(level = 1, hp = 80, dps = 20, speed = 0.7f, range = 1f, trainingCost = Resources(gold = 100, metal = 20), trainingTimeSeconds = 40),
            TroopLevelConfig(level = 2, hp = 110, dps = 28, speed = 0.7f, range = 1f, trainingCost = Resources(gold = 150, metal = 40), trainingTimeSeconds = 50),
        ),
        TroopType.OrcBerserker to listOf(
            TroopLevelConfig(level = 1, hp = 60, dps = 18, speed = 1.1f, range = 1f, trainingCost = Resources(gold = 75, metal = 10), trainingTimeSeconds = 30),
            TroopLevelConfig(level = 2, hp = 80, dps = 25, speed = 1.1f, range = 1f, trainingCost = Resources(gold = 120, metal = 25), trainingTimeSeconds = 40),
        ),
    )

    fun configFor(type: TroopType, level: Int): TroopLevelConfig? =
        configs[type]?.find { it.level == level }
}
