package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.models.Resources
import it.curzel.tamahero.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GameHudView(
    resources: Resources,
    buildings: List<PlacedBuilding>,
    shieldExpiresAt: Long,
    isPlacing: Boolean,
    onBuildClick: () -> Unit,
    onCancelClick: () -> Unit,
    onAccountClick: () -> Unit,
    onArmyClick: () -> Unit = {},
    onAttackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCollectAllClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onDefenseLogClick: () -> Unit = {},
    onWsLogClick: () -> Unit = {},
    trophies: Int = 0,
    modifier: Modifier = Modifier,
) {
    val maxResources = remember(buildings) { totalStorageCapacity(buildings) }
    val activeConstructions = remember(buildings) { buildings.count { it.constructionStartedAt != null } }
    val workerCount = remember(buildings) {
        buildings.count { it.type == BuildingType.RoboticsFactory && it.constructionStartedAt == null }.coerceAtLeast(1)
    }

    Column(
        modifier = modifier
            .background(TamaColors.Background.copy(alpha = 0.8f))
            .padding(horizontal = TamaSpacing.Small, vertical = TamaSpacing.XSmall),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TamaSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResourceText("Metal", resources.metal, maxResources.metal, TamaColors.Alloy)
            ResourceText("Crystal", resources.crystal, maxResources.crystal, TamaColors.Crystal)
            ResourceText("Deuterium", resources.deuterium, maxResources.deuterium, TamaColors.Plasma)
            ResourceText("Credits", resources.credits, 0, TamaColors.Credits)
        }
        Spacer(Modifier.height(TamaSpacing.XXSmall))
        Row(
            horizontalArrangement = Arrangement.spacedBy(TamaSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Builders: $activeConstructions/$workerCount",
                color = if (activeConstructions >= workerCount) TamaColors.Warning else TamaColors.TextMuted,
                fontSize = 12.sp,
            )
            Text("Trophies: $trophies", color = TamaColors.Warning, fontSize = 12.sp)
            ShieldTimerText(shieldExpiresAt)
        }
        Spacer(Modifier.height(TamaSpacing.XXSmall))
        Row(
            horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall),
        ) {
            if (isPlacing) {
                TamaDangerButton(text = "Cancel", onClick = onCancelClick)
            } else {
                TamaButton(
                    text = "Build",
                    onClick = onBuildClick,
                    color = if (activeConstructions < workerCount) TamaColors.Success else TamaColors.SurfaceElevated,
                    enabled = activeConstructions < workerCount,
                )
                TamaSecondaryButton(text = "Collect", onClick = onCollectAllClick)
                TamaSecondaryButton(text = "Army", onClick = onArmyClick)
                TamaDangerButton(text = "Attack", onClick = onAttackClick)
                TamaSecondaryButton(text = "Ranks", onClick = onLeaderboardClick)
                TamaSecondaryButton(text = "Log", onClick = onDefenseLogClick)
            }
            TamaSecondaryButton(text = "Account", onClick = onAccountClick)
            TamaSecondaryButton(text = "Settings", onClick = onSettingsClick)
            TamaSecondaryButton(text = "WS", onClick = onWsLogClick)
        }
    }
}

@Composable
private fun ResourceText(label: String, current: Long, max: Long, color: Color) {
    val ratio = if (max > 0) current.toFloat() / max else 0f
    val displayColor = when {
        ratio >= 0.95f -> TamaColors.Error
        ratio >= 0.80f -> TamaColors.Warning
        else -> color
    }
    val text = if (max > 0) "$label: $current/$max" else "$label: $current"
    Text(text, color = displayColor, fontSize = 14.sp)
}

@Composable
private fun ShieldTimerText(shieldExpiresAt: Long) {
    var now by remember { mutableStateOf(kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        }
    }
    val remainingMs = shieldExpiresAt - now
    if (remainingMs > 0) {
        val hours = remainingMs / 3_600_000
        val minutes = (remainingMs % 3_600_000) / 60_000
        Text("Shield: ${hours}h ${minutes}m", color = TamaColors.Info, fontSize = 12.sp)
    }
}

private fun totalStorageCapacity(buildings: List<PlacedBuilding>): Resources {
    var capacity = Resources()
    for (building in buildings) {
        if (building.constructionStartedAt != null) continue
        val config = BuildingConfig.configFor(building.type, building.level) ?: continue
        capacity = capacity + config.storageCapacity
    }
    return capacity
}
