package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val playerId: Long,
    val resources: Resources = Resources(),
    val village: Village,
    val troops: List<Troop> = emptyList(),
    val lastUpdatedAt: Long,
)
