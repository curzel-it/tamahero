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
    WizardTower,
    Wall,
    SpikeTrap,
    SpringTrap,
    GiantBomb,
    ShieldDome;

    val isProducer: Boolean get() = this in setOf(LumberCamp, GoldMine, Forge)
    val isDefense: Boolean get() = this in setOf(Cannon, ArcherTower, Mortar, WizardTower, Wall)
    val isStorage: Boolean get() = this in setOf(WoodStorage, GoldStorage, MetalStorage)
    val isTrap: Boolean get() = this in setOf(SpikeTrap, SpringTrap, GiantBomb)
    val isResource: Boolean get() = isProducer || isStorage || this == TownHall
}
