package it.curzel.tamahero.rendering

import androidx.compose.ui.input.key.*
import androidx.lifecycle.ViewModel
import it.curzel.tamahero.game.GameTimerItem
import it.curzel.tamahero.game.TILE_SIZE
import it.curzel.tamahero.utils.Vector2d
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class GameViewModel : ViewModel(), GameTimerItem {
    var camera: Vector2d = Vector2d.zero()
        private set

    private var frameCount: Int = 0
    private var fpsLastTime: Long = 0
    private var viewWidth: Float = 0f
    private var viewHeight: Float = 0f

    private val _renderingScale = MutableStateFlow(2f)
    val renderingScale = _renderingScale.asStateFlow()

    private val _totalRunTime = MutableStateFlow(0f)
    val totalRunTime = _totalRunTime.asStateFlow()

    private val _fps = MutableStateFlow(0f)
    val fps = _fps.asStateFlow()

    private val _showFps = MutableStateFlow(true)
    val showFps = _showFps.asStateFlow()

    private val _showGrid = MutableStateFlow(true)
    val showGrid = _showGrid.asStateFlow()

    fun onViewSizeChanged(width: Float, height: Float) {
        if (width <= 0 || height <= 0) return
        viewWidth = width
        viewHeight = height
        _renderingScale.value = RenderingScaleProviderHolder.instance.calculateScale(width, height)
    }

    fun onRenderingStarted() {
        fpsLastTime = Clock.System.now().toEpochMilliseconds()
        frameCount = 0
    }

    override fun onFrameTick(totalRunTime: Long, timeDelta: Long) {
        _totalRunTime.value = totalRunTime / 1000f
        updateFpsCounter()
    }

    private fun updateFpsCounter() {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        frameCount++
        val elapsed = currentTime - fpsLastTime
        if (elapsed >= 1000) {
            val currentFps = frameCount * 1000f / elapsed
            _fps.value = currentFps
            fpsLastTime = currentTime
            frameCount = 0
        }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        when (event.type) {
            KeyEventType.KeyDown -> {
                return when (event.key) {
                    Key.DirectionUp, Key.W -> {
                        camera = camera.offset(0f, -0.5f)
                        true
                    }
                    Key.DirectionDown, Key.S -> {
                        camera = camera.offset(0f, 0.5f)
                        true
                    }
                    Key.DirectionLeft, Key.A -> {
                        camera = camera.offset(-0.5f, 0f)
                        true
                    }
                    Key.DirectionRight, Key.D -> {
                        camera = camera.offset(0.5f, 0f)
                        true
                    }
                    Key.G -> {
                        _showGrid.value = !_showGrid.value
                        true
                    }
                    Key.F -> {
                        _showFps.value = !_showFps.value
                        true
                    }
                    Key.Equals, Key.Plus -> {
                        onZoom(1.1f)
                        true
                    }
                    Key.Minus -> {
                        onZoom(0.9f)
                        true
                    }
                    else -> false
                }
            }
            else -> return false
        }
    }

    fun onCameraDrag(dragX: Float, dragY: Float) {
        val zoom = _renderingScale.value
        val tileSize = TILE_SIZE * zoom
        val tileDragX = -dragX / tileSize
        val tileDragY = -dragY / tileSize
        camera = camera.offset(tileDragX, tileDragY)
    }

    fun onZoom(zoomFactor: Float) {
        val oldScale = _renderingScale.value
        val newScale = (oldScale * zoomFactor).coerceIn(0.5f, 8f)
        if (oldScale == newScale) return
        _renderingScale.value = newScale
    }
}
