package it.curzel.tamahero.rendering

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import it.curzel.tamahero.ui.theme.TamaSpacing
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import it.curzel.tamahero.auth.AccountView
import it.curzel.tamahero.game.GameTimer
import it.curzel.tamahero.village.BuildMenuView
import it.curzel.tamahero.village.BuildPlacementViewModel
import it.curzel.tamahero.village.BuildingInfoView
import it.curzel.tamahero.village.BuildingSelectionViewModel
import it.curzel.tamahero.village.GameHudView
import it.curzel.tamahero.village.VillageViewModel
import it.curzel.tamahero.village.WsDebugPanel
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameView(
    username: String = "",
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    viewModel: GameViewModel = remember { GameViewModel() },
    villageViewModel: VillageViewModel = remember { VillageViewModel() },
    placementViewModel: BuildPlacementViewModel = remember { BuildPlacementViewModel() },
    selectionViewModel: BuildingSelectionViewModel = remember { BuildingSelectionViewModel() },
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
    val selectedBuilding by selectionViewModel.selectedBuilding.collectAsState()
    var showBuildMenu by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var showWsLog by remember { mutableStateOf(false) }

    LaunchedEffect(buildings) {
        placementViewModel.updateBuildings(buildings)
        selectionViewModel.updateBuildings(buildings)
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

    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        if (showAccount) {
                            showAccount = false
                            true
                        } else if (placementViewModel.isPlacing) {
                            placementViewModel.cancelPlacement()
                            true
                        } else if (selectedBuilding != null) {
                            selectionViewModel.deselect()
                            true
                        } else {
                            false
                        }
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
                        placementViewModel.updateGhostPosition(pos.x, pos.y, viewModel.camera.value, renderingScale)
                    }
                }
                .pointerInput(selectedType) {
                    if (selectedType != null) {
                        detectTapGestures { offset ->
                            placementViewModel.updateGhostPosition(offset.x, offset.y, viewModel.camera.value, renderingScale)
                            placementViewModel.confirmPlacement()
                            focusRequester.requestFocus()
                        }
                    } else {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            var totalDrag = Offset.Zero
                            var isTap = true

                            do {
                                val event = awaitPointerEvent()
                                val changes = event.changes

                                if (changes.size >= 2) {
                                    isTap = false
                                    val first = changes[0]
                                    val second = changes[1]
                                    val prevDist = (first.previousPosition - second.previousPosition).getDistance()
                                    val currDist = (first.position - second.position).getDistance()
                                    if (prevDist > 0f) {
                                        val zoomChange = currDist / prevDist
                                        if (zoomChange != 1f) {
                                            viewModel.onZoom(zoomChange)
                                        }
                                    }
                                    val avgDrag = Offset(
                                        (first.position.x - first.previousPosition.x + second.position.x - second.previousPosition.x) / 2f,
                                        (first.position.y - first.previousPosition.y + second.position.y - second.previousPosition.y) / 2f,
                                    )
                                    if (avgDrag != Offset.Zero) {
                                        viewModel.onCameraDrag(avgDrag.x, avgDrag.y)
                                    }
                                    changes.forEach { it.consume() }
                                } else if (changes.size == 1) {
                                    val change = changes[0]
                                    if (change.positionChanged()) {
                                        val drag = change.position - change.previousPosition
                                        totalDrag += drag
                                        if (abs(totalDrag.x) > 10f || abs(totalDrag.y) > 10f) {
                                            isTap = false
                                        }
                                        if (!isTap) {
                                            viewModel.onCameraDrag(drag.x, drag.y)
                                        }
                                        change.consume()
                                    }
                                }
                            } while (changes.any { it.pressed })

                            if (isTap) {
                                selectionViewModel.selectAt(startPos.x, startPos.y, viewModel.camera.value, renderingScale)
                                focusRequester.requestFocus()
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
            RenderingView(
                viewModel = viewModel,
                buildings = buildings,
                ghost = ghost,
                selectedBuildingId = selectedBuilding?.id,
            )
            GameHudView(
                resources = resources,
                isPlacing = placementViewModel.isPlacing,
                onBuildClick = {
                    selectionViewModel.deselect()
                    showBuildMenu = true
                    focusRequester.requestFocus()
                },
                onCancelClick = {
                    placementViewModel.cancelPlacement()
                    focusRequester.requestFocus()
                },
                onAccountClick = {
                    showAccount = true
                },
                onWsLogClick = {
                    showWsLog = !showWsLog
                },
                modifier = Modifier.align(Alignment.TopStart),
            )

            if (selectedBuilding != null && !placementViewModel.isPlacing && !showBuildMenu && !showAccount) {
                BuildingInfoView(
                    building = selectedBuilding!!,
                    resources = resources,
                    onDismiss = {
                        selectionViewModel.deselect()
                        focusRequester.requestFocus()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (showBuildMenu) {
                BuildMenuView(
                    resources = resources,
                    buildings = buildings,
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

            if (showAccount) {
                AccountView(
                    username = username,
                    onLogout = onLogout,
                    onDeleteAccount = onDeleteAccount,
                    onDismiss = {
                        showAccount = false
                        focusRequester.requestFocus()
                    },
                )
            }
        }

        if (showWsLog) {
            WsDebugPanel(
                onClose = { showWsLog = false },
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}
