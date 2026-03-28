package it.curzel.tamahero.village

import androidx.lifecycle.ViewModel
import it.curzel.tamahero.game.TILE_SIZE
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.models.Resources
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.utils.Vector2d
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.floor

class BuildPlacementViewModel : ViewModel() {

    private val _selectedType = MutableStateFlow<BuildingType?>(null)
    val selectedType = _selectedType.asStateFlow()

    private val _ghostGridX = MutableStateFlow(0)
    val ghostGridX = _ghostGridX.asStateFlow()

    private val _ghostGridY = MutableStateFlow(0)
    val ghostGridY = _ghostGridY.asStateFlow()

    private val _isValidPlacement = MutableStateFlow(false)
    val isValidPlacement = _isValidPlacement.asStateFlow()

    private var currentBuildings: List<PlacedBuilding> = emptyList()
    private var movingBuildingId: Long? = null

    val isPlacing: Boolean get() = _selectedType.value != null

    fun startPlacement(type: BuildingType) {
        movingBuildingId = null
        _selectedType.value = type
    }

    fun startMove(building: PlacedBuilding) {
        movingBuildingId = building.id
        _selectedType.value = building.type
    }

    fun cancelPlacement() {
        _selectedType.value = null
        _isValidPlacement.value = false
        movingBuildingId = null
    }

    fun updateBuildings(buildings: List<PlacedBuilding>) {
        currentBuildings = buildings
        revalidate()
    }

    fun updateGhostPosition(screenX: Float, screenY: Float, camera: Vector2d, renderingScale: Float) {
        if (_selectedType.value == null) return
        val tileSize = TILE_SIZE * renderingScale
        val gridX = floor(screenX / tileSize + camera.x).toInt()
        val gridY = floor(screenY / tileSize + camera.y).toInt()
        _ghostGridX.value = gridX
        _ghostGridY.value = gridY
        revalidate()
    }

    fun confirmPlacement() {
        val type = _selectedType.value ?: return
        if (!_isValidPlacement.value) return
        val moveId = movingBuildingId
        if (moveId != null) {
            GameSocketClient.move(moveId, _ghostGridX.value, _ghostGridY.value)
        } else {
            GameSocketClient.build(type, _ghostGridX.value, _ghostGridY.value)
        }
        _selectedType.value = null
        _isValidPlacement.value = false
        movingBuildingId = null
    }

    fun canAfford(type: BuildingType, resources: Resources): Boolean {
        val config = BuildingConfig.configFor(type, 1) ?: return false
        return resources.hasEnough(config.cost)
    }

    private fun revalidate() {
        val type = _selectedType.value
        if (type == null) {
            _isValidPlacement.value = false
            return
        }
        val config = BuildingConfig.configFor(type, 1)
        if (config == null) {
            _isValidPlacement.value = false
            return
        }
        val gx = _ghostGridX.value
        val gy = _ghostGridY.value
        val w = config.width
        val h = config.height

        if (gx < 0 || gy < 0 || gx + w > GRID_SIZE || gy + h > GRID_SIZE) {
            _isValidPlacement.value = false
            return
        }

        for (building in currentBuildings) {
            if (building.id == movingBuildingId) continue
            val bConfig = BuildingConfig.configFor(building.type, building.level)
            val bw = bConfig?.width ?: 2
            val bh = bConfig?.height ?: 2
            if (gx < building.x + bw && gx + w > building.x &&
                gy < building.y + bh && gy + h > building.y
            ) {
                _isValidPlacement.value = false
                return
            }
        }
        _isValidPlacement.value = true
    }

    companion object {
        private const val GRID_SIZE = 20
    }
}
