package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.BuildingType
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.models.Resources
import it.curzel.tamahero.ui.theme.*

private enum class BuildCategory(val label: String) {
    Resources("Resources"),
    Army("Army"),
    Defense("Defense"),
    Traps("Traps"),
}

private enum class BuildItemState { Buyable, TooExpensive, MaxedOut, Locked }

@Composable
fun BuildMenuView(
    resources: Resources,
    buildings: List<PlacedBuilding>,
    onSelectBuilding: (BuildingType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(BuildCategory.Resources) }
    val townHallLevel = buildings
        .filter { it.type == BuildingType.TownHall && it.constructionStartedAt == null }
        .maxOfOrNull { it.level } ?: 1

    val allTypes = BuildingType.entries.filter { it != BuildingType.TownHall }
    val categorized = allTypes.groupBy { categoryFor(it) }
    val currentItems = (categorized[selectedTab] ?: emptyList()).sortedWith(
        compareBy<BuildingType> { stateFor(it, resources, townHallLevel, buildings).ordinal }
            .thenBy { it.name }
    )

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XXSmall),
            ) {
                for (tab in BuildCategory.entries) {
                    val isSelected = tab == selectedTab
                    val count = categorized[tab]?.size ?: 0
                    Text(
                        "${tab.label} ($count)",
                        color = if (isSelected) TamaColors.Accent else TamaColors.TextMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(TamaRadius.Small))
                            .background(if (isSelected) TamaColors.SurfaceElevated else TamaColors.Surface)
                            .clickable { selectedTab = tab }
                            .padding(horizontal = TamaSpacing.Small, vertical = TamaSpacing.XXSmall),
                    )
                }
            }

            Spacer(Modifier.height(TamaSpacing.Small))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall),
                verticalArrangement = Arrangement.spacedBy(TamaSpacing.XSmall),
                modifier = Modifier.heightIn(max = 400.dp),
            ) {
                items(currentItems) { type ->
                    val config = BuildingConfig.configFor(type, 1)
                    val itemState = stateFor(type, resources, townHallLevel, buildings)
                    val currentCount = buildings.count { it.type == type }
                    val maxCount = BuildingConfig.maxCount(type, townHallLevel)
                    BuildMenuItem(
                        type = type,
                        cost = config?.cost ?: Resources(),
                        state = itemState,
                        requiredTh = config?.requiredTownHallLevel ?: 1,
                        currentCount = currentCount,
                        maxCount = maxCount,
                        onClick = {
                            if (itemState == BuildItemState.Buyable) onSelectBuilding(type)
                        },
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
    state: BuildItemState,
    requiredTh: Int,
    currentCount: Int,
    maxCount: Int?,
    onClick: () -> Unit,
) {
    val costText = buildList {
        if (cost.gold > 0) add("${cost.gold}g")
        if (cost.wood > 0) add("${cost.wood}w")
        if (cost.metal > 0) add("${cost.metal}m")
    }.joinToString(" ")

    val borderColor = when (state) {
        BuildItemState.Buyable -> TamaColors.Success
        BuildItemState.TooExpensive -> TamaColors.Warning
        BuildItemState.MaxedOut -> TamaColors.Info
        BuildItemState.Locked -> TamaColors.TextMuted
    }
    val alpha = when (state) {
        BuildItemState.Buyable -> 1f
        BuildItemState.TooExpensive -> 0.6f
        BuildItemState.MaxedOut -> 0.5f
        BuildItemState.Locked -> 0.3f
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(TamaRadius.Medium))
            .border(1.dp, borderColor.copy(alpha = alpha), RoundedCornerShape(TamaRadius.Medium))
            .background(TamaColors.SurfaceElevated)
            .alpha(alpha)
            .clickable(enabled = state == BuildItemState.Buyable, onClick = onClick)
            .padding(TamaSpacing.Small)
            .heightIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(type.name, color = TamaColors.Text, fontSize = 14.sp)

        if (maxCount != null) {
            Text(
                "$currentCount / $maxCount",
                color = if (state == BuildItemState.MaxedOut) TamaColors.Info else TamaColors.TextMuted,
                fontSize = 11.sp,
            )
        }

        when (state) {
            BuildItemState.Locked -> {
                Text("TH $requiredTh", color = TamaColors.TextMuted, fontSize = 12.sp)
            }
            BuildItemState.MaxedOut -> {
                Text("Max", color = TamaColors.Info, fontSize = 12.sp)
            }
            else -> {
                if (costText.isNotEmpty()) {
                    Text(
                        costText,
                        color = if (state == BuildItemState.Buyable) TamaColors.Success else TamaColors.Warning,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

private fun categoryFor(type: BuildingType): BuildCategory = when {
    type.isProducer || type.isStorage -> BuildCategory.Resources
    type == BuildingType.Barracks || type == BuildingType.ArmyCamp -> BuildCategory.Army
    type.isDefense -> BuildCategory.Defense
    type.isTrap || type == BuildingType.ShieldDome -> BuildCategory.Traps
    else -> BuildCategory.Resources
}

private fun stateFor(
    type: BuildingType,
    resources: Resources,
    townHallLevel: Int,
    buildings: List<PlacedBuilding>,
): BuildItemState {
    val config = BuildingConfig.configFor(type, 1) ?: return BuildItemState.Locked
    if (config.requiredTownHallLevel > townHallLevel) return BuildItemState.Locked
    val maxCount = BuildingConfig.maxCount(type, townHallLevel)
    if (maxCount != null && buildings.count { it.type == type } >= maxCount) return BuildItemState.MaxedOut
    if (!resources.hasEnough(config.cost)) return BuildItemState.TooExpensive
    return BuildItemState.Buyable
}
