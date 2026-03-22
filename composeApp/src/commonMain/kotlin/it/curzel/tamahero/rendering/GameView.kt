package it.curzel.tamahero.rendering

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import it.curzel.tamahero.game.GameTimer
import it.curzel.tamahero.village.BuildMenuView
import it.curzel.tamahero.village.BuildPlacementViewModel
import it.curzel.tamahero.village.GameHudView
import it.curzel.tamahero.village.VillageViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameView(
    viewModel: GameViewModel = remember { GameViewModel() },
    villageViewModel: VillageViewModel = remember { VillageViewModel() },
    placementViewModel: BuildPlacementViewModel = remember { BuildPlacementViewModel() },
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val renderingScale by viewModel.renderingScale.collectAsState()
    val buildings by villageViewModel.buildings.collectAsState()
    val resources by villageViewModel.resources.collectAsState()
    val selectedType by placementViewModel.selectedType.collectAsState()
    val ghostX by placementViewModel.ghostGridX.collectAsState()
    val ghostY by placementViewModel.ghostGridY.collectAsState()
    val isValidPlacement by placementViewModel.isValidPlacement.collectAsState()
    var showBuildMenu by remember { mutableStateOf(false) }

    LaunchedEffect(buildings) {
        placementViewModel.updateBuildings(buildings)
    }

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

    val ghost = if (selectedType != null) {
        GhostBuilding(type = selectedType!!, gridX = ghostX, gridY = ghostY, isValid = isValidPlacement)
    } else null

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && placementViewModel.isPlacing) {
                    placementViewModel.cancelPlacement()
                    true
                } else {
                    viewModel.handleKeyEvent(event)
                }
            }
            .onKeyEvent { event ->
                viewModel.handleKeyEvent(event)
            }
            .onPointerEvent(PointerEventType.Move) { event ->
                if (placementViewModel.isPlacing) {
                    val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                    placementViewModel.updateGhostPosition(pos.x, pos.y, viewModel.camera, renderingScale)
                }
            }
            .pointerInput(selectedType) {
                if (selectedType != null) {
                    detectTapGestures { offset ->
                        placementViewModel.updateGhostPosition(offset.x, offset.y, viewModel.camera, renderingScale)
                        placementViewModel.confirmPlacement()
                        focusRequester.requestFocus()
                    }
                } else {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        viewModel.onCameraDrag(pan.x, pan.y)
                        if (zoomChange != 1f) {
                            viewModel.onZoom(zoomChange)
                        }
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
        RenderingView(viewModel = viewModel, buildings = buildings, ghost = ghost)
        GameHudView(
            resources = resources,
            isPlacing = placementViewModel.isPlacing,
            onBuildClick = {
                showBuildMenu = true
                focusRequester.requestFocus()
            },
            onCancelClick = {
                placementViewModel.cancelPlacement()
                focusRequester.requestFocus()
            },
            modifier = Modifier.align(Alignment.TopStart),
        )

        if (showBuildMenu) {
            BuildMenuView(
                resources = resources,
                onSelectBuilding = { type ->
                    showBuildMenu = false
                    placementViewModel.startPlacement(type)
                    focusRequester.requestFocus()
                },
                onDismiss = {
                    showBuildMenu = false
                    focusRequester.requestFocus()
                },
            )
        }
    }
}
