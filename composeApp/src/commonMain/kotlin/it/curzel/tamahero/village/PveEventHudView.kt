package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.ActiveEvent
import it.curzel.tamahero.ui.theme.*

@Composable
fun PveEventHudView(
    event: ActiveEvent,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(TamaColors.Error.copy(alpha = 0.85f))
            .padding(horizontal = TamaSpacing.Medium, vertical = TamaSpacing.XSmall),
        horizontalArrangement = Arrangement.spacedBy(TamaSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${event.type.name} in progress",
            color = TamaColors.Text,
            fontSize = 14.sp,
        )
        if (event.totalWaves > 1) {
            Text(
                "Wave ${event.currentWave + 1}/${event.totalWaves}",
                color = TamaColors.Text,
                fontSize = 14.sp,
            )
        }
    }
}
