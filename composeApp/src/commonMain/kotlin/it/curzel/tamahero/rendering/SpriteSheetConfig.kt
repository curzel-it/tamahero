package it.curzel.tamahero.rendering

import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.TroopType

data class SpriteRegion(val x: Int, val y: Int, val width: Int, val height: Int)

object SpriteSheetConfig {

    private val BUILDING_SPRITES = mapOf(
        BuildingType.TownHall to SpriteRegion(0, 0, 128, 128),
        BuildingType.Barracks to SpriteRegion(128, 0, 128, 64),
        BuildingType.ArmyCamp to SpriteRegion(256, 0, 128, 64),
        BuildingType.LumberCamp to SpriteRegion(384, 0, 64, 64),
        BuildingType.GoldMine to SpriteRegion(448, 0, 64, 64),
        BuildingType.Forge to SpriteRegion(0, 128, 64, 64),
        BuildingType.WoodStorage to SpriteRegion(64, 128, 64, 64),
        BuildingType.GoldStorage to SpriteRegion(128, 128, 64, 64),
        BuildingType.MetalStorage to SpriteRegion(192, 128, 64, 64),
        BuildingType.Cannon to SpriteRegion(320, 128, 64, 64),
        BuildingType.ArcherTower to SpriteRegion(384, 128, 64, 64),
        BuildingType.Mortar to SpriteRegion(448, 128, 64, 64),
        BuildingType.ShieldDome to SpriteRegion(0, 192, 64, 64),
        BuildingType.Wall to SpriteRegion(128, 192, 32, 32),
        BuildingType.SpikeTrap to SpriteRegion(160, 192, 32, 32),
        BuildingType.SpringTrap to SpriteRegion(192, 192, 32, 32),
    )

    private val TROOP_SPRITES = mapOf(
        TroopType.OrcBerserker to SpriteRegion(64, 192, 64, 64),
        TroopType.HumanSoldier to SpriteRegion(224, 192, 32, 32),
        TroopType.ElfArcher to SpriteRegion(256, 192, 32, 32),
        TroopType.DwarfSapper to SpriteRegion(288, 192, 32, 32),
    )

    fun regionFor(type: BuildingType): SpriteRegion? = BUILDING_SPRITES[type]

    fun regionFor(type: TroopType): SpriteRegion? = TROOP_SPRITES[type]
}
