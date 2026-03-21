package it.curzel.tamahero.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage {

    @Serializable
    @SerialName("keep_alive")
    data object KeepAlive : ClientMessage()

    @Serializable
    @SerialName("get_village")
    data object GetVillage : ClientMessage()

    @Serializable
    @SerialName("build")
    data class Build(
        val buildingType: BuildingType,
        val x: Int,
        val y: Int,
    ) : ClientMessage()

    @Serializable
    @SerialName("upgrade")
    data class Upgrade(
        val buildingId: Long,
    ) : ClientMessage()

    @Serializable
    @SerialName("move")
    data class Move(
        val buildingId: Long,
        val x: Int,
        val y: Int,
    ) : ClientMessage()

    @Serializable
    @SerialName("collect")
    data class Collect(
        val buildingId: Long,
    ) : ClientMessage()
}
