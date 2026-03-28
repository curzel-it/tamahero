package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.BuildingConfig
import it.curzel.tamahero.models.PlacedBuilding
import it.curzel.tamahero.ui.theme.*

@Composable
fun BuildingTooltipView(
    building: PlacedBuilding,
    modifier: Modifier = Modifier,
) {
    val config = BuildingConfig.configFor(building.type, building.level)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(TamaRadius.Small))
            .background(TamaColors.Surface.copy(alpha = 0.95f))
            .padding(horizontal = TamaSpacing.Small, vertical = TamaSpacing.XXSmall),
    ) {
        Text(
            "${building.type.displayName} Lv${building.level}",
            color = TamaColors.Text,
            fontSize = 12.sp,
        )
        if (config != null) {
            Text(
                "HP: ${building.hp}/${config.hp}",
                color = TamaColors.TextMuted,
                fontSize = 11.sp,
            )
            val prod = config.productionPerHour
            if (prod.credits > 0 || prod.metal > 0 || prod.crystal > 0 || prod.deuterium > 0) {
                val prodText = buildList {
                    if (prod.credits > 0) add("${prod.credits}cr")
                    if (prod.metal > 0) add("${prod.metal}m")
                    if (prod.crystal > 0) add("${prod.crystal}c")
                    if (prod.deuterium > 0) add("${prod.deuterium}dt")
                }.joinToString(" ")
                Text(prodText + "/hr", color = TamaColors.TextMuted, fontSize = 11.sp)
            }
            if (config.damage > 0) {
                Text("DMG: ${config.damage}", color = TamaColors.TextMuted, fontSize = 11.sp)
            }
        }
    }
}
