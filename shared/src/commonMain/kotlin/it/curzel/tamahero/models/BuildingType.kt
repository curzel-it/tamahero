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
    Mortar,
    Wall,
    SpikeTrap,
    SpringTrap,
    ShieldDome;

    val isProducer: Boolean get() = this in setOf(LumberCamp, GoldMine, Forge)
    val isDefense: Boolean get() = this in setOf(Cannon, ArcherTower, Mortar, Wall)
    val isStorage: Boolean get() = this in setOf(WoodStorage, GoldStorage, MetalStorage)
    val isTrap: Boolean get() = this in setOf(SpikeTrap, SpringTrap)
}
