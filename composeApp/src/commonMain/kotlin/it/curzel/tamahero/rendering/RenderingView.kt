package it.curzel.tamahero.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.models.Troop
import it.curzel.tamahero.village.FloatingText

data class GhostBuilding(
    val type: BuildingType,
    val gridX: Int,
    val gridY: Int,
    val isValid: Boolean,
)

@Composable
fun RenderingView(
    viewModel: GameViewModel,
    buildings: List<PlacedBuilding>,
    troops: List<Troop> = emptyList(),
    ghost: GhostBuilding? = null,
    selectedBuildingId: Long? = null,
    floatingTexts: List<FloatingText> = emptyList(),
) {
    val camera by viewModel.camera.collectAsState()
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
        drawRect(Color(0xFF0F0F0F), size = size)

        if (showGrid) {
            drawGrid(camera = camera, renderingScale = renderingScale)
        }

        drawBuildings(buildings, camera, renderingScale, selectedBuildingId)

        if (troops.isNotEmpty()) {
            drawTroops(troops, camera, renderingScale)
        }

        if (ghost != null) {
            drawBuildingGhost(ghost.type, ghost.gridX, ghost.gridY, ghost.isValid, camera, renderingScale)
        }

        if (floatingTexts.isNotEmpty()) {
            drawFloatingTexts(floatingTexts, textMeasurer, size)
        }

        if (showFps) {
            drawFpsText(fps = fps, textMeasurer = textMeasurer, canvasSize = size)
        }
    }
}
