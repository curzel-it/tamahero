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
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.village.*
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
    val army by villageViewModel.army.collectAsState()
    val trainingQueue by villageViewModel.trainingQueue.collectAsState()
    val troops by villageViewModel.troops.collectAsState()

    val floatingTexts by villageViewModel.floatingTexts.collectAsState()
    val offlineSummary by villageViewModel.offlineSummary.collectAsState()
    val errorMessage by villageViewModel.errorMessage.collectAsState()
    val selectedType by placementViewModel.selectedType.collectAsState()
    val ghostX by placementViewModel.ghostGridX.collectAsState()
    val ghostY by placementViewModel.ghostGridY.collectAsState()
    val isValidPlacement by placementViewModel.isValidPlacement.collectAsState()
    val selectedBuilding by selectionViewModel.selectedBuilding.collectAsState()
    val showGrid by viewModel.showGrid.collectAsState()
    val showFps by viewModel.showFps.collectAsState()
    var showBuildMenu by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var showWsLog by remember { mutableStateOf(false) }
    var showArmy by remember { mutableStateOf(false) }

    var showSettings by remember { mutableStateOf(false) }
    var hoverPos by remember { mutableStateOf<Offset?>(null) }
    var hoveredBuilding by remember { mutableStateOf<it.curzel.tamahero.models.PlacedBuilding?>(null) }

    val pvpViewModel = remember { PvpViewModel() }
    val pvpPhase by pvpViewModel.phase.collectAsState()
    val pvpMatch by pvpViewModel.match.collectAsState()
    val pvpBattle by pvpViewModel.battle.collectAsState()
    val pvpResult by pvpViewModel.result.collectAsState()
    val pvpError by pvpViewModel.error.collectAsState()
    val inBattle = pvpPhase == PvpPhase.Battling
    var selectedDeployTroop by remember { mutableStateOf<it.curzel.tamahero.models.TroopType?>(null) }

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
                        when {
                            showAccount -> { showAccount = false; true }
                            showArmy -> { showArmy = false; true }
                            showSettings -> { showSettings = false; true }
                            placementViewModel.isPlacing -> { placementViewModel.cancelPlacement(); true }
                            selectedBuilding != null -> { selectionViewModel.deselect(); true }
                            else -> false
                        }
                    } else {
                        viewModel.handleKeyEvent(event)
                    }
                }
                .onKeyEvent { event ->
                    viewModel.handleKeyEvent(event)
                }
                .onPointerEvent(PointerEventType.Move) { event ->
                    val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                    if (placementViewModel.isPlacing) {
                        placementViewModel.updateGhostPosition(pos.x, pos.y, viewModel.camera.value, renderingScale)
                    }
                    hoverPos = pos
                    val tileSize = it.curzel.tamahero.game.TILE_SIZE * renderingScale
                    val gridX = kotlin.math.floor(pos.x / tileSize + viewModel.camera.value.x).toInt()
                    val gridY = kotlin.math.floor(pos.y / tileSize + viewModel.camera.value.y).toInt()
                    hoveredBuilding = buildings.find { b ->
                        val bConfig = it.curzel.tamahero.models.BuildingConfig.configFor(b.type, b.level)
                        val bw = bConfig?.width ?: 2
                        val bh = bConfig?.height ?: 2
                        gridX >= b.x && gridX < b.x + bw && gridY >= b.y && gridY < b.y + bh
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
                                if (inBattle && selectedDeployTroop != null) {
                                    val tileSize = it.curzel.tamahero.game.TILE_SIZE * renderingScale
                                    val gx = (startPos.x / tileSize + viewModel.camera.value.x).toFloat()
                                    val gy = (startPos.y / tileSize + viewModel.camera.value.y).toFloat()
                                    pvpViewModel.deployTroop(selectedDeployTroop!!, gx, gy)
                                } else {
                                    selectionViewModel.selectAt(startPos.x, startPos.y, viewModel.camera.value, renderingScale)
                                }
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
                troops = troops,
                ghost = ghost,
                selectedBuildingId = selectedBuilding?.id,
                floatingTexts = floatingTexts,
            )
            if (!inBattle) {
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
                    onAccountClick = { showAccount = true },
                    onArmyClick = { showArmy = true },
                    onAttackClick = { pvpViewModel.findOpponent() },
                    onSettingsClick = { showSettings = true },
                    onCollectAllClick = { GameSocketClient.collectAll() },
                    onWsLogClick = { showWsLog = !showWsLog },
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }

            val currentBattle = pvpBattle
            if (inBattle && currentBattle != null) {
                PvpBattleHudView(
                    battle = currentBattle,
                    selectedTroop = selectedDeployTroop,
                    onSelectTroop = { troopType -> selectedDeployTroop = troopType },
                    onSurrender = { pvpViewModel.surrender() },
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }

            if (selectedBuilding != null && !placementViewModel.isPlacing && !showBuildMenu && !showAccount) {
                BuildingInfoView(
                    building = selectedBuilding!!,
                    resources = resources,
                    onDismiss = {
                        selectionViewModel.deselect()
                        focusRequester.requestFocus()
                    },
                    onMove = { building ->
                        selectionViewModel.deselect()
                        placementViewModel.startMove(building)
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

            if (showArmy) {
                ArmyOverviewView(
                    army = army,
                    trainingQueue = trainingQueue,
                    resources = resources,
                    buildings = buildings,
                    onDismiss = {
                        showArmy = false
                        focusRequester.requestFocus()
                    },
                )
            }

            if (showSettings) {
                SettingsView(
                    showGrid = showGrid,
                    showFps = showFps,
                    onToggleGrid = { viewModel.setShowGrid(it) },
                    onToggleFps = { viewModel.setShowFps(it) },
                    onDismiss = {
                        showSettings = false
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

            val currentMatch = pvpMatch
            if ((pvpPhase == PvpPhase.Scouting || pvpPhase == PvpPhase.Searching) && currentMatch != null) {
                PvpScoutView(
                    match = currentMatch,
                    searching = pvpPhase == PvpPhase.Searching,
                    error = pvpError,
                    onAttack = { pvpViewModel.startBattle() },
                    onNext = { pvpViewModel.nextOpponent() },
                    onDismiss = {
                        pvpViewModel.dismiss()
                        focusRequester.requestFocus()
                    },
                )
            }

            val currentResult = pvpResult
            if (pvpPhase == PvpPhase.Results && currentResult != null) {
                PvpResultView(
                    result = currentResult,
                    onDismiss = {
                        pvpViewModel.dismiss()
                        GameSocketClient.getVillage()
                        focusRequester.requestFocus()
                    },
                )
            }

            val hb = hoveredBuilding
            if (hb != null && selectedBuilding == null && !placementViewModel.isPlacing &&
                !showBuildMenu && !showArmy && !showSettings && !showAccount
            ) {
                BuildingTooltipView(
                    building = hb,
                    modifier = Modifier.align(Alignment.TopEnd)
                        .padding(top = it.curzel.tamahero.ui.theme.TamaSpacing.XLarge),
                )
            }

            val summary = offlineSummary
            if (summary != null) {
                OfflineSummaryView(
                    summary = summary,
                    onDismiss = {
                        villageViewModel.dismissOfflineSummary()
                        focusRequester.requestFocus()
                    },
                )
            }

            val error = errorMessage
            if (error != null) {
                ErrorToastView(
                    message = error,
                    onDismiss = { villageViewModel.dismissError() },
                    modifier = Modifier.align(Alignment.BottomCenter),
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
