package it.curzel.tamahero.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import it.curzel.tamahero.game.TILE_SIZE
import kotlin.math.sin

@Composable
fun RenderingView(viewModel: GameViewModel) {
    val time by viewModel.totalRunTime.collectAsState()
    val renderingScale by viewModel.renderingScale.collectAsState()
    val fps by viewModel.fps.collectAsState()
    val showFps by viewModel.showFps.collectAsState()
    val showGrid by viewModel.showGrid.collectAsState()

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewModel.onViewSizeChanged(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        drawRect(Color(0xFF1e6f50), size = Size(time, 1f))
        drawRect(Color(0xFF111111), size = size)

        val camera = viewModel.camera

        if (showGrid) {
            drawGrid(camera = camera, renderingScale = renderingScale)
        }

        // Demo: draw some test rectangles to prove the rendering system works
        val tileSize = TILE_SIZE * renderingScale

        // A green square at tile (2, 2)
        val greenX = (2f - camera.x) * tileSize
        val greenY = (2f - camera.y) * tileSize
        drawRect(
            color = Color(0xFF4CAF50),
            topLeft = Offset(greenX, greenY),
            size = Size(tileSize, tileSize)
        )

        // A blue square at tile (4, 3)
        val blueX = (4f - camera.x) * tileSize
        val blueY = (3f - camera.y) * tileSize
        drawRect(
            color = Color(0xFF2196F3),
            topLeft = Offset(blueX, blueY),
            size = Size(tileSize, tileSize)
        )

        // A bouncing red square to show animation works
        val bounceOffset = sin(time * 2f) * 0.5f
        val redX = (6f - camera.x) * tileSize
        val redY = (2f + bounceOffset - camera.y) * tileSize
        drawRect(
            color = Color(0xFFF44336),
            topLeft = Offset(redX, redY),
            size = Size(tileSize, tileSize)
        )

        // A yellow 2x1 rectangle at tile (8, 4)
        val yellowX = (8f - camera.x) * tileSize
        val yellowY = (4f - camera.y) * tileSize
        drawRect(
            color = Color(0xFFFFEB3B),
            topLeft = Offset(yellowX, yellowY),
            size = Size(tileSize * 2f, tileSize)
        )

        if (showFps) {
            drawFpsText(
                fps = fps,
                textMeasurer = textMeasurer,
                canvasSize = size
            )
        }
    }
}
