package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val playerId: Long,
    val playerName: String,
    val trophies: Int,
    val commandCenterLevel: Int,
)
