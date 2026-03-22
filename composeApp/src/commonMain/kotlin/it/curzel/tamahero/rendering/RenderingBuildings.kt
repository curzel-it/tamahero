package it.curzel.tamahero.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.game.TILE_SIZE
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.utils.Vector2d

fun DrawScope.drawBuildings(
    buildings: List<PlacedBuilding>,
    camera: Vector2d,
    renderingScale: Float,
    textMeasurer: TextMeasurer,
) {
    val tileSize = TILE_SIZE * renderingScale
    for (building in buildings) {
        val config = BuildingConfig.configFor(building.type, building.level)
        val bSize = config?.size ?: 2
        val screenX = (building.x.toFloat() - camera.x) * tileSize
        val screenY = (building.y.toFloat() - camera.y) * tileSize
        val w = tileSize * bSize
        val h = tileSize * bSize
        val color = building.type.color()

        drawRect(color = color, topLeft = Offset(screenX, screenY), size = Size(w, h))
        drawRect(color = Color.Black, topLeft = Offset(screenX, screenY), size = Size(w, h), style = Stroke(width = 1f))

        val label = building.type.label()
        val textResult = textMeasurer.measure(
            text = label,
            style = TextStyle(color = Color.White, fontSize = (8 * renderingScale).sp),
        )
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(
                screenX + (w - textResult.size.width) / 2,
                screenY + (h - textResult.size.height) / 2,
            ),
        )

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
    textMeasurer: TextMeasurer,
) {
    val tileSize = TILE_SIZE * renderingScale
    val config = BuildingConfig.configFor(buildingType, 1) ?: return
    val bSize = config.size
    val screenX = (gridX.toFloat() - camera.x) * tileSize
    val screenY = (gridY.toFloat() - camera.y) * tileSize
    val w = tileSize * bSize
    val h = tileSize * bSize

    val fillColor = if (isValid) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color(0xFFF44336).copy(alpha = 0.4f)
    val borderColor = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336)

    drawRect(color = fillColor, topLeft = Offset(screenX, screenY), size = Size(w, h))
    drawRect(color = borderColor, topLeft = Offset(screenX, screenY), size = Size(w, h), style = Stroke(width = 2f))

    val label = buildingType.label()
    val textResult = textMeasurer.measure(
        text = label,
        style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = (8 * renderingScale).sp),
    )
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            screenX + (w - textResult.size.width) / 2,
            screenY + (h - textResult.size.height) / 2,
        ),
    )
}

private fun BuildingType.color(): Color = when (this) {
    BuildingType.TownHall -> Color(0xFFFFD700)
    BuildingType.LumberCamp -> Color(0xFF8B4513)
    BuildingType.GoldMine -> Color(0xFFDAA520)
    BuildingType.Forge -> Color(0xFFB0B0B0)
    BuildingType.WoodStorage -> Color(0xFFA0522D)
    BuildingType.GoldStorage -> Color(0xFFFFD700)
    BuildingType.MetalStorage -> Color(0xFF808080)
    BuildingType.Barracks -> Color(0xFF8B0000)
    BuildingType.ArmyCamp -> Color(0xFF556B2F)
    BuildingType.Cannon -> Color(0xFF2F4F4F)
    BuildingType.ArcherTower -> Color(0xFF4682B4)
    BuildingType.Mortar -> Color(0xFF696969)
    BuildingType.Wall -> Color(0xFF708090)
    BuildingType.SpikeTrap -> Color(0xFF8B0000)
    BuildingType.SpringTrap -> Color(0xFF9ACD32)
    BuildingType.ShieldDome -> Color(0xFF4169E1)
}

private fun BuildingType.label(): String = when (this) {
    BuildingType.TownHall -> "TH"
    BuildingType.LumberCamp -> "LC"
    BuildingType.GoldMine -> "GM"
    BuildingType.Forge -> "FG"
    BuildingType.WoodStorage -> "WS"
    BuildingType.GoldStorage -> "GS"
    BuildingType.MetalStorage -> "MS"
    BuildingType.Barracks -> "BK"
    BuildingType.ArmyCamp -> "AC"
    BuildingType.Cannon -> "CN"
    BuildingType.ArcherTower -> "AT"
    BuildingType.Mortar -> "MR"
    BuildingType.Wall -> "WL"
    BuildingType.SpikeTrap -> "ST"
    BuildingType.SpringTrap -> "SP"
    BuildingType.ShieldDome -> "SD"
}
