package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class PvpBattle(
    val battleId: String,
    val attackerId: Long,
    val defenderId: Long,
    val defenderName: String,
    val defenderTrophies: Int,
    val defenderBase: Village,
    val availableTroops: Army,
    val deployedTroops: List<Troop> = emptyList(),
    val buildings: List<PlacedBuilding>,
    val battleShieldHp: Int = 0,
    val startedAt: Long,
    val timeLimitMs: Long = 180_000,
    val stars: Int = 0,
    val loot: Resources = Resources(),
    val ended: Boolean = false,
    val surrendered: Boolean = false,
) {
    val totalPreBattleHp: Int get() = defenderBase.buildings
        .filter { !it.type.isTrap && it.type != BuildingType.ShieldDome }
        .sumOf { it.hp }

    val currentHp: Int get() = buildings
        .filter { !it.type.isTrap && it.type != BuildingType.ShieldDome }
        .sumOf { it.hp }

    val destructionPercent: Int get() {
        val total = totalPreBattleHp
        if (total <= 0) return 100
        val destroyed = total - currentHp
        return ((destroyed * 100) / total).coerceIn(0, 100)
    }

    val commandCenterDestroyed: Boolean get() =
        !buildings.any { it.type == BuildingType.CommandCenter }

    val currentStars: Int get() {
        var s = 0
        if (destructionPercent >= 50) s++
        if (commandCenterDestroyed) s++
        if (destructionPercent >= 100) s++
        return s
    }

    fun timeRemainingMs(now: Long): Long = (timeLimitMs - (now - startedAt)).coerceAtLeast(0)

    fun isTimeUp(now: Long): Boolean = now - startedAt >= timeLimitMs
}

@Serializable
data class PvpResult(
    val battleId: String,
    val attackerId: Long,
    val defenderId: Long,
    val stars: Int,
    val loot: Resources,
    val attackerTrophyDelta: Int,
    val defenderTrophyDelta: Int,
    val destructionPercent: Int,
)

@Serializable
data class DefenseLogEntry(
    val timestamp: Long,
    val attackerName: String,
    val stars: Int,
    val lootLost: Resources,
    val trophyDelta: Int,
)

@Serializable
data class MatchmakingResult(
    val targetId: Long,
    val targetName: String,
    val targetTrophies: Int,
    val targetCommandCenterLevel: Int,
    val targetBase: Village,
    val lootAvailable: Resources,
)

object PvpCalculations {

    fun calculateLootAvailable(defenderResources: Resources): Resources {
        return Resources(
            credits = (defenderResources.credits * 0.2).toLong().coerceAtLeast(0),
            metal = (defenderResources.metal * 0.2).toLong().coerceAtLeast(0),
            crystal = (defenderResources.crystal * 0.2).toLong().coerceAtLeast(0),
            deuterium = (defenderResources.deuterium * 0.1).toLong().coerceAtLeast(0),
        )
    }

    fun calculateLootStolen(availableLoot: Resources, destructionPercent: Int): Resources {
        val factor = destructionPercent / 100.0
        return Resources(
            credits = (availableLoot.credits * factor).toLong(),
            metal = (availableLoot.metal * factor).toLong(),
            crystal = (availableLoot.crystal * factor).toLong(),
            deuterium = (availableLoot.deuterium * factor).toLong(),
        )
    }

    fun calculateTrophyDelta(attackerTrophies: Int, defenderTrophies: Int, stars: Int): Int {
        if (stars <= 0) return -calculateTrophyLoss(attackerTrophies, defenderTrophies)
        val diff = defenderTrophies - attackerTrophies
        val base = (30 + diff * 0.1).coerceIn(5.0, 59.0)
        return (base * stars / 3.0).toInt().coerceAtLeast(1)
    }

    private fun calculateTrophyLoss(attackerTrophies: Int, defenderTrophies: Int): Int {
        val diff = attackerTrophies - defenderTrophies
        val base = (20 + diff * 0.1).coerceIn(5.0, 40.0)
        return base.toInt()
    }

    fun calculateShieldDuration(destructionPercent: Int): Long {
        return when {
            destructionPercent >= 90 -> 16 * 3_600_000L
            destructionPercent >= 60 -> 14 * 3_600_000L
            destructionPercent >= 30 -> 12 * 3_600_000L
            else -> 0L
        }
    }
}
