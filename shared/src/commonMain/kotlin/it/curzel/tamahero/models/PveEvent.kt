package it.curzel.tamahero.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EventType {
    Earthquake,
    Storm,
    ScoutParty,
    Battle;

    val isBattle: Boolean get() = this !in setOf(Earthquake, Storm)
    val isDisaster: Boolean get() = this in setOf(Earthquake, Storm)
    val requiredTownHallLevel: Int get() = when (this) {
        Earthquake, Storm, ScoutParty -> 1
        Battle -> 1
    }
}

@Serializable
data class EventWave(
    val troops: List<EventTroop>,
    val delayMs: Long = 0,
)

@Serializable
data class EventTroop(
    val type: TroopType,
    val count: Int,
    val level: Int = 1,
    val hp: Int? = null,
    val dps: Int? = null,
    val isBoss: Boolean = false,
)

@Serializable
data class EventRewards(
    val success: Resources = Resources(),
    val debrisRecoveryRate: Double = 0.3,
)

@Serializable
data class ActiveEvent(
    val type: EventType,
    val startedAt: Long,
    val currentWave: Int = 0,
    val totalWaves: Int = 1,
    val nextWaveAt: Long = 0,
    val completed: Boolean = false,
    val rewards: EventRewards = EventRewards(),
    val pendingRewards: Resources? = null,
)
