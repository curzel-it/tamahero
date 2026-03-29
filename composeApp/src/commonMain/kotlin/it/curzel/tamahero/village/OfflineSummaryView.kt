package it.curzel.tamahero.village

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import it.curzel.tamahero.ui.theme.*

@Composable
fun OfflineSummaryView(
    summary: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TamaColors.Background.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = TamaSizes.ModalMaxWidth)
                .clip(RoundedCornerShape(TamaRadius.Large))
                .background(TamaColors.Surface)
                .clickable { }
                .padding(TamaSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (line in summary.lines()) {
                Text(line, color = TamaColors.Text, fontSize = 14.sp)
            }
            Spacer(Modifier.height(TamaSpacing.Medium))
            TamaButton(text = "OK", onClick = onDismiss)
        }
    }
}
