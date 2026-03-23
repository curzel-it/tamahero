package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
    val hasBarracks = buildings.any { it.type == BuildingType.Academy && it.constructionStartedAt == null }

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
                .clickable(enabled = false, onClick = {})
                .padding(TamaSpacing.Medium),
        ) {
            Text("Army (${army.totalCount}/$capacity)", color = TamaColors.Text, fontSize = 18.sp)
            Spacer(Modifier.height(TamaSpacing.Small))

            if (army.troops.isEmpty()) {
                Text("No troops", color = TamaColors.TextMuted, fontSize = 14.sp)
            } else {
                for (t in army.troops) {
                    Text("${t.type.name} Lv${t.level} x${t.count}", color = TamaColors.Text, fontSize = 14.sp)
                }
            }

            if (trainingQueue.entries.isNotEmpty()) {
                Spacer(Modifier.height(TamaSpacing.Small))
                Text("Training Queue", color = TamaColors.Text, fontSize = 16.sp)
                Spacer(Modifier.height(TamaSpacing.XXSmall))
                for ((i, entry) in trainingQueue.entries.withIndex()) {
                    val status = if (entry.startedAt != null) "Training" else "Queued"
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${entry.troopType.name} [$status]", color = TamaColors.TextMuted, fontSize = 14.sp)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XXSmall),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    for (type in TroopType.entries) {
                        val config = TroopConfig.configFor(type, 1) ?: continue
                        val canAfford = resources.hasEnough(config.trainingCost)
                        val atCapacity = army.totalCount >= capacity
                        TamaButton(
                            text = type.name.take(6),
                            color = if (canAfford && !atCapacity) TamaColors.Success else TamaColors.SurfaceElevated,
                            enabled = canAfford && !atCapacity,
                            onClick = { GameSocketClient.train(type) },
                        )
                    }
                }
            }
        }
    }
}
