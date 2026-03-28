package it.curzel.tamahero.models

import kotlinx.serialization.Serializable

@Serializable
enum class BuildingType(val displayName: String) {
    CommandCenter("Command Center"),
    MetalMine("Metal Mine"),
    CrystalMine("Crystal Mine"),
    DeuteriumSynthesizer("Deuterium Synthesizer"),
    MetalStorage("Metal Storage"),
    CrystalStorage("Crystal Storage"),
    DeuteriumStorage("Deuterium Storage"),
    Barracks("Barracks"),
    Hangar("Hangar"),
    RoboticsFactory("Robotics Factory"),
    GaussCannon("Gauss Cannon"),
    LightLaser("Light Laser"),
    HeavyLaser("Heavy Laser"),
    MissileLauncher("Missile Launcher"),
    IonCannon("Ion Cannon"),
    PlasmaCannon("Plasma Cannon"),
    Wall("Wall"),
    LandMine("Land Mine"),
    GravityWell("Gravity Well"),
    NovaBomb("Nova Bomb"),
    ShieldDome("Shield Dome");

    val isProducer: Boolean get() = this in setOf(MetalMine, CrystalMine, DeuteriumSynthesizer)
    val isDefense: Boolean get() = this in setOf(GaussCannon, LightLaser, HeavyLaser, MissileLauncher, IonCannon, PlasmaCannon, Wall)
    val isStorage: Boolean get() = this in setOf(MetalStorage, CrystalStorage, DeuteriumStorage)
    val isTrap: Boolean get() = this in setOf(LandMine, GravityWell, NovaBomb)
    val isResource: Boolean get() = isProducer || isStorage || this == CommandCenter
}
