package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
enum class BuildingType {
    TownHall,
    LumberCamp,
    GoldMine,
    Forge,
    WoodStorage,
    GoldStorage,
    MetalStorage,
    Barracks,
    ArmyCamp,
    Cannon,
    ArcherTower,
    Wall,
}
