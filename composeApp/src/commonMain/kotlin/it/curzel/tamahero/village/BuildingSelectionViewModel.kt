package it.curzel.tamahero.village

import androidx.lifecycle.ViewModel
import it.curzel.tamahero.game.TILE_SIZE
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.utils.Vector2d
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.floor

class BuildingSelectionViewModel : ViewModel() {

    private val _selectedBuilding = MutableStateFlow<PlacedBuilding?>(null)
    val selectedBuilding = _selectedBuilding.asStateFlow()

    private var currentBuildings: List<PlacedBuilding> = emptyList()

    fun updateBuildings(buildings: List<PlacedBuilding>) {
        currentBuildings = buildings
        val selected = _selectedBuilding.value ?: return
        _selectedBuilding.value = buildings.find { it.id == selected.id }
    }

    fun selectAt(screenX: Float, screenY: Float, camera: Vector2d, renderingScale: Float) {
        val tileSize = TILE_SIZE * renderingScale
        val gridX = floor(screenX / tileSize + camera.x).toInt()
        val gridY = floor(screenY / tileSize + camera.y).toInt()

        _selectedBuilding.value = currentBuildings.find { building ->
            val config = BuildingConfig.configFor(building.type, building.level)
            val bw = config?.width ?: 2
            val bh = config?.height ?: 2
            gridX >= building.x && gridX < building.x + bw &&
                gridY >= building.y && gridY < building.y + bh
        }
    }

    fun deselect() {
        _selectedBuilding.value = null
    }
}
