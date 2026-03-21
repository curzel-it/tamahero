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
    @SerialName("error")
    data class Error(
        val reason: String,
        val details: String = "",
    ) : ServerMessage()
}
