package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
data class TrainingQueueEntry(
    val troopType: TroopType,
    val level: Int = 1,
    val startedAt: Long? = null,
)

@Serializable
data class TrainingQueue(
    val entries: List<TrainingQueueEntry> = emptyList(),
)
