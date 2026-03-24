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
        TroopType.Marine to listOf(
            TroopLevelConfig(level = 1, hp = 45, dps = 8, speed = 1f, range = 0.5f, trainingCost = Resources(credits = 25), trainingTimeSeconds = 20),
            TroopLevelConfig(level = 2, hp = 54, dps = 11, speed = 1f, range = 0.5f, trainingCost = Resources(credits = 40), trainingTimeSeconds = 20),
            TroopLevelConfig(level = 3, hp = 65, dps = 14, speed = 1f, range = 0.5f, trainingCost = Resources(credits = 60), trainingTimeSeconds = 20),
            TroopLevelConfig(level = 4, hp = 78, dps = 18, speed = 1f, range = 0.5f, trainingCost = Resources(credits = 90), trainingTimeSeconds = 20),
            TroopLevelConfig(level = 5, hp = 94, dps = 23, speed = 1f, range = 0.5f, trainingCost = Resources(credits = 135), trainingTimeSeconds = 20),
            TroopLevelConfig(level = 6, hp = 113, dps = 29, speed = 1f, range = 0.5f, trainingCost = Resources(credits = 200), trainingTimeSeconds = 20),
        ),
        TroopType.Sniper to listOf(
            TroopLevelConfig(level = 1, hp = 20, dps = 7, speed = 1.2f, range = 3.5f, trainingCost = Resources(credits = 50), trainingTimeSeconds = 25),
            TroopLevelConfig(level = 2, hp = 23, dps = 9, speed = 1.2f, range = 3.5f, trainingCost = Resources(credits = 80), trainingTimeSeconds = 25),
            TroopLevelConfig(level = 3, hp = 28, dps = 12, speed = 1.2f, range = 3.5f, trainingCost = Resources(credits = 120), trainingTimeSeconds = 25),
            TroopLevelConfig(level = 4, hp = 34, dps = 15, speed = 1.2f, range = 3.5f, trainingCost = Resources(credits = 180), trainingTimeSeconds = 25),
            TroopLevelConfig(level = 5, hp = 41, dps = 19, speed = 1.2f, range = 3.5f, trainingCost = Resources(credits = 270), trainingTimeSeconds = 25),
            TroopLevelConfig(level = 6, hp = 49, dps = 24, speed = 1.2f, range = 3.5f, trainingCost = Resources(credits = 400), trainingTimeSeconds = 25),
        ),
        TroopType.Engineer to listOf(
            TroopLevelConfig(level = 1, hp = 20, dps = 12, speed = 1.5f, range = 0.5f, trainingCost = Resources(credits = 100, crystal = 20), trainingTimeSeconds = 30, wallDamageMultiplier = 10f),
            TroopLevelConfig(level = 2, hp = 24, dps = 16, speed = 1.5f, range = 0.5f, trainingCost = Resources(credits = 150, crystal = 30), trainingTimeSeconds = 30, wallDamageMultiplier = 10f),
            TroopLevelConfig(level = 3, hp = 29, dps = 21, speed = 1.5f, range = 0.5f, trainingCost = Resources(credits = 200, crystal = 40), trainingTimeSeconds = 30, wallDamageMultiplier = 10f),
            TroopLevelConfig(level = 4, hp = 35, dps = 27, speed = 1.5f, range = 0.5f, trainingCost = Resources(credits = 300, crystal = 60), trainingTimeSeconds = 30, wallDamageMultiplier = 10f),
            TroopLevelConfig(level = 5, hp = 42, dps = 35, speed = 1.5f, range = 0.5f, trainingCost = Resources(credits = 450, crystal = 90), trainingTimeSeconds = 30, wallDamageMultiplier = 10f),
            TroopLevelConfig(level = 6, hp = 50, dps = 45, speed = 1.5f, range = 0.5f, trainingCost = Resources(credits = 650, crystal = 130), trainingTimeSeconds = 30, wallDamageMultiplier = 10f),
        ),
        TroopType.Juggernaut to listOf(
            TroopLevelConfig(level = 1, hp = 300, dps = 11, speed = 0.6f, range = 0.5f, trainingCost = Resources(credits = 250, crystal = 20), trainingTimeSeconds = 60),
            TroopLevelConfig(level = 2, hp = 360, dps = 14, speed = 0.6f, range = 0.5f, trainingCost = Resources(credits = 400, crystal = 40), trainingTimeSeconds = 60),
            TroopLevelConfig(level = 3, hp = 430, dps = 18, speed = 0.6f, range = 0.5f, trainingCost = Resources(credits = 600, crystal = 60), trainingTimeSeconds = 60),
            TroopLevelConfig(level = 4, hp = 520, dps = 23, speed = 0.6f, range = 0.5f, trainingCost = Resources(credits = 900, crystal = 90), trainingTimeSeconds = 60),
            TroopLevelConfig(level = 5, hp = 625, dps = 29, speed = 0.6f, range = 0.5f, trainingCost = Resources(credits = 1350, crystal = 135), trainingTimeSeconds = 60),
            TroopLevelConfig(level = 6, hp = 750, dps = 37, speed = 0.6f, range = 0.5f, trainingCost = Resources(credits = 2000, crystal = 200), trainingTimeSeconds = 60),
        ),
        TroopType.Drone to listOf(
            TroopLevelConfig(level = 1, hp = 25, dps = 11, speed = 2f, range = 0.5f, trainingCost = Resources(credits = 25), trainingTimeSeconds = 15),
            TroopLevelConfig(level = 2, hp = 30, dps = 14, speed = 2f, range = 0.5f, trainingCost = Resources(credits = 40), trainingTimeSeconds = 15),
            TroopLevelConfig(level = 3, hp = 36, dps = 19, speed = 2f, range = 0.5f, trainingCost = Resources(credits = 60), trainingTimeSeconds = 15),
            TroopLevelConfig(level = 4, hp = 43, dps = 24, speed = 2f, range = 0.5f, trainingCost = Resources(credits = 90), trainingTimeSeconds = 15),
            TroopLevelConfig(level = 5, hp = 52, dps = 30, speed = 2f, range = 0.5f, trainingCost = Resources(credits = 135), trainingTimeSeconds = 15),
            TroopLevelConfig(level = 6, hp = 62, dps = 38, speed = 2f, range = 0.5f, trainingCost = Resources(credits = 200), trainingTimeSeconds = 15),
        ),
        TroopType.Spectre to listOf(
            TroopLevelConfig(level = 1, hp = 75, dps = 50, speed = 1f, range = 3f, trainingCost = Resources(credits = 150, crystal = 50), trainingTimeSeconds = 60, splashRadius = 1f),
            TroopLevelConfig(level = 2, hp = 90, dps = 70, speed = 1f, range = 3f, trainingCost = Resources(credits = 250, crystal = 80), trainingTimeSeconds = 60, splashRadius = 1f),
            TroopLevelConfig(level = 3, hp = 108, dps = 90, speed = 1f, range = 3f, trainingCost = Resources(credits = 400, crystal = 120), trainingTimeSeconds = 60, splashRadius = 1f),
            TroopLevelConfig(level = 4, hp = 130, dps = 115, speed = 1f, range = 3f, trainingCost = Resources(credits = 600, crystal = 180), trainingTimeSeconds = 60, splashRadius = 1f),
            TroopLevelConfig(level = 5, hp = 156, dps = 145, speed = 1f, range = 3f, trainingCost = Resources(credits = 900, crystal = 270), trainingTimeSeconds = 60, splashRadius = 1f),
            TroopLevelConfig(level = 6, hp = 187, dps = 185, speed = 1f, range = 3f, trainingCost = Resources(credits = 1350, crystal = 400), trainingTimeSeconds = 60, splashRadius = 1f),
        ),
        TroopType.Gunship to listOf(
            TroopLevelConfig(level = 1, hp = 500, dps = 35, speed = 0.8f, range = 2.5f, trainingCost = Resources(credits = 500, crystal = 100), trainingTimeSeconds = 120, splashRadius = 1f),
            TroopLevelConfig(level = 2, hp = 600, dps = 45, speed = 0.8f, range = 2.5f, trainingCost = Resources(credits = 750, crystal = 150), trainingTimeSeconds = 120, splashRadius = 1f),
            TroopLevelConfig(level = 3, hp = 720, dps = 55, speed = 0.8f, range = 2.5f, trainingCost = Resources(credits = 1000, crystal = 200), trainingTimeSeconds = 120, splashRadius = 1f),
            TroopLevelConfig(level = 4, hp = 865, dps = 68, speed = 0.8f, range = 2.5f, trainingCost = Resources(credits = 1500, crystal = 300), trainingTimeSeconds = 120, splashRadius = 1f),
            TroopLevelConfig(level = 5, hp = 1040, dps = 85, speed = 0.8f, range = 2.5f, trainingCost = Resources(credits = 2250, crystal = 450), trainingTimeSeconds = 120, splashRadius = 1f),
            TroopLevelConfig(level = 6, hp = 1250, dps = 105, speed = 0.8f, range = 2.5f, trainingCost = Resources(credits = 3400, crystal = 650), trainingTimeSeconds = 120, splashRadius = 1f),
        ),
    )

    fun configFor(type: TroopType, level: Int): TroopLevelConfig? =
        configs[type]?.find { it.level == level }
}
