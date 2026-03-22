package it.curzel.tamahero.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import it.curzel.tamahero.game.TILE_SIZE
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.sprites.SpritesRepository
import it.curzel.tamahero.utils.Vector2d
import kotlin.math.roundToInt

private const val SPRITES_SHEET_ID: UInt = 100u

fun initBuildingSprites() {
    SpritesRepository.registerSheet(SPRITES_SHEET_ID, "sprites")
}

fun DrawScope.drawBuildings(
    buildings: List<PlacedBuilding>,
    camera: Vector2d,
    renderingScale: Float,
) {
    val tileSize = TILE_SIZE * renderingScale
    val spriteSheet = SpritesRepository.imageBitmap(SPRITES_SHEET_ID)

    for (building in buildings) {
        val config = BuildingConfig.configFor(building.type, building.level)
        val bSize = config?.size ?: 2
        val screenX = (building.x.toFloat() - camera.x) * tileSize
        val screenY = (building.y.toFloat() - camera.y) * tileSize
        val w = tileSize * bSize
        val h = tileSize * bSize

        val region = SpriteSheetConfig.regionFor(building.type)
        if (region != null) {
            drawImage(
                image = spriteSheet,
                srcOffset = IntOffset(region.x, region.y),
                srcSize = IntSize(region.width, region.height),
                dstOffset = IntOffset(screenX.roundToInt(), screenY.roundToInt()),
                dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                filterQuality = FilterQuality.None,
            )
        }

        if (building.constructionStartedAt != null) {
            drawRect(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(screenX, screenY), size = Size(w, h))
        }
    }
}

fun DrawScope.drawBuildingGhost(
    buildingType: BuildingType,
    gridX: Int,
    gridY: Int,
    isValid: Boolean,
    camera: Vector2d,
    renderingScale: Float,
) {
    val tileSize = TILE_SIZE * renderingScale
    val config = BuildingConfig.configFor(buildingType, 1) ?: return
    val bSize = config.size
    val screenX = (gridX.toFloat() - camera.x) * tileSize
    val screenY = (gridY.toFloat() - camera.y) * tileSize
    val w = tileSize * bSize
    val h = tileSize * bSize

    val region = SpriteSheetConfig.regionFor(buildingType)
    if (region != null) {
        val spriteSheet = SpritesRepository.imageBitmap(SPRITES_SHEET_ID)
        drawImage(
            image = spriteSheet,
            srcOffset = IntOffset(region.x, region.y),
            srcSize = IntSize(region.width, region.height),
            dstOffset = IntOffset(screenX.roundToInt(), screenY.roundToInt()),
            dstSize = IntSize(w.roundToInt(), h.roundToInt()),
            filterQuality = FilterQuality.None,
            alpha = 0.5f,
        )
    }

    val borderColor = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336)
    drawRect(color = borderColor, topLeft = Offset(screenX, screenY), size = Size(w, h), style = Stroke(width = 2f))
}
