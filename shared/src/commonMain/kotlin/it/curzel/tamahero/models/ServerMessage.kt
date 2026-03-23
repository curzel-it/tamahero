package it.curzel.tamahero.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ServerMessage {

    @Serializable
    @SerialName("connected")
    data class Connected(
        val playerId: Long,
        val protocolVersion: Int = 1,
    ) : ServerMessage()

    @Serializable
    @SerialName("game_state")
    data class GameStateUpdated(
        val state: GameState,
    ) : ServerMessage()

    @Serializable
    @SerialName("building_complete")
    data class BuildingComplete(
        val buildingId: Long,
        val buildingType: BuildingType,
        val level: Int,
    ) : ServerMessage()

    @Serializable
    @SerialName("resources_updated")
    data class ResourcesUpdated(
        val resources: Resources,
    ) : ServerMessage()

    @Serializable
    @SerialName("training_complete")
    data class TrainingComplete(
        val troopType: TroopType,
        val level: Int,
    ) : ServerMessage()

    @Serializable
    @SerialName("event_started")
    data class EventStarted(
        val eventType: EventType,
    ) : ServerMessage()

    @Serializable
    @SerialName("event_ended")
    data class EventEnded(
        val eventType: EventType,
        val success: Boolean,
        val rewards: Resources,
    ) : ServerMessage()

    @Serializable
    @SerialName("hero_level_up")
    data class HeroLevelUp(
        val level: Int,
    ) : ServerMessage()

    @Serializable
    @SerialName("opponent_found")
    data class OpponentFound(
        val match: MatchmakingResult,
    ) : ServerMessage()

    @Serializable
    @SerialName("no_opponent")
    data class NoOpponentFound(
        val reason: String = "",
    ) : ServerMessage()

    @Serializable
    @SerialName("pvp_battle_started")
    data class PvpBattleStarted(
        val battle: PvpBattle,
    ) : ServerMessage()

    @Serializable
    @SerialName("pvp_battle_tick")
    data class PvpBattleTick(
        val battle: PvpBattle,
    ) : ServerMessage()

    @Serializable
    @SerialName("pvp_battle_ended")
    data class PvpBattleEnded(
        val result: PvpResult,
    ) : ServerMessage()

    @Serializable
    @SerialName("defense_result")
    data class DefenseResult(
        val entry: DefenseLogEntry,
    ) : ServerMessage()

    @Serializable
    @SerialName("leaderboard")
    data class Leaderboard(
        val entries: List<LeaderboardEntry>,
        val yourRank: Int,
    ) : ServerMessage()

    @Serializable
    @SerialName("error")
    data class Error(
        val reason: String,
        val details: String = "",
    ) : ServerMessage()
}
