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
    selectedBuildingId: Long? = null,
) {
    val tileSize = TILE_SIZE * renderingScale
    val spriteSheet = SpritesRepository.imageBitmap(SPRITES_SHEET_ID)

    for (building in buildings) {
        val config = BuildingConfig.configFor(building.type, building.level)
        val bw = config?.width ?: 2
        val bh = config?.height ?: 2
        val screenX = (building.x.toFloat() - camera.x) * tileSize
        val screenY = (building.y.toFloat() - camera.y) * tileSize
        val w = tileSize * bw
        val h = tileSize * bh

        val region = SpriteSheetConfig.regionFor(building.type)
        val isUnderConstruction = building.constructionStartedAt != null
        if (region != null) {
            drawImage(
                image = spriteSheet,
                srcOffset = IntOffset(region.x, region.y),
                srcSize = IntSize(region.width, region.height),
                dstOffset = IntOffset(screenX.roundToInt(), screenY.roundToInt()),
                dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                filterQuality = FilterQuality.None,
                alpha = if (isUnderConstruction) 0.4f else 1.0f,
            )
        }

        if (building.id == selectedBuildingId) {
            drawRect(
                color = Color(0xFFE8E8F0),
                topLeft = Offset(screenX, screenY),
                size = Size(w, h),
                style = Stroke(width = 2f * renderingScale),
            )
        }

        // HP bar (when damaged)
        if (config != null && !isUnderConstruction && building.hp > 0 && building.hp < config.hp) {
            val barWidth = w * 0.8f
            val barHeight = 3f * renderingScale
            val barX = screenX + (w - barWidth) * 0.5f
            val barY = screenY + h + 2f
            val hpRatio = building.hp.toFloat() / config.hp
            val hpColor = when {
                hpRatio > 0.5f -> Color(0xFF22C55E)
                hpRatio > 0.25f -> Color(0xFFFF9800)
                else -> Color(0xFFEF4444)
            }
            drawRect(Color(0x80000000), Offset(barX, barY), Size(barWidth, barHeight))
            drawRect(hpColor, Offset(barX, barY), Size(barWidth * hpRatio, barHeight))
        }

        // Production indicator (small dot above producer buildings with accumulated resources)
        if (config != null && !isUnderConstruction && building.type.isProducer) {
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val elapsed = now - building.lastCollectedAt
            if (elapsed > 10 * 60 * 1000) {
                val dotRadius = 3f * renderingScale
                val dotColor = when {
                    config.productionPerHour.credits > 0 -> Color(0xFFFFD700)
                    config.productionPerHour.alloy > 0 -> Color(0xFF4CAF50)
                    config.productionPerHour.crystal > 0 -> Color(0xFF9E9E9E)
                    config.productionPerHour.plasma > 0 -> Color(0xFF7B1FA2)
                    else -> Color.White
                }
                drawCircle(dotColor, dotRadius, Offset(screenX + w * 0.5f, screenY - dotRadius - 2f))
            }
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
    val screenX = (gridX.toFloat() - camera.x) * tileSize
    val screenY = (gridY.toFloat() - camera.y) * tileSize
    val w = tileSize * config.width
    val h = tileSize * config.height

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

    val borderColor = if (isValid) Color(0xFF22C55E) else Color(0xFFEF4444)
    drawRect(color = borderColor, topLeft = Offset(screenX, screenY), size = Size(w, h), style = Stroke(width = 2f))
}
