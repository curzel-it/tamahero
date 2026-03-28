package it.curzel.tamahero.village

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.TroopConfig
import it.curzel.tamahero.models.TroopType
import it.curzel.tamahero.ui.theme.*

@Composable
fun TroopInfoView(
    troopType: TroopType,
    maxLevel: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TamaColors.Background.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(360.dp)
                .clip(RoundedCornerShape(TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable(enabled = false, onClick = {})
                .padding(TamaSpacing.Large),
        ) {
            Text(troopType.name, color = TamaColors.Text, fontSize = 20.sp)
            Spacer(Modifier.height(TamaSpacing.Small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Lv", color = TamaColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                Text("HP", color = TamaColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                Text("DPS", color = TamaColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(40.dp))
                Text("Spd", color = TamaColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(36.dp))
                Text("Rng", color = TamaColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(36.dp))
                Text("Cost", color = TamaColors.TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(TamaSpacing.XXSmall))

            for (level in 1..6) {
                val config = TroopConfig.configFor(troopType, level) ?: continue
                val isAvailable = level <= maxLevel
                val textColor = if (isAvailable) TamaColors.Text else TamaColors.TextMuted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("$level", color = textColor, fontSize = 13.sp, modifier = Modifier.width(24.dp))
                    Text("${config.hp}", color = textColor, fontSize = 13.sp, modifier = Modifier.width(48.dp))
                    Text("${config.dps}", color = textColor, fontSize = 13.sp, modifier = Modifier.width(40.dp))
                    Text("${formatOneDecimal(config.speed)}", color = textColor, fontSize = 13.sp, modifier = Modifier.width(36.dp))
                    Text("${formatOneDecimal(config.range)}", color = textColor, fontSize = 13.sp, modifier = Modifier.width(36.dp))
                    val costText = buildList {
                        if (config.trainingCost.credits > 0) add("${config.trainingCost.credits}cr")
                        if (config.trainingCost.crystal > 0) add("${config.trainingCost.crystal}c")
                    }.joinToString(" ")
                    Text(costText, color = textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }

            val config = TroopConfig.configFor(troopType, 1)
            if (config != null) {
                Spacer(Modifier.height(TamaSpacing.Small))
                if (config.wallDamageMultiplier > 1f) {
                    Text("Wall damage: ${config.wallDamageMultiplier.toInt()}x", color = TamaColors.Warning, fontSize = 13.sp)
                }
                if (config.splashRadius > 0f) {
                    Text("Splash radius: ${formatOneDecimal(config.splashRadius)}", color = TamaColors.Warning, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(TamaSpacing.Medium))
            TamaSecondaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

internal fun formatOneDecimal(value: Float): String {
    val rounded = (value * 10).roundToInt()
    return "${rounded / 10}.${rounded % 10}"
}

internal fun formatOneDecimal(value: Double): String = formatOneDecimal(value.toFloat())
