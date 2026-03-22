package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
enum class TroopType {
    HumanSoldier,
    ElfArcher,
    DwarfSapper,
    OrcBerserker,
    Goblin,
    Wizard,
    Dragon;

    val targetPreference: TargetPreference get() = when (this) {
        OrcBerserker -> TargetPreference.Defenses
        DwarfSapper -> TargetPreference.Walls
        Goblin -> TargetPreference.Resources
        else -> TargetPreference.Nearest
    }
}

enum class TargetPreference {
    Nearest,
    Defenses,
    Walls,
    Resources,
}
