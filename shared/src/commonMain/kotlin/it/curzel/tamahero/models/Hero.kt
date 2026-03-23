package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class Hero(
    val level: Int = 1,
    val xp: Long = 0,
    val hunger: Int = 100,
    val happiness: Int = 100,
    val lastFedAt: Long = 0,
    val lastTrainedAt: Long = 0,
    val lastUpdatedAt: Long = 0,
)
