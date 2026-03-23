package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.models.Resources
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun BuildingInfoView(
    building: PlacedBuilding,
    resources: Resources,
    onDismiss: () -> Unit,
    onMove: (PlacedBuilding) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val config = BuildingConfig.configFor(building.type, building.level)
    val nextConfig = BuildingConfig.configFor(building.type, building.level + 1)
    val maxLevel = BuildingConfig.maxLevel(building.type)
    val isUnderConstruction = building.constructionStartedAt != null
    val isProducer = config?.productionPerHour?.let {
        it.gold > 0 || it.wood > 0 || it.metal > 0
    } ?: false

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = TamaRadius.Large, topEnd = TamaRadius.Large))
            .background(TamaColors.Surface)
            .padding(TamaSpacing.Medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${building.type.name} (Lv ${building.level})",
                color = TamaColors.Text,
                fontSize = 18.sp,
            )
            TamaGhostButton(text = "X", onClick = onDismiss)
        }

        Spacer(Modifier.height(TamaSpacing.XSmall))

        if (config != null) {
            Text("HP: ${building.hp}/${config.hp}", color = TamaColors.TextMuted, fontSize = 14.sp)
            if (isProducer) {
                val prod = config.productionPerHour
                val prodText = buildList {
                    if (prod.gold > 0) add("${prod.gold} gold")
                    if (prod.wood > 0) add("${prod.wood} wood")
                    if (prod.metal > 0) add("${prod.metal} metal")
                }.joinToString(", ")
                Text("Production: $prodText/hr", color = TamaColors.TextMuted, fontSize = 14.sp)
            }
            val storage = config.storageCapacity
            if (storage.gold > 0 || storage.wood > 0 || storage.metal > 0) {
                val storageText = buildList {
                    if (storage.gold > 0) add("${storage.gold} gold")
                    if (storage.wood > 0) add("${storage.wood} wood")
                    if (storage.metal > 0) add("${storage.metal} metal")
                }.joinToString(", ")
                Text("Storage: $storageText", color = TamaColors.TextMuted, fontSize = 14.sp)
            }
        }

        if (isUnderConstruction && config != null) {
            Spacer(Modifier.height(TamaSpacing.XXSmall))
            ConstructionProgress(building, config.buildTimeSeconds)
        }

        Spacer(Modifier.height(TamaSpacing.Small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall),
        ) {
            if (isProducer && !isUnderConstruction) {
                TamaButton(
                    text = "Collect",
                    color = TamaColors.Success,
                    onClick = { GameSocketClient.collect(building.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (building.type.isTrap && building.triggered) {
                TamaButton(
                    text = "Rearm",
                    color = TamaColors.Warning,
                    onClick = { GameSocketClient.rearmTrap(building.id) },
                    modifier = Modifier.weight(1f),
                )
                TamaButton(
                    text = "Rearm All",
                    color = TamaColors.Warning,
                    onClick = { GameSocketClient.rearmAllTraps() },
                    modifier = Modifier.weight(1f),
                )
            }
            if (nextConfig != null && building.level < maxLevel && !isUnderConstruction) {
                val canAfford = resources.hasEnough(nextConfig.cost)
                val costText = formatCost(nextConfig.cost)
                TamaButton(
                    text = "Upgrade ($costText)",
                    color = TamaColors.Info,
                    onClick = { if (canAfford) GameSocketClient.upgrade(building.id) },
                    enabled = canAfford,
                    modifier = Modifier.weight(1f),
                )
            }
            if (isUnderConstruction) {
                TamaButton(
                    text = "Speed Up",
                    color = TamaColors.Accent,
                    onClick = { GameSocketClient.speedUp(building.id) },
                    modifier = Modifier.weight(1f),
                )
                TamaDangerButton(
                    text = "Cancel",
                    onClick = {
                        GameSocketClient.cancelConstruction(building.id)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            if (!isUnderConstruction) {
                TamaButton(
                    text = "Move",
                    color = TamaColors.Info,
                    onClick = { onMove(building) },
                    modifier = Modifier.weight(1f),
                )
                TamaDangerButton(
                    text = "Demolish",
                    onClick = {
                        GameSocketClient.demolish(building.id)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ConstructionProgress(building: PlacedBuilding, buildTimeSeconds: Long) {
    val startedAt = building.constructionStartedAt ?: return
    val totalMs = buildTimeSeconds * 1000L
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(building.id) {
        while (true) {
            delay(500)
            now = System.currentTimeMillis()
        }
    }

    val elapsed = (now - startedAt).coerceAtLeast(0)
    val progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
    val remainingSeconds = ((totalMs - elapsed) / 1000).coerceAtLeast(0)
    val eta = if (remainingSeconds >= 60) {
        "${remainingSeconds / 60}m ${remainingSeconds % 60}s"
    } else {
        "${remainingSeconds}s"
    }

    Column {
        Text("Building... $eta remaining", color = TamaColors.Warning, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = TamaColors.Accent,
            trackColor = TamaColors.SurfaceElevated,
        )
    }
}

private fun formatCost(cost: Resources): String {
    return buildList {
        if (cost.gold > 0) add("${cost.gold}g")
        if (cost.wood > 0) add("${cost.wood}w")
        if (cost.metal > 0) add("${cost.metal}m")
    }.joinToString(" ")
}
