package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.*
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.ui.theme.*

@Composable
fun ArmyOverviewView(
    army: Army,
    trainingQueue: TrainingQueue,
    resources: Resources,
    buildings: List<PlacedBuilding>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val capacity = buildings
        .filter { it.type == BuildingType.Hangar && it.constructionStartedAt == null }
        .sumOf { BuildingConfig.configFor(it.type, it.level)?.troopCapacity ?: 0 }
    val academyLevel = buildings
        .filter { it.type == BuildingType.Barracks && it.constructionStartedAt == null }
        .maxOfOrNull { it.level } ?: 0
    val hasBarracks = academyLevel > 0
    val maxTrainLevel = TroopConfig.maxLevelForAcademy(academyLevel)

    var selectedTroopInfo by remember { mutableStateOf<TroopType?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TamaColors.Background.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = TamaRadius.Large, topEnd = TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable { }
                .padding(TamaSpacing.Medium),
        ) {
            Text("Army (${army.totalCount}/$capacity)", color = TamaColors.Text, fontSize = 18.sp)
            Spacer(Modifier.height(TamaSpacing.Small))

            if (army.troops.isEmpty()) {
                Text("No troops", color = TamaColors.TextMuted, fontSize = 14.sp)
            } else {
                for (t in army.troops) {
                    Text(
                        "${t.type.name} Lv${t.level} x${t.count}",
                        color = TamaColors.Accent,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { selectedTroopInfo = t.type },
                    )
                }
            }

            if (trainingQueue.entries.isNotEmpty()) {
                Spacer(Modifier.height(TamaSpacing.Small))
                Text("Training Queue", color = TamaColors.Text, fontSize = 16.sp)
                Spacer(Modifier.height(TamaSpacing.XXSmall))
                for ((i, entry) in trainingQueue.entries.withIndex()) {
                    val status = if (entry.startedAt != null) {
                        val config = TroopConfig.configFor(entry.troopType, entry.level)
                        if (config != null) {
                            val elapsed = (kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - entry.startedAt!!).coerceAtLeast(0)
                            val remainingMs = (config.trainingTimeSeconds * 1000 - elapsed).coerceAtLeast(0)
                            val remainingSec = remainingMs / 1000
                            "Training ${remainingSec}s"
                        } else "Training"
                    } else "Queued"
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${entry.troopType.name} Lv${entry.level} [$status]", color = TamaColors.TextMuted, fontSize = 14.sp)
                        if (entry.startedAt == null) {
                            TamaGhostButton(text = "X", onClick = { GameSocketClient.cancelTraining(i) })
                        }
                    }
                }
            }

            if (hasBarracks) {
                Spacer(Modifier.height(TamaSpacing.Small))
                Text("Train", color = TamaColors.Text, fontSize = 16.sp)
                Spacer(Modifier.height(TamaSpacing.XXSmall))
                for (type in TroopType.entries) {
                    val config = TroopConfig.configFor(type, maxTrainLevel) ?: continue
                    val canAfford1 = resources.hasEnough(config.trainingCost)
                    val canAfford5 = resources.hasEnough(config.trainingCost * 5.0)
                    val atCapacity = army.totalCount >= capacity
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XXSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${type.name} L$maxTrainLevel",
                            color = TamaColors.Accent,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f).clickable { selectedTroopInfo = type },
                        )
                        TamaButton(
                            text = "+1",
                            color = if (canAfford1 && !atCapacity) TamaColors.Success else TamaColors.SurfaceElevated,
                            enabled = canAfford1 && !atCapacity,
                            onClick = { GameSocketClient.train(type, count = 1, level = maxTrainLevel) },
                        )
                        TamaButton(
                            text = "+5",
                            color = if (canAfford5 && !atCapacity) TamaColors.Success else TamaColors.SurfaceElevated,
                            enabled = canAfford5 && !atCapacity,
                            onClick = { GameSocketClient.train(type, count = 5, level = maxTrainLevel) },
                        )
                    }
                }
            }
        }

        val infoType = selectedTroopInfo
        if (infoType != null) {
            TroopInfoView(
                troopType = infoType,
                maxLevel = maxTrainLevel,
                onDismiss = { selectedTroopInfo = null },
            )
        }
    }
}
