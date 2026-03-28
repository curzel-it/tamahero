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
    var confirmDemolish by remember { mutableStateOf(false) }
    val config = BuildingConfig.configFor(building.type, building.level)
    val nextConfig = BuildingConfig.configFor(building.type, building.level + 1)
    val maxLevel = BuildingConfig.maxLevel(building.type)
    val isUnderConstruction = building.constructionStartedAt != null
    val isProducer = config?.productionPerHour?.let {
        it.credits > 0 || it.metal > 0 || it.crystal > 0
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
                "${building.type.displayName} (Lv ${building.level})",
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
                    if (prod.credits > 0) add("${prod.credits} credits")
                    if (prod.metal > 0) add("${prod.metal} alloy")
                    if (prod.crystal > 0) add("${prod.crystal} crystal")
                }.joinToString(", ")
                Text("Production: $prodText/hr", color = TamaColors.TextMuted, fontSize = 14.sp)
            }
            val storage = config.storageCapacity
            if (storage.credits > 0 || storage.metal > 0 || storage.crystal > 0) {
                val storageText = buildList {
                    if (storage.credits > 0) add("${storage.credits} credits")
                    if (storage.metal > 0) add("${storage.metal} alloy")
                    if (storage.crystal > 0) add("${storage.crystal} crystal")
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
                if (confirmDemolish) {
                    TamaDangerButton(
                        text = "Confirm",
                        onClick = {
                            GameSocketClient.demolish(building.id)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TamaSecondaryButton(
                        text = "Cancel",
                        onClick = { confirmDemolish = false },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    TamaDangerButton(
                        text = "Demolish",
                        onClick = { confirmDemolish = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (config != null && nextConfig != null && !isUnderConstruction && building.level < maxLevel) {
            Spacer(Modifier.height(TamaSpacing.XXSmall))
            Text("Lv${building.level} \u2192 Lv${building.level + 1}", color = TamaColors.TextMuted, fontSize = 13.sp)
            UpgradeStatDiff("HP", config.hp.toLong(), nextConfig.hp.toLong())
            if (config.productionPerHour.credits > 0 || nextConfig.productionPerHour.credits > 0)
                UpgradeStatDiff("Credits/hr", config.productionPerHour.credits, nextConfig.productionPerHour.credits)
            if (config.productionPerHour.metal > 0 || nextConfig.productionPerHour.metal > 0)
                UpgradeStatDiff("Alloy/hr", config.productionPerHour.metal, nextConfig.productionPerHour.metal)
            if (config.productionPerHour.crystal > 0 || nextConfig.productionPerHour.crystal > 0)
                UpgradeStatDiff("Crystal/hr", config.productionPerHour.crystal, nextConfig.productionPerHour.crystal)
            if (config.storageCapacity.credits > 0 || nextConfig.storageCapacity.credits > 0)
                UpgradeStatDiff("Credit storage", config.storageCapacity.credits, nextConfig.storageCapacity.credits)
            if (config.storageCapacity.metal > 0 || nextConfig.storageCapacity.metal > 0)
                UpgradeStatDiff("Alloy storage", config.storageCapacity.metal, nextConfig.storageCapacity.metal)
            if (config.storageCapacity.crystal > 0 || nextConfig.storageCapacity.crystal > 0)
                UpgradeStatDiff("Crystal storage", config.storageCapacity.crystal, nextConfig.storageCapacity.crystal)
            if (config.damage > 0 || nextConfig.damage > 0)
                UpgradeStatDiff("Damage", config.damage.toLong(), nextConfig.damage.toLong())
            if (config.range > 0f || nextConfig.range > 0f)
                UpgradeStatDiffFloat("Range", config.range, nextConfig.range)
            if (config.troopCapacity > 0 || nextConfig.troopCapacity > 0)
                UpgradeStatDiff("Troop capacity", config.troopCapacity.toLong(), nextConfig.troopCapacity.toLong())
        }
    }
}

@Composable
private fun ConstructionProgress(building: PlacedBuilding, buildTimeSeconds: Long) {
    val startedAt = building.constructionStartedAt ?: return
    val totalMs = buildTimeSeconds * 1000L
    var now by remember { mutableStateOf(kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) }

    LaunchedEffect(building.id) {
        while (true) {
            delay(500)
            now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
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
        if (cost.credits > 0) add("${cost.credits}cr")
        if (cost.metal > 0) add("${cost.metal}m")
        if (cost.crystal > 0) add("${cost.crystal}c")
    }.joinToString(" ")
}

@Composable
private fun UpgradeStatDiff(label: String, current: Long, next: Long) {
    if (current == next) return
    val delta = next - current
    val sign = if (delta > 0) "+" else ""
    Text(
        "$label: $current \u2192 $next ($sign$delta)",
        color = if (delta > 0) TamaColors.Success else TamaColors.Error,
        fontSize = 12.sp,
    )
}

@Composable
private fun UpgradeStatDiffFloat(label: String, current: Float, next: Float) {
    if (current == next) return
    val delta = next - current
    val sign = if (delta > 0) "+" else ""
    Text(
        "$label: ${formatOneDecimal(current)} \u2192 ${formatOneDecimal(next)} ($sign${formatOneDecimal(delta)})",
        color = if (delta > 0) TamaColors.Success else TamaColors.Error,
        fontSize = 12.sp,
    )
}
