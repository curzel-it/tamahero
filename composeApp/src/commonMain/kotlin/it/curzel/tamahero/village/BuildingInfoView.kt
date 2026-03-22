package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.models.Resources
import it.curzel.tamahero.network.GameSocketClient
import it.curzel.tamahero.ui.theme.*

@Composable
fun BuildingInfoView(
    building: PlacedBuilding,
    resources: Resources,
    onDismiss: () -> Unit,
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

        if (isUnderConstruction) {
            Spacer(Modifier.height(TamaSpacing.XXSmall))
            Text("Under construction...", color = TamaColors.Warning, fontSize = 14.sp)
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

private fun formatCost(cost: Resources): String {
    return buildList {
        if (cost.gold > 0) add("${cost.gold}g")
        if (cost.wood > 0) add("${cost.wood}w")
        if (cost.metal > 0) add("${cost.metal}m")
    }.joinToString(" ")
}
