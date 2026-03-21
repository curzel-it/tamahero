package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val playerId: Long,
    val resources: Resources = Resources(),
    val village: Village,
    val troops: List<Troop> = emptyList(),
    val trainingQueue: TrainingQueue = TrainingQueue(),
    val army: Army = Army(),
    val shieldExpiresAt: Long = 0,
    val battleShieldHp: Int = 0,
    val preBattleBuildings: List<PlacedBuilding> = emptyList(),
    val lastUpdatedAt: Long,
)
