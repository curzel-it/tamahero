package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
enum class TroopType {
    Marine,
    Sniper,
    Engineer,
    Juggernaut,
    Drone,
    Spectre,
    Gunship;

    val targetPreference: TargetPreference get() = when (this) {
        Juggernaut -> TargetPreference.Defenses
        Engineer -> TargetPreference.Walls
        Drone -> TargetPreference.Resources
        else -> TargetPreference.Nearest
    }
}

enum class TargetPreference {
    Nearest,
    Defenses,
    Walls,
    Resources,
}
