package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.Resources
import it.curzel.tamahero.ui.theme.*

@Composable
fun BuildMenuView(
    resources: Resources,
    onSelectBuilding: (BuildingType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buildableTypes = BuildingType.entries.filter { it != BuildingType.TownHall }

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
            Text("Build", color = TamaColors.Text, fontSize = 18.sp)
            Spacer(Modifier.height(TamaSpacing.Small))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall),
                verticalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall),
                modifier = Modifier.heightIn(max = 400.dp),
            ) {
                items(buildableTypes) { type ->
                    val config = BuildingConfig.configFor(type, 1)
                    val canAfford = config != null && resources.hasEnough(config.cost)
                    BuildMenuItem(
                        type = type,
                        cost = config?.cost ?: Resources(),
                        canAfford = canAfford,
                        onClick = { if (canAfford) onSelectBuilding(type) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BuildMenuItem(
    type: BuildingType,
    cost: Resources,
    canAfford: Boolean,
    onClick: () -> Unit,
) {
    val costText = buildList {
        if (cost.gold > 0) add("${cost.gold}g")
        if (cost.wood > 0) add("${cost.wood}w")
        if (cost.metal > 0) add("${cost.metal}m")
    }.joinToString(" ")

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(TamaRadius.Medium))
            .background(TamaColors.SurfaceElevated)
            .alpha(if (canAfford) 1f else 0.4f)
            .clickable(enabled = canAfford, onClick = onClick)
            .padding(TamaSpacing.Small)
            .heightIn(min = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(type.name, color = TamaColors.Text, fontSize = 14.sp)
        if (costText.isNotEmpty()) {
            Text(costText, color = TamaColors.TextMuted, fontSize = 12.sp)
        }
    }
}
