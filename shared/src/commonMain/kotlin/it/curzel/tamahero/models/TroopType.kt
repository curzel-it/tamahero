package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
enum class TroopType {
    HumanSoldier,
    ElfArcher,
    DwarfSapper,
    OrcBerserker,
}
