package it.curzel.tamahero.rendering

import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.TroopType

data class SpriteRegion(val x: Int, val y: Int, val width: Int, val height: Int)

object SpriteSheetConfig {

    private val BUILDING_SPRITES = mapOf(
        BuildingType.CommandCenter to SpriteRegion(0, 0, 128, 128),
        BuildingType.Barracks to SpriteRegion(128, 0, 128, 64),
        BuildingType.Hangar to SpriteRegion(256, 0, 128, 64),
        BuildingType.MetalMine to SpriteRegion(384, 0, 64, 64),
        BuildingType.CrystalMine to SpriteRegion(448, 0, 64, 64),
        BuildingType.CrystalMine to SpriteRegion(0, 128, 64, 64),
        BuildingType.MetalStorage to SpriteRegion(64, 128, 64, 64),
        BuildingType.CrystalStorage to SpriteRegion(128, 128, 64, 64),
        BuildingType.DeuteriumStorage to SpriteRegion(192, 128, 64, 64),
        BuildingType.GaussCannon to SpriteRegion(320, 128, 64, 64),
        BuildingType.LightLaser to SpriteRegion(384, 128, 64, 64),
        BuildingType.MissileLauncher to SpriteRegion(448, 128, 64, 64),
        BuildingType.ShieldDome to SpriteRegion(0, 192, 64, 64),
        BuildingType.Wall to SpriteRegion(128, 192, 32, 32),
        BuildingType.LandMine to SpriteRegion(160, 192, 32, 32),
        BuildingType.GravityWell to SpriteRegion(192, 192, 32, 32),
        BuildingType.DeuteriumSynthesizer to SpriteRegion(0, 192, 64, 64),
        BuildingType.DeuteriumStorage to SpriteRegion(192, 128, 64, 64),
        BuildingType.RoboticsFactory to SpriteRegion(64, 128, 64, 64),
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
