package it.curzel.tamahero.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.game.TILE_SIZE
import it.curzel.tamahero.utils.Vector2d
import kotlin.math.roundToInt

private const val GRID_SIZE = 20
private val GridColor = Color(0xFFE8E8F0).copy(alpha = 0.08f)
private val BorderColor = Color(0xFFE8E8F0).copy(alpha = 0.25f)
private val FpsTextColor = Color(0xFFE8E8F0)

fun DrawScope.drawVillageBorder(
    camera: Vector2d,
    renderingScale: Float
) {
    val tileSize = TILE_SIZE * renderingScale
    val originX = -camera.x * tileSize
    val originY = -camera.y * tileSize
    val gridWidth = GRID_SIZE * tileSize
    val gridHeight = GRID_SIZE * tileSize

    drawRect(
        color = BorderColor,
        topLeft = Offset(originX, originY),
        size = Size(gridWidth, gridHeight),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * renderingScale)
    )
}

fun DrawScope.drawGrid(
    camera: Vector2d,
    renderingScale: Float
) {
    val tileSize = TILE_SIZE * renderingScale
    val originX = -camera.x * tileSize
    val originY = -camera.y * tileSize
    val gridWidth = GRID_SIZE * tileSize
    val gridHeight = GRID_SIZE * tileSize

    for (i in 1 until GRID_SIZE) {
        val x = originX + i * tileSize
        drawLine(
            color = GridColor,
            start = Offset(x, originY),
            end = Offset(x, originY + gridHeight),
            strokeWidth = 1f
        )
        val y = originY + i * tileSize
        drawLine(
            color = GridColor,
            start = Offset(originX, y),
            end = Offset(originX + gridWidth, y),
            strokeWidth = 1f
        )
    }
}

fun DrawScope.drawFpsText(
    fps: Float,
    textMeasurer: TextMeasurer,
    canvasSize: Size
) {
    val fpsText = "FPS: ${fps.roundToInt()}"
    val textResult = textMeasurer.measure(
        text = fpsText,
        style = TextStyle(
            color = FpsTextColor,
            fontSize = 14.sp
        )
    )

    val padding = 8f
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            x = padding,
            y = canvasSize.height - textResult.size.height - padding
        )
    )
}
