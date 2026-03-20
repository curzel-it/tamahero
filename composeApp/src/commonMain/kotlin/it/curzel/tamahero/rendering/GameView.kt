package it.curzel.tamahero.rendering

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import it.curzel.tamahero.game.GameTimer

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameView(
    viewModel: GameViewModel = remember { GameViewModel() },
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val renderingScale by viewModel.renderingScale.collectAsState()

    LaunchedEffect(viewModel) {
        focusRequester.requestFocus()
        viewModel.onRenderingStarted()
        GameTimer.add("GameViewModel", viewModel)
        GameTimer.start()
    }

    DisposableEffect(viewModel) {
        onDispose {
            GameTimer.remove("GameViewModel")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                viewModel.handleKeyEvent(event)
            }
            .onKeyEvent { event ->
                viewModel.handleKeyEvent(event)
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    viewModel.onCameraDrag(pan.x, pan.y)
                    if (zoomChange != 1f) {
                        viewModel.onZoom(zoomChange)
                    }
                }
            }
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                if (scrollDelta != 0f) {
                    val zoomFactor = if (scrollDelta > 0) 0.9f else 1.1f
                    viewModel.onZoom(zoomFactor)
                }
            }
    ) {
        RenderingView(viewModel = viewModel)
    }
}
