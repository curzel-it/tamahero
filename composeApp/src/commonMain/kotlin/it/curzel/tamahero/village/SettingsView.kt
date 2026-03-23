package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.ui.theme.*

@Composable
fun SettingsView(
    showGrid: Boolean,
    showFps: Boolean,
    onToggleGrid: (Boolean) -> Unit,
    onToggleFps: (Boolean) -> Unit,
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
                .widthIn(max = TamaSizes.ModalMaxWidth)
                .clip(RoundedCornerShape(TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable(enabled = false, onClick = {})
                .padding(TamaSpacing.Medium),
        ) {
            Text("Settings", color = TamaColors.Text, fontSize = 18.sp)
            Spacer(Modifier.height(TamaSpacing.Small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Show Grid", color = TamaColors.Text, fontSize = 14.sp)
                Switch(checked = showGrid, onCheckedChange = onToggleGrid)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Show FPS", color = TamaColors.Text, fontSize = 14.sp)
                Switch(checked = showFps, onCheckedChange = onToggleFps)
            }

            Spacer(Modifier.height(TamaSpacing.Small))
            TamaSecondaryButton(text = "Close", onClick = onDismiss)
        }
    }
}
