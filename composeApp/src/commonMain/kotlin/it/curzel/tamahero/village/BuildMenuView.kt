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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.Resources

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
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color(0xFF1A1A2E))
                .clickable(enabled = false, onClick = {})
                .padding(16.dp),
        ) {
            Text("Build", color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp),
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
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF16213E))
            .alpha(if (canAfford) 1f else 0.4f)
            .clickable(enabled = canAfford, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(type.name, color = Color.White, fontSize = 13.sp)
        if (costText.isNotEmpty()) {
            Text(costText, color = Color(0xFFCCCCCC), fontSize = 11.sp)
        }
    }
}
