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

    @Serializable
    @SerialName("collect_all")
    data object CollectAll : ClientMessage()

    @Serializable
    @SerialName("demolish")
    data class Demolish(
        val buildingId: Long,
    ) : ClientMessage()

    @Serializable
    @SerialName("cancel_construction")
    data class CancelConstruction(
        val buildingId: Long,
    ) : ClientMessage()

    @Serializable
    @SerialName("speed_up")
    data class SpeedUp(
        val buildingId: Long,
    ) : ClientMessage()

    @Serializable
    @SerialName("train")
    data class Train(
        val troopType: TroopType,
        val count: Int = 1,
        val level: Int = 1,
    ) : ClientMessage()

    @Serializable
    @SerialName("cancel_training")
    data class CancelTraining(
        val index: Int,
    ) : ClientMessage()

    @Serializable
    @SerialName("rearm_trap")
    data class RearmTrap(
        val buildingId: Long,
    ) : ClientMessage()

    @Serializable
    @SerialName("rearm_all_traps")
    data object RearmAllTraps : ClientMessage()

    @Serializable
    @SerialName("collect_event_rewards")
    data object CollectEventRewards : ClientMessage()

    @Serializable
    @SerialName("find_opponent")
    data object FindOpponent : ClientMessage()

    @Serializable
    @SerialName("next_opponent")
    data object NextOpponent : ClientMessage()

    @Serializable
    @SerialName("start_pvp")
    data class StartPvp(
        val targetId: Long,
    ) : ClientMessage()

    @Serializable
    @SerialName("deploy_troop")
    data class DeployTroop(
        val troopType: TroopType,
        val x: Float,
        val y: Float,
    ) : ClientMessage()

    @Serializable
    @SerialName("end_battle")
    data object EndBattle : ClientMessage()

    @Serializable
    @SerialName("get_leaderboard")
    data object GetLeaderboard : ClientMessage()

    @Serializable
    @SerialName("register_device")
    data class RegisterDevice(
        val token: String,
        val platform: String,
    ) : ClientMessage()

    @Serializable
    @SerialName("unregister_device")
    data class UnregisterDevice(
        val token: String,
    ) : ClientMessage()
}
