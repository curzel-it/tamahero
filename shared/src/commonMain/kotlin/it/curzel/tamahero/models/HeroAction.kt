package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    TRAIN_STRENGTH,
    TRAIN_AGILITY,
    TRAIN_INTELLIGENCE,
    TRAIN_ENDURANCE,
    REST,
    WORK
}

@Serializable
data class HeroAction(
    val id: Long = 0,
    val heroId: Long = 0,
    val type: ActionType,
    val startedAt: Long,
    val completesAt: Long,
    val completed: Boolean = false
)
