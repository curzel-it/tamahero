package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
enum class BuildingType {
    CommandCenter,
    AlloyRefinery,
    CreditMint,
    Foundry,
    AlloySilo,
    CreditVault,
    CrystalSilo,
    Academy,
    Hangar,
    RailGun,
    LaserTurret,
    MissileBattery,
    TeslaTower,
    Barrier,
    MineTrap,
    GravityTrap,
    NovaBomb,
    ShieldDome,
    PlasmaReactor,
    PlasmaBank,
    DroneStation;

    val isProducer: Boolean get() = this in setOf(AlloyRefinery, CreditMint, Foundry, PlasmaReactor)
    val isDefense: Boolean get() = this in setOf(RailGun, LaserTurret, MissileBattery, TeslaTower, Barrier)
    val isStorage: Boolean get() = this in setOf(AlloySilo, CreditVault, CrystalSilo, PlasmaBank)
    val isTrap: Boolean get() = this in setOf(MineTrap, GravityTrap, NovaBomb)
    val isResource: Boolean get() = isProducer || isStorage || this == CommandCenter
}
