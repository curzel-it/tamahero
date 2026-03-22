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

private val GridColor = Color(0xFFE8E8F0).copy(alpha = 0.08f)
private val FpsTextColor = Color(0xFFE8E8F0)

fun DrawScope.drawGrid(
    camera: Vector2d,
    renderingScale: Float
) {
    val tileSize = TILE_SIZE * renderingScale
    val strokeWidth = 1f

    val cameraOffsetX = -camera.x * TILE_SIZE * renderingScale
    val cameraOffsetY = -camera.y * TILE_SIZE * renderingScale

    val startX = ((cameraOffsetX % tileSize) - tileSize)
    val startY = ((cameraOffsetY % tileSize) - tileSize)

    var x = startX
    while (x <= size.width) {
        drawLine(
            color = GridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth
        )
        x += tileSize
    }

    var y = startY
    while (y <= size.height) {
        drawLine(
            color = GridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
        y += tileSize
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
