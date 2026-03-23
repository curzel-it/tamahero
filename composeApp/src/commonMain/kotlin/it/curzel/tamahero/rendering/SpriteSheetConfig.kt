package it.curzel.tamahero.rendering

import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.TroopType

data class SpriteRegion(val x: Int, val y: Int, val width: Int, val height: Int)

object SpriteSheetConfig {

    private val BUILDING_SPRITES = mapOf(
        BuildingType.CommandCenter to SpriteRegion(0, 0, 128, 128),
        BuildingType.Academy to SpriteRegion(128, 0, 128, 64),
        BuildingType.Hangar to SpriteRegion(256, 0, 128, 64),
        BuildingType.AlloyRefinery to SpriteRegion(384, 0, 64, 64),
        BuildingType.CreditMint to SpriteRegion(448, 0, 64, 64),
        BuildingType.Foundry to SpriteRegion(0, 128, 64, 64),
        BuildingType.AlloySilo to SpriteRegion(64, 128, 64, 64),
        BuildingType.CreditVault to SpriteRegion(128, 128, 64, 64),
        BuildingType.CrystalSilo to SpriteRegion(192, 128, 64, 64),
        BuildingType.RailGun to SpriteRegion(320, 128, 64, 64),
        BuildingType.LaserTurret to SpriteRegion(384, 128, 64, 64),
        BuildingType.MissileBattery to SpriteRegion(448, 128, 64, 64),
        BuildingType.ShieldDome to SpriteRegion(0, 192, 64, 64),
        BuildingType.Barrier to SpriteRegion(128, 192, 32, 32),
        BuildingType.MineTrap to SpriteRegion(160, 192, 32, 32),
        BuildingType.GravityTrap to SpriteRegion(192, 192, 32, 32),
        BuildingType.PlasmaReactor to SpriteRegion(0, 192, 64, 64),
        BuildingType.PlasmaBank to SpriteRegion(192, 128, 64, 64),
        BuildingType.DroneStation to SpriteRegion(64, 128, 64, 64),
    )

    private val TROOP_SPRITES = mapOf(
        TroopType.Juggernaut to SpriteRegion(64, 192, 64, 64),
        TroopType.Marine to SpriteRegion(224, 192, 32, 32),
        TroopType.Sniper to SpriteRegion(256, 192, 32, 32),
        TroopType.Engineer to SpriteRegion(288, 192, 32, 32),
    )

    fun regionFor(type: BuildingType): SpriteRegion? = BUILDING_SPRITES[type]

    fun regionFor(type: TroopType): SpriteRegion? = TROOP_SPRITES[type]
}
