package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.models.ActiveEvent
import it.curzel.tamahero.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PveEventHudView(
    event: ActiveEvent,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        }
    }

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
        if (event.nextWaveAt > 0) {
            val remainingSec = ((event.nextWaveAt - now) / 1000).coerceAtLeast(0)
            Text(
                "Next wave in ${remainingSec}s",
                color = TamaColors.Warning,
                fontSize = 14.sp,
            )
        }
    }
}
